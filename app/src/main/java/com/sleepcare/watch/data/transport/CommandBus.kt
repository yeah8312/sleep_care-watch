package com.sleepcare.watch.data.transport

import com.sleepcare.watch.protocol.IncomingCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CommandBus {
    private val mutableCommands = MutableSharedFlow<IncomingCommand>(extraBufferCapacity = 64)

    val commands: SharedFlow<IncomingCommand> = mutableCommands.asSharedFlow()

    fun tryEmit(command: IncomingCommand) {
        mutableCommands.tryEmit(command)
    }
}

