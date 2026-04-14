package com.sleepcare.watch

import android.app.Application
import com.sleepcare.watch.data.runtime.WatchRuntime

class SleepCareWatchApplication : Application() {
    lateinit var runtime: WatchRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        runtime = WatchRuntime.create(this)
    }
}

