package com.ca.screentime.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager

/** 제한하지 않고 사용시간에도 넣지 않는 전화 관련 앱 (전화, 통화 화면, 긴급전화). */
class PhoneApps(context: Context) {
    private val packages: Set<String>

    init {
        val set = mutableSetOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.incallui",
            "com.samsung.android.incallui",
            "com.android.server.telecom",
            "com.android.phone",
            "com.android.emergency",
            "com.google.android.apps.safetyhub",
        )
        runCatching {
            val telecom = context.getSystemService(TelecomManager::class.java)
            telecom?.defaultDialerPackage?.let(set::add)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) telecom?.systemDialerPackage?.let(set::add)
        }
        runCatching {
            val pm = context.packageManager
            listOf(Intent(Intent.ACTION_DIAL), Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))).forEach { intent ->
                pm.queryIntentActivities(intent, 0).forEach { set.add(it.activityInfo.packageName) }
            }
        }
        packages = set
    }

    fun isPhone(pkg: String?): Boolean = pkg != null && pkg in packages
}
