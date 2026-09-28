package com.hifth.ui

import com.hifth.data.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VerseSelectionTest {
    private val chapters = listOf(
        Chapter(1, "الفاتحة", "Al-Fatihah", "The Opening", 7),
        Chapter(2, "البقرة", "Al-Baqarah", "The Cow", 286),
        Chapter(3, "آل عمران", "Aal-Imran", "The Family of Imran", 200)
    )

    @Test
    fun parsesSurahsAndVerseRangesIntoAnOrderedUniqueSet() {
        assertEquals(
            listOf("1:1", "1:2", "2:255", "3:1", "3:2"),
            parseVerseSelection("1:1-2, 2:255, 3:1-2", chapters).verseKeys
        )
    }

    @Test
    fun rejectsOutOfRangeAndMalformedInput() {
        assertNotNull(parseVerseSelection("1:8", chapters).error)
        assertNotNull(parseVerseSelection("67:x", chapters).error)
        assertNotNull(parseVerseSelection("999999999999999999999", chapters).error)
    }
}
