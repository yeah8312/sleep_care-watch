package com.sleepcare.watch.domain.session

import com.sleepcare.watch.domain.model.HeartSample

class SampleBuffer {
    private val samples = linkedMapOf<Long, HeartSample>()
    private var latestSampleSeq = 0L

    fun add(sample: HeartSample) {
        samples[sample.sampleSeq] = sample
        latestSampleSeq = maxOf(latestSampleSeq, sample.sampleSeq)
    }

    fun addAll(newSamples: List<HeartSample>) {
        newSamples.forEach(::add)
    }

    fun pending(): List<HeartSample> = samples.values.toList()

    fun latestSampleSeq(): Long = latestSampleSeq

    fun size(): Int = samples.size

    fun acknowledgeThrough(ackSampleSeq: Long) {
        val iterator = samples.keys.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() <= ackSampleSeq) {
                iterator.remove()
            }
        }
    }

    fun fromSampleSeq(sampleSeq: Long): List<HeartSample> {
        return samples.values.filter { it.sampleSeq >= sampleSeq }
    }
}

