package com.sleepcare.watch.data.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

interface PhoneLauncher {
    suspend fun openOnPhone(uri: Uri = DEFAULT_URI.toUri()): Boolean

    companion object {
        const val DEFAULT_URI = "sleepcare://watch/setup"
    }
}

class WearPhoneLauncher(
    private val context: Context,
) : PhoneLauncher {
    private val executor = Executors.newSingleThreadExecutor()
    private val remoteActivityHelper = RemoteActivityHelper(context, executor)

    override suspend fun openOnPhone(uri: Uri): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val future = remoteActivityHelper.startRemoteActivity(Intent(Intent.ACTION_VIEW, uri))
            future.addListener(
                {
                    continuation.resume(
                        runCatching {
                            future.get()
                            true
                        }.getOrElse { false },
                    )
                },
                context.mainExecutor,
            )
        }
    }
}
