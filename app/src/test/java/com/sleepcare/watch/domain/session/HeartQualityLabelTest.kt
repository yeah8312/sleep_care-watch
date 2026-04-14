package com.sleepcare.watch.domain.session

import com.google.common.truth.Truth.assertThat
import com.sleepcare.watch.domain.model.HeartQualityLabel
import org.junit.Test

class HeartQualityLabelTest {
    @Test
    fun `returns ok when hr and ibi statuses are valid`() {
        val label = HeartQualityLabel.fromStatuses(hrStatus = 1, ibiStatuses = listOf(0, 0))

        assertThat(label).isEqualTo(HeartQualityLabel.OK)
    }

    @Test
    fun `returns motion_or_weak for weak statuses`() {
        val label = HeartQualityLabel.fromStatuses(hrStatus = -8, ibiStatuses = emptyList())

        assertThat(label).isEqualTo(HeartQualityLabel.MOTION_OR_WEAK)
    }

    @Test
    fun `returns detached for detached status`() {
        val label = HeartQualityLabel.fromStatuses(hrStatus = -3, ibiStatuses = emptyList())

        assertThat(label).isEqualTo(HeartQualityLabel.DETACHED)
    }

    @Test
    fun `returns busy_or_initial for unknown idle states`() {
        val label = HeartQualityLabel.fromStatuses(hrStatus = 0, ibiStatuses = emptyList())

        assertThat(label).isEqualTo(HeartQualityLabel.BUSY_OR_INITIAL)
    }
}

