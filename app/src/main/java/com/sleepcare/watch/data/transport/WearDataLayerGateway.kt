package com.sleepcare.watch.data.transport

import android.content.Context
import androidx.core.net.toUri
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sleepcare.watch.domain.model.HeartSample
import com.sleepcare.watch.domain.model.WatchUiState
import com.sleepcare.watch.protocol.ProtocolCodec
import com.sleepcare.watch.protocol.ProtocolConstants
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicLong

class WearDataLayerGateway(
    context: Context,
    private val codec: ProtocolCodec,
) {
    private val appContext = context.applicationContext
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val nodeClient: NodeClient = Wearable.getNodeClient(appContext)
    private val dataClient = Wearable.getDataClient(appContext)
    private val outgoingSeq = AtomicLong(1L)

    suspend fun sendLiveSample(sessionId: String, sample: HeartSample): Boolean {
        val payload = codec.encodeLiveSample(sessionId, outgoingSeq.getAndIncrement(), sample)
        return sendMessage(ProtocolConstants.PATH_HR_LIVE, payload)
    }

    suspend fun sendBatch(sessionId: String, samples: List<HeartSample>): Boolean {
        if (samples.isEmpty()) return true
        val payload = codec.encodeBatch(sessionId, outgoingSeq.getAndIncrement(), samples)
        return sendMessage(ProtocolConstants.PATH_HR_BATCH, payload)
    }

    suspend fun publishStatus(state: WatchUiState): Boolean {
        val sessionId = state.sessionContext?.sessionId ?: return false
        val currentPayload = codec.encodeStatus(state, sessionId, outgoingSeq.getAndIncrement())
        val cursorPayload = codec.encodeCursor(
            sessionId = sessionId,
            seq = outgoingSeq.getAndIncrement(),
            ackSampleSeq = state.ackSampleSeq,
            latestSampleSeq = state.latestSample?.sampleSeq ?: state.ackSampleSeq,
        )

        val currentRequest = PutDataMapRequest.create(ProtocolConstants.PATH_SESSION_CURRENT).apply {
            dataMap.putString("payload", currentPayload)
            dataMap.putLong("updated_at_ms", System.currentTimeMillis())
        }
        val cursorRequest = PutDataMapRequest.create(ProtocolConstants.sessionCursorPath(sessionId)).apply {
            dataMap.putString("payload", cursorPayload)
            dataMap.putLong("updated_at_ms", System.currentTimeMillis())
        }

        dataClient.putDataItem(currentRequest.asPutDataRequest()).await()
        dataClient.putDataItem(cursorRequest.asPutDataRequest()).await()
        return true
    }

    suspend fun sendError(sessionId: String, code: String, message: String): Boolean {
        val payload = codec.encodeError(sessionId, outgoingSeq.getAndIncrement(), code, message)
        return sendMessage("/sc/v1/session/error", payload)
    }

    suspend fun hasReachableNode(): Boolean {
        return nodeClient.connectedNodes.await().isNotEmpty()
    }

    private suspend fun sendMessage(path: String, payload: String): Boolean {
        val nodes = nodeClient.connectedNodes.await()
        if (nodes.isEmpty()) return false

        var sentAny = false
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, path, payload.encodeToByteArray()).await()
            sentAny = true
        }
        return sentAny
    }
}

