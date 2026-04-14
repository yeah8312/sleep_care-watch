package com.sleepcare.watch.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProtocolEnvelope<T>(
    val v: Int = 1,
    val t: String,
    val sid: String,
    val seq: Long,
    val src: String,
    @SerialName("sent_at_ms")
    val sentAtMs: Long,
    @SerialName("ack_required")
    val ackRequired: Boolean,
    val body: T,
)

@Serializable
data class StartSessionBody(
    @SerialName("study_mode")
    val studyMode: String,
    @SerialName("flush_policy")
    val flushPolicy: FlushPolicyBody,
    @SerialName("hr_required")
    val hrRequired: Boolean,
    @SerialName("watch_vibration_enabled")
    val watchVibrationEnabled: Boolean,
)

@Serializable
data class StopSessionBody(
    val reason: String? = null,
)

@Serializable
data class FlushPolicyBody(
    @SerialName("normal_sec")
    val normalSec: Int,
    @SerialName("suspect_sec")
    val suspectSec: Int,
    @SerialName("alert_sec")
    val alertSec: Int,
    @SerialName("recovery_sec")
    val recoverySec: Int = 15,
)

@Serializable
data class BackfillRequestBody(
    val from: Long,
)

@Serializable
data class AlertVibrateBody(
    val level: Int,
    val pattern: String,
    val reason: String = "mobile_alert",
)

@Serializable
data class HrSampleBody(
    @SerialName("sample_seq")
    val sampleSeq: Long,
    @SerialName("sensor_ts_ms")
    val sensorTsMs: Long,
    val bpm: Int,
    @SerialName("hr_status")
    val hrStatus: Int,
    @SerialName("ibi_ms")
    val ibiMs: List<Int>,
    @SerialName("ibi_status")
    val ibiStatus: List<Int>,
    @SerialName("delivery_mode")
    val deliveryMode: String,
)

@Serializable
data class HrBatchBody(
    @SerialName("from_sample_seq")
    val fromSampleSeq: Long,
    @SerialName("to_sample_seq")
    val toSampleSeq: Long,
    @SerialName("delivery_mode")
    val deliveryMode: String,
    val samples: List<HrSampleBody>,
)

@Serializable
data class HrAckBody(
    @SerialName("ack_sample_seq")
    val ackSampleSeq: Long,
)

@Serializable
data class SessionStatusBody(
    @SerialName("session_state")
    val sessionState: String,
    @SerialName("last_sample_at_ms")
    val lastSampleAtMs: Long? = null,
    @SerialName("last_ack_at_ms")
    val lastAckAtMs: Long? = null,
    @SerialName("buffered_sample_count")
    val bufferedSampleCount: Int = 0,
    @SerialName("phone_connection_state")
    val phoneConnectionState: String,
    @SerialName("sensor_availability")
    val sensorAvailability: String,
    @SerialName("last_error")
    val lastError: String? = null,
)

@Serializable
data class SessionCursorBody(
    @SerialName("ack_sample_seq")
    val ackSampleSeq: Long,
    @SerialName("latest_sample_seq")
    val latestSampleSeq: Long,
)

@Serializable
data class ErrorReportBody(
    val code: String,
    val message: String,
)

