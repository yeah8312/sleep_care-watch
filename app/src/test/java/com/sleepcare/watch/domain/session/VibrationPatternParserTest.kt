package com.sleepcare.watch.domain.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VibrationPatternParserTest {
    @Test
    fun `parses comma separated millisecond values`() {
        val parsed = VibrationPatternParser.parse("200,100,400")

        assertThat(parsed.toList()).containsExactly(200L, 100L, 400L).inOrder()
    }

    @Test
    fun `drops invalid and negative values`() {
        val parsed = VibrationPatternParser.parse("200,abc,-1, 300")

        assertThat(parsed.toList()).containsExactly(200L, 300L).inOrder()
    }
}

