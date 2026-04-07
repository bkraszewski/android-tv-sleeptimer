package com.bk.sleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        val serviceComponent = "${context.packageName}/.SleepTimerService"
        if (serviceComponent in enabled) {
            Log.i("SleepTimer", "Boot complete — accessibility service is enabled")
        } else {
            Log.w("SleepTimer", "Boot complete — accessibility service NOT in enabled list, re-run setup.sh")
        }

        if (!Settings.canDrawOverlays(context)) {
            Log.w("SleepTimer", "Boot complete — overlay permission NOT granted, re-run setup.sh")
        }
    }
}
