package com.sleepcare.watch.data.transport

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sleepcare.watch.SleepCareWatchApplication
import kotlinx.coroutines.launch

class WatchCommandListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        val app = application as SleepCareWatchApplication
        val command = app.runtime.codec.decode(messageEvent.path, messageEvent.data) ?: return
        app.runtime.scope.launch {
            app.runtime.commandBus.tryEmit(command)
        }
    }
}

