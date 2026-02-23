package com.laileme.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.laileme.app.MainActivity
import com.laileme.app.R

object NotificationHelper {

    // 通知渠道ID
    private const val CHANNEL_PERIOD = "laileme_period"
    private const val CHANNEL_WATER = "laileme_water"
    private const val CHANNEL_MED = "laileme_medication"
    private const val CHANNEL_SLEEP = "laileme_sleep"
    private const val CHANNEL_TIPS = "laileme_tips"

    /** 初始化所有通知渠道（Android 8.0+） */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_PERIOD, "经期提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "经期来临前的温馨提醒" },

                NotificationChannel(
                    CHANNEL_WATER, "喝水提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "定时提醒你补充水分" },

                NotificationChannel(
                    CHANNEL_MED, "用药提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "按时服药提醒" },

                NotificationChannel(
                    CHANNEL_SLEEP, "睡眠提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "睡前温馨提醒" },

                NotificationChannel(
                    CHANNEL_TIPS, "暖心小贴士",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "不定时推送的健康生活小贴士" }
            )

            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    /** 发送通知（通用） */
    private fun send(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        content: String
    ) {
        // Android 13+ 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    // ── 各类型通知快捷方法 ──

    fun sendPeriodReminder(context: Context) {
        val (title, content) = NotificationMessages.randomPeriod()
        send(context, CHANNEL_PERIOD, 1001, title, content)
    }

    fun sendWaterReminder(context: Context) {
        val (title, content) = NotificationMessages.randomWater()
        send(context, CHANNEL_WATER, 1002, title, content)
    }

    fun sendMedicationReminder(context: Context) {
        val (title, content) = NotificationMessages.randomMedication()
        send(context, CHANNEL_MED, 1003, title, content)
    }

    fun sendSleepReminder(context: Context) {
        val (title, content) = NotificationMessages.randomSleep()
        send(context, CHANNEL_SLEEP, 1004, title, content)
    }

    fun sendWarmTip(context: Context) {
        val (title, content) = NotificationMessages.randomTip()
        send(context, CHANNEL_TIPS, 1005, title, content)
    }
}
