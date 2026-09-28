package com.hifth.ui

import com.hifth.data.QuranVerse
import com.hifth.data.QuranWord
import com.hifth.data.SavedVocabulary
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeQuizTest {
    private val verses = listOf(
        verse("1:1", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Most Merciful", listOf(word("بِسْمِ", "name"), word("اللَّهِ", "Allah"))),
        verse("1:2", "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "All praise is for Allah, Lord of all worlds", listOf(word("الْحَمْدُ", "praise"), word("رَبِّ", "Lord"))),
        verse("1:3", "الرَّحْمَٰنِ الرَّحِيمِ مَالِكِ يَوْمِ الدِّينِ", "The Most Merciful, Master of the Day of Judgment", listOf(word("مَالِكِ", "Master"), word("يَوْمِ", "day")))
    )

    @Test
    fun continuationQuizIncludesCorrectAnswerAndUniqueChoices() {
        val questions = buildPracticeQuiz(PracticeQuizMode.CONTINUATION, verses, emptyList(), Random(1))

        assertTrue(questions.isNotEmpty())
        questions.forEach { question ->
            assertTrue(question.answer in question.options)
            assertEquals(question.options.size, question.options.distinct().size)
            assertTrue(question.options.size in 2..4)
        }
    }

    @Test
    fun translationQuizUsesOtherAyahsAsDistractors() {
        val questions = buildPracticeQuiz(PracticeQuizMode.TRANSLATION, verses, emptyList(), Random(2))

        assertEquals(3, questions.size)
        questions.forEach { assertTrue(it.answer in it.options) }
    }

    @Test
    fun wordMeaningQuizIncludesSavedVocabulary() {
        val savedWords = listOf(SavedVocabulary(1, "1:4", "كِتَاب", "book", "", 1L))
        val questions = buildPracticeQuiz(PracticeQuizMode.WORD_MEANING, verses, savedWords, Random(3))

        assertTrue(questions.any { it.promptArabic == "كِتَاب" && it.answer == "book" })
    }

    @Test
    fun quizSkipsItemsWithoutEnoughDistinctAnswers() {
        val oneVerse = listOf(verse("1:1", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Same translation", emptyList()))

        assertTrue(buildPracticeQuiz(PracticeQuizMode.TRANSLATION, oneVerse, emptyList()).isEmpty())
    }

    private fun verse(key: String, arabic: String, translation: String, words: List<QuranWord>) =
        QuranVerse(key, key.substringBefore(":").toInt(), key.substringAfter(":").toInt(), arabic, translation, words)

    private fun word(text: String, translation: String) = QuranWord(null, text, translation, "")
}