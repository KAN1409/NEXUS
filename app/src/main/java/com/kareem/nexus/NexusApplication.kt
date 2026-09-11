package com.kareem.nexus

import android.app.Application
import com.kareem.nexus.observe.UnderstandingWork
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UnderstandingWork.schedule(this)
    }
}
