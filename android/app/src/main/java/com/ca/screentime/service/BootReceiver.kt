package com.ca.screentime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 재부팅 후, 앱 업데이트 후 감시 서비스를 자동으로 다시 시작한다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> MonitorService.startIfReady(context)
        }
    }
}
