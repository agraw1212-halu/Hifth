package com.hifth.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeComparisonTest {
    @Test
    fun matchesArabicAfterRemovingVowelMarks() {
        assertEquals(
            listOf(true, true),
            compareWords(listOf("بِسْمِ", "اللَّهِ"), listOf("بسم", "الله"))
        )
    }

    @Test
    fun highlightsWrongMissingAndExtraWordsByPosition() {
        assertEquals(
            listOf(true, false, false),
            compareWords(listOf("In", "the"), listOf("in,", "wrong", "extra"))
        )
    }
}
