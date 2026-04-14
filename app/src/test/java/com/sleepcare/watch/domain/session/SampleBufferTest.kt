package com.sleepcare.watch.domain.session

import com.google.common.truth.Truth.assertThat
import com.sleepcare.watch.domain.model.HeartQualityLabel
import com.sleepcare.watch.domain.model.HeartSample
import org.junit.Test

class SampleBufferTest {
    @Test
    fun `acknowledgeThrough removes contiguous acknowledged samples`() {
        val buffer = SampleBuffer()
        buffer.add(sample(1))
        buffer.add(sample(2))
        buffer.add(sample(3))

        buffer.acknowledgeThrough(2)

        assertThat(buffer.pending().map { it.sampleSeq }).containsExactly(3L)
    }

    @Test
    fun `fromSampleSeq returns only requested range`() {
        val buffer = SampleBuffer()
        buffer.add(sample(10))
        buffer.add(sample(11))
        buffer.add(sample(12))

        assertThat(buffer.fromSampleSeq(11).map { it.sampleSeq }).containsExactly(11L, 12L).inOrder()
    }

    private fun sample(seq: Long): HeartSample {
        return HeartSample(
            sampleSeq = seq,
            sensorTsMs = seq * 1_000,
            bpm = 70,
            hrStatus = 1,
            ibiMs = listOf(830),
            ibiStatus = listOf(0),
            qualityLabel = HeartQualityLabel.OK,
        )
    }
}

