package com.hifth.ui

import com.hifth.data.QuranVerse
import com.hifth.data.SavedVocabulary
import kotlin.random.Random

internal enum class PracticeQuizMode(val label: String, val prompt: String) {
    CONTINUATION("Continue ayah", "Choose the words that follow."),
    WORD_MEANING("Word meaning", "Choose the closest meaning."),
    TRANSLATION("Translation", "Choose the matching translation.")
}

internal data class PracticeQuizQuestion(
    val verseKey: String,
    val prompt: String,
    val promptArabic: String,
    val answer: String,
    val options: List<String>
)

internal fun buildPracticeQuiz(
    mode: PracticeQuizMode,
    verses: List<QuranVerse>,
    savedWords: List<SavedVocabulary>,
    random: Random = Random.Default
): List<PracticeQuizQuestion> {
    val questions = when (mode) {
        PracticeQuizMode.CONTINUATION -> buildContinuationQuestions(verses, random)
        PracticeQuizMode.WORD_MEANING -> buildMeaningQuestions(verses, savedWords, random)
        PracticeQuizMode.TRANSLATION -> buildTranslationQuestions(verses, random)
    }
    return questions.shuffled(random).take(10)
}

private fun buildContinuationQuestions(
    verses: List<QuranVerse>,
    random: Random
): List<PracticeQuizQuestion> {
    val tokenized = verses.map { verse -> verse to verse.arabic.trim().split(Regex("\\s+")) }
        .filter { (_, words) -> words.size >= 4 }
    return tokenized.mapNotNull { (verse, words) ->
        val splitAt = (words.size / 2).coerceAtLeast(2)
        val answer = words.drop(splitAt).joinToString(" ")
        val distractors = tokenized.asSequence()
            .filter { (other, _) -> other.key != verse.key }
            .map { (_, otherWords) -> otherWords.drop(splitAt.coerceAtMost(otherWords.lastIndex)).joinToString(" ") }
            .filter(String::isNotBlank)
            .toList()
        val options = makeOptions(answer, distractors, random) ?: return@mapNotNull null
        PracticeQuizQuestion(
            verseKey = verse.key,
            prompt = PracticeQuizMode.CONTINUATION.prompt,
            promptArabic = "${words.take(splitAt).joinToString(" ")} …",
            answer = answer,
            options = options
        )
    }
}

private fun buildMeaningQuestions(
    verses: List<QuranVerse>,
    savedWords: List<SavedVocabulary>,
    random: Random
): List<PracticeQuizQuestion> {
    val entries = verses.flatMap { verse ->
        verse.words.filter { it.text.isNotBlank() && it.translation.isNotBlank() }
            .map { Triple(it.text, it.translation, verse.key) }
    } + savedWords.filter { it.arabic.isNotBlank() && it.meaning.isNotBlank() }
        .map { Triple(it.arabic, it.meaning, it.verseKey) }

    return entries.distinctBy { (arabic, meaning, _) -> arabic to meaning }.mapNotNull { (arabic, meaning, verseKey) ->
        val distractors = entries.map { it.second }
        val options = makeOptions(meaning, distractors, random) ?: return@mapNotNull null
        PracticeQuizQuestion(
            verseKey = verseKey,
            prompt = PracticeQuizMode.WORD_MEANING.prompt,
            promptArabic = arabic,
            answer = meaning,
            options = options
        )
    }
}

private fun buildTranslationQuestions(
    verses: List<QuranVerse>,
    random: Random
): List<PracticeQuizQuestion> {
    val usable = verses.filter { it.arabic.isNotBlank() && it.translation.isNotBlank() }
    return usable.mapNotNull { verse ->
        val options = makeOptions(verse.translation, usable.filter { it.key != verse.key }.map { it.translation }, random)
            ?: return@mapNotNull null
        PracticeQuizQuestion(
            verseKey = verse.key,
            prompt = PracticeQuizMode.TRANSLATION.prompt,
            promptArabic = verse.arabic,
            answer = verse.translation,
            options = options
        )
    }
}

private fun makeOptions(answer: String, distractors: List<String>, random: Random): List<String>? {
    val choices = (distractors.filter { it.isNotBlank() && it != answer }
        .distinct()
        .shuffled(random)
        .take(3) + answer)
        .shuffled(random)
    return choices.takeIf { it.size >= 2 && answer in it }
}