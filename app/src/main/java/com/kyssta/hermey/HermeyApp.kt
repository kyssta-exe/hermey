package com.kyssta.hermey

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HermeyApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
