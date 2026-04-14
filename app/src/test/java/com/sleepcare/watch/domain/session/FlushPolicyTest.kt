package com.sleepcare.watch.domain.session

import com.google.common.truth.Truth.assertThat
import com.sleepcare.watch.domain.model.AppSessionState
import com.sleepcare.watch.domain.model.FlushPolicy
import org.junit.Test

class FlushPolicyTest {
    private val policy = FlushPolicy(normalSec = 15, suspectSec = 5, alertSec = 2, recoverySec = 15)

    @Test
    fun `uses normal interval for running`() {
        assertThat(policy.currentIntervalSec(AppSessionState.RUNNING)).isEqualTo(15)
    }

    @Test
    fun `uses suspect interval for degraded`() {
        assertThat(policy.currentIntervalSec(AppSessionState.DEGRADED)).isEqualTo(5)
    }

    @Test
    fun `uses alert interval for alerting`() {
        assertThat(policy.currentIntervalSec(AppSessionState.ALERTING)).isEqualTo(2)
    }
}

