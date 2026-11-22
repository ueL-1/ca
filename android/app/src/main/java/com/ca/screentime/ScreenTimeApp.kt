package com.ca.screentime

import android.app.Application
import com.ca.screentime.service.MonitorService

class ScreenTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MonitorService.ensureChannel(this)
    }
}
