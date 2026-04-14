package com.sleepcare.watch.protocol

import com.sleepcare.watch.domain.model.AlertState
import com.sleepcare.watch.domain.model.AlertVibrateBody
import com.sleepcare.watch.domain.model.AppSessionState
import com.sleepcare.watch.domain.model.ErrorReportBody
import com.sleepcare.watch.domain.model.FlushPolicy
import com.sleepcare.watch.domain.model.FlushPolicyBody
import com.sleepcare.watch.domain.model.HeartQualityLabel
import com.sleepcare.watch.domain.model.HeartSample
import com.sleepcare.watch.domain.model.HrAckBody
import com.sleepcare.watch.domain.model.HrBatchBody
import com.sleepcare.watch.domain.model.HrSampleBody
import com.sleepcare.watch.domain.model.PhoneConnectionState
import com.sleepcare.watch.domain.model.ProtocolEnvelope
import com.sleepcare.watch.domain.model.SensorAvailability
import com.sleepcare.watch.domain.model.SessionContext
import com.sleepcare.watch.domain.model.SessionCursorBody
import com.sleepcare.watch.domain.model.SessionStatusBody
import com.sleepcare.watch.domain.model.StartSessionBody
import com.sleepcare.watch.domain.model.StopSessionBody
import com.sleepcare.watch.domain.model.WatchUiState
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

sealed interface IncomingCommand {
    data class StartSession(
        val sessionId: String,
        val context: SessionContext,
        val flushPolicy: FlushPolicy,
    ) : IncomingCommand

    data class StopSession(
        val sessionId: String,
        val reason: String?,
    ) : IncomingCommand

    data class UpdateFlushPolicy(
        val sessionId: String,
        val flushPolicy: FlushPolicy,
    ) : IncomingCommand

    data class RequestBackfill(
        val sessionId: String,
        val fromSampleSeq: Long,
    ) : IncomingCommand

    data class TriggerAlert(
        val sessionId: String,
        val alert: AlertState,
    ) : IncomingCommand

    data class AckReceived(
        val sessionId: String,
        val ackSampleSeq: Long,
    ) : IncomingCommand
}

class ProtocolCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    },
) {
    fun decode(path: String, payload: ByteArray): IncomingCommand? {
        return when (path) {
            ProtocolConstants.PATH_CTL_START -> {
                val envelope = json.decodeFromString<ProtocolEnvelope<StartSessionBody>>(payload.decodeToString())
                IncomingCommand.StartSession(
                    sessionId = envelope.sid,
                    context = SessionContext(
                        sessionId = envelope.sid,
                        studyMode = envelope.body.studyMode,
                        hrRequired = envelope.body.hrRequired,
                        watchVibrationEnabled = envelope.body.watchVibrationEnabled,
                    ),
                    flushPolicy = envelope.body.flushPolicy.toDomain(),
                )
            }

            ProtocolConstants.PATH_CTL_STOP -> {
                val envelope = json.decodeFromString<ProtocolEnvelope<StopSessionBody>>(payload.decodeToString())
                IncomingCommand.StopSession(envelope.sid, envelope.body.reason)
            }

            ProtocolConstants.PATH_CTL_FLUSH_POLICY -> {
                val envelope = json.decodeFromString<ProtocolEnvelope<FlushPolicyBody>>(payload.decodeToString())
                IncomingCommand.UpdateFlushPolicy(
                    sessionId = envelope.sid,
                    flushPolicy = envelope.body.toDomain(),
                )
            }

            ProtocolConstants.PATH_CTL_BACKFILL_REQ -> {
                val envelope =
                    json.decodeFromString<ProtocolEnvelope<com.sleepcare.watch.domain.model.BackfillRequestBody>>(payload.decodeToString())
                IncomingCommand.RequestBackfill(
                    sessionId = envelope.sid,
                    fromSampleSeq = envelope.body.from,
                )
            }

            ProtocolConstants.PATH_ALERT_VIBRATE -> {
                val envelope = json.decodeFromString<ProtocolEnvelope<AlertVibrateBody>>(payload.decodeToString())
                IncomingCommand.TriggerAlert(
                    sessionId = envelope.sid,
                    alert = AlertState(
                        level = envelope.body.level,
                        reason = envelope.body.reason,
                        pattern = envelope.body.pattern,
                    ),
                )
            }

            ProtocolConstants.PATH_HR_ACK -> {
                val envelope = json.decodeFromString<ProtocolEnvelope<HrAckBody>>(payload.decodeToString())
                IncomingCommand.AckReceived(
                    sessionId = envelope.sid,
                    ackSampleSeq = envelope.body.ackSampleSeq,
                )
            }

            else -> null
        }
    }

    fun encodeLiveSample(sessionId: String, seq: Long, sample: HeartSample): String {
        return json.encodeToString(
            ProtocolEnvelope(
                t = "hr.sample",
                sid = sessionId,
                seq = seq,
                src = ProtocolConstants.SOURCE_WATCH,
                sentAtMs = System.currentTimeMillis(),
                ackRequired = true,
                body = sample.toBody(deliveryMode = "live"),
            ),
        )
    }

    fun encodeBatch(sessionId: String, seq: Long, samples: List<HeartSample>): String {
        val first = samples.first()
        val last = samples.last()
        return json.encodeToString(
            ProtocolEnvelope(
                t = "hr.batch",
                sid = sessionId,
                seq = seq,
                src = ProtocolConstants.SOURCE_WATCH,
                sentAtMs = System.currentTimeMillis(),
                ackRequired = true,
                body = HrBatchBody(
                    fromSampleSeq = first.sampleSeq,
                    toSampleSeq = last.sampleSeq,
                    deliveryMode = "batch",
                    samples = samples.map { it.toBody(deliveryMode = "batch") },
                ),
            ),
        )
    }

    fun encodeAck(sessionId: String, seq: Long, ackSampleSeq: Long): String {
        return json.encodeToString(
            ProtocolEnvelope(
                t = "hr.ack",
                sid = sessionId,
                seq = seq,
                src = ProtocolConstants.SOURCE_WATCH,
                sentAtMs = System.currentTimeMillis(),
                ackRequired = false,
                body = HrAckBody(ackSampleSeq = ackSampleSeq),
            ),
        )
    }

    fun encodeStatus(state: WatchUiState, sessionId: String, seq: Long): String {
        return json.encodeToString(
            ProtocolEnvelope(
                t = "session.status",
                sid = sessionId,
                seq = seq,
                src = ProtocolConstants.SOURCE_WATCH,
                sentAtMs = System.currentTimeMillis(),
                ackRequired = false,
                body = SessionStatusBody(
                    sessionState = state.appState.name,
                    lastSampleAtMs = state.lastSampleAtMs,
                    lastAckAtMs = state.lastAckAtMs,
                    bufferedSampleCount = state.bufferedSampleCount,
                    phoneConnectionState = state.phoneConnectionState.name,
                    sensorAvailability = state.sensorAvailability.name,
                    lastError = state.lastErrorMessage,
                ),
            ),
        )
    }

    fun encodeCursor(
        sessionId: String,
        seq: Long,
        ackSampleSeq: Long,
        latestSampleSeq: Long,
    ): String {
        return json.encodeToString(
            ProtocolEnvelope(
                t = "session.cursor",
                sid = sessionId,
                seq = seq,
                src = ProtocolConstants.SOURCE_WATCH,
                sentAtMs = System.currentTimeMillis(),
                ackRequired = false,
                body = SessionCursorBody(
                    ackSampleSeq = ackSampleSeq,
                    latestSampleSeq = latestSampleSeq,
                ),
            ),
        )
    }

    fun encodeError(sessionId: String, seq: Long, code: String, message: String): String {
        return json.encodeToString(
            ProtocolEnvelope(
                t = "session.error",
                sid = sessionId,
                seq = seq,
                src = ProtocolConstants.SOURCE_WATCH,
                sentAtMs = System.currentTimeMillis(),
                ackRequired = false,
                body = ErrorReportBody(code = code, message = message),
            ),
        )
    }

    private fun HeartSample.toBody(deliveryMode: String): HrSampleBody {
        return HrSampleBody(
            sampleSeq = sampleSeq,
            sensorTsMs = sensorTsMs,
            bpm = bpm,
            hrStatus = hrStatus,
            ibiMs = ibiMs,
            ibiStatus = ibiStatus,
            deliveryMode = deliveryMode,
        )
    }

    private fun FlushPolicyBody.toDomain(): FlushPolicy {
        return FlushPolicy(
            normalSec = normalSec,
            suspectSec = suspectSec,
            alertSec = alertSec,
            recoverySec = recoverySec,
        )
    }
}

