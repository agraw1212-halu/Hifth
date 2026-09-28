package com.hifth.data

data class Chapter(
    val id: Int,
    val nameArabic: String,
    val name: String,
    val translatedName: String,
    val verseCount: Int
)

data class QuranWord(
    val id: Int?,
    val text: String,
    val translation: String,
    val transliteration: String,
    val root: String = "",
    val lemma: String = "",
    val grammar: String = ""
)

data class QuranVerse(
    val key: String,
    val chapterId: Int,
    val verseNumber: Int,
    val arabic: String,
    val translation: String,
    val words: List<QuranWord>,
    val memorizedAt: Long? = null,
    val juzNumber: Int = 0
)

data class QuranVersePage(val verses: List<QuranVerse>, val hasMore: Boolean)

data class Reciter(val id: Int, val name: String)

data class TafsirOption(val id: Int, val name: String)

data class StudyPlan(
    val id: Long,
    val title: String,
    val verseKeys: List<String>,
    val createdAt: Long
)

data class SavedVocabulary(
    val id: Long,
    val verseKey: String,
    val arabic: String,
    val meaning: String,
    val root: String,
    val savedAt: Long
)

data class RecitationRecording(
    val id: Long,
    val verseKey: String,
    val path: String,
    val createdAt: Long
)

data class WeakSpot(
    val verseKey: String,
    val attempts: Int,
    val errorPoints: Int,
    val lastPracticedAt: Long
)
