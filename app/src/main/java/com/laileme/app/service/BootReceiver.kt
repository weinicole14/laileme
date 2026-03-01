package com.laileme.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "开机完成，启动保活服务")
            try {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    KeepAliveService.start(context)
                }
            } catch (e: Exception) {
                Log.w("BootReceiver", "启动保活服务失败: ${e.message}")
            }
        }
    }
}
