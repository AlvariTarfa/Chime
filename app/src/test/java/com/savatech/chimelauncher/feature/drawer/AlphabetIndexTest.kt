package com.savatech.chimelauncher.feature.drawer

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetIndexTest {
    @Test
    fun accentedInitialUsesItsBaseLetter() {
        assertEquals("E", AlphabetIndex.bucketFor("Éclair"))
    }

    @Test
    fun nonLetterInitialUsesNumberBucket() {
        assertEquals("#", AlphabetIndex.bucketFor("123App"))
    }

    @Test
    fun emptyLabelUsesNumberBucket() {
        assertEquals("#", AlphabetIndex.bucketFor(""))
    }
}
