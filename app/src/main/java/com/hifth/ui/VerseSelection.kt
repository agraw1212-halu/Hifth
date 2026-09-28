package com.hifth.ui

import com.hifth.data.Chapter

internal data class VerseSelection(
    val verseKeys: List<String>,
    val error: String? = null
)

internal fun parseVerseSelection(input: String, chapters: List<Chapter>): VerseSelection {
    val tokens = input.split(",").map { it.trim() }.filter(String::isNotEmpty)
    if (tokens.isEmpty()) return VerseSelection(emptyList(), "Enter a surah or ayah range.")

    val chapterCounts = chapters.associate { it.id to it.verseCount }
    val selected = linkedSetOf<String>()
    for (token in tokens) {
        when {
            Regex("^\\d+:\\d+(?:-\\d+)?$").matches(token) -> {
                val parts = token.split(":")
                val chapterId = parts[0].toIntOrNull()
                    ?: return VerseSelection(emptyList(), "Surah number in \"$token\" is too large.")
                val range = parts[1].split("-")
                val firstVerse = range[0].toIntOrNull()
                    ?: return VerseSelection(emptyList(), "Ayah number in \"$token\" is too large.")
                val end = range.getOrNull(1)?.toIntOrNull()
                    ?: if (range.size == 1) firstVerse else return VerseSelection(emptyList(), "Ayah number in \"$token\" is too large.")
                val count = chapterCounts[chapterId] ?: return VerseSelection(emptyList(), "Surah $chapterId does not exist.")
                if (firstVerse !in 1..end || end > count) {
                    return VerseSelection(emptyList(), "Ayah range $token is outside surah $chapterId.")
                }
                (firstVerse..end).forEach { selected += "$chapterId:$it" }
            }
            Regex("^\\d+(?:-\\d+)?$").matches(token) -> {
                val bounds = token.split("-")
                val start = bounds[0].toIntOrNull()
                    ?: return VerseSelection(emptyList(), "Surah number in \"$token\" is too large.")
                val end = bounds.getOrNull(1)?.toIntOrNull()
                    ?: if (bounds.size == 1) start else return VerseSelection(emptyList(), "Surah number in \"$token\" is too large.")
                if (start !in 1..end || end !in chapterCounts.keys) {
                    return VerseSelection(emptyList(), "Surah range $token is invalid.")
                }
                (start..end).forEach { chapterId ->
                    val count = chapterCounts[chapterId]
                        ?: return VerseSelection(emptyList(), "Surah $chapterId does not exist.")
                    (1..count).forEach { verse -> selected += "$chapterId:$verse" }
                }
            }
            else -> return VerseSelection(emptyList(), "Can't read \"$token\". Try 1, 2:255, or 67:1-10.")
        }
        if (selected.size > 6_236) return VerseSelection(emptyList(), "A selection cannot exceed the full Quran.")
    }
    return VerseSelection(selected.sortedWith(compareBy(
        { it.substringBefore(":").toInt() },
        { it.substringAfter(":").toInt() }
    )))
}
