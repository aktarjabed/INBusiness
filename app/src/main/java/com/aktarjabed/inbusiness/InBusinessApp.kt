package com.aktarjabed.inbusiness

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InBusinessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}
