package com.youtubebridge.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.youtubebridge.app.service.ServerForegroundService
import com.youtubebridge.app.util.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = Prefs(context)
            if (prefs.autoStart) {
                ServerForegroundService.start(context)
            }
        }
    }
}
