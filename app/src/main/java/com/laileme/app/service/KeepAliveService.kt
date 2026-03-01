package com.laileme.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.laileme.app.MainActivity
import com.laileme.app.R
import java.text.SimpleDateFormat
import java.util.*

class KeepAliveService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "KeepAliveService"
        private const val CHANNEL_ID = "laileme_keepalive"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            return manager.getRunningServices(Int.MAX_VALUE).any {
                it.service.className == KeepAliveService::class.java.name
            }
        }
    }

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var baseline = -1f
    private var todaySteps = 0

    private val todayKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val healthPrefs by lazy {
        val userId = getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
            .getString("user_id", "default") ?: "default"
        getSharedPreferences("laileme_health_$userId", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 读取今日已有步数
        todaySteps = healthPrefs.getInt("steps_$todayKey", 0)
        baseline = healthPrefs.getFloat("step_baseline_$todayKey", -1f)

        // 注册计步传感器
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            Log.i(TAG, "计步传感器已注册")
        } else {
            Log.w(TAG, "设备无计步传感器")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.i(TAG, "前台服务已启动")
        } catch (e: Exception) {
            Log.e(TAG, "startForeground失败（可能缺少权限）: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY // 被杀后自动重启
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "应用被划掉，调度 AlarmManager 重启服务")
        scheduleRestart()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        // 非用户主动关闭时自动重启
        val prefs = getSharedPreferences("laileme_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("step_notification", true)) {
            Log.i(TAG, "服务被销毁，调度重启")
            scheduleRestart()
        }
        Log.i(TAG, "前台服务已停止")
        super.onDestroy()
    }

    private fun scheduleRestart() {
        try {
            val restartIntent = Intent(applicationContext, RestartReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0, restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 3000,  // 3秒后重启
                pendingIntent
            )
        } catch (e: Exception) {
            Log.w(TAG, "调度重启失败: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── 传感器回调 ──
    override fun onSensorChanged(event: SensorEvent?) {
        val totalSteps = event?.values?.firstOrNull() ?: return
        val key = todayKey

        if (baseline < 0f) {
            baseline = totalSteps - healthPrefs.getInt("steps_$key", 0).toFloat()
            healthPrefs.edit().putFloat("step_baseline_$key", baseline).apply()
        }

        val steps = (totalSteps - baseline).toInt().coerceAtLeast(0)
        if (steps > todaySteps) {
            todaySteps = steps
            val calories = (steps * 0.04f).toInt()
            val exerciseTime = (steps / 100).coerceAtMost(120)

            healthPrefs.edit()
                .putInt("steps_$key", todaySteps)
                .putInt("calories_$key", calories)
                .putInt("exercise_$key", exerciseTime)
                .apply()

            // 更新通知
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── 通知 ──
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "来了么 · 健康守护",
                NotificationManager.IMPORTANCE_LOW  // 低重要性，不弹出不响铃
            ).apply {
                description = "持续记录步数和健康数据"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 读取经期信息
        val periodInfo = getPeriodInfo()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("来了么 · 健康守护中")
            .setContentText("今日 $todaySteps 步  $periodInfo")
            .setContentIntent(pendingIntent)
            .setOngoing(true)  // 不可滑除，保持后台计步
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun getPeriodInfo(): String {
        return try {
            val db = com.laileme.app.data.AppDatabase.getDatabase(this)
            val records = db.periodDao().getAllRecordsSync()
            if (records.isEmpty()) return "未记录经期"

            val latest = records.maxByOrNull { it.startDate }!!
            val now = System.currentTimeMillis()
            val startMs = latest.startDate
            val periodLength = latest.periodLength.toLong()
            val cycleLength = latest.cycleLength.toLong()
            val dayMs = 24L * 60L * 60L * 1000L
            val periodEndMs = startMs + periodLength * dayMs
            val isInPeriod = latest.endDate == null && now in startMs..periodEndMs

            if (isInPeriod) {
                val daysIn = ((now - startMs) / dayMs).toInt() + 1
                "经期第${daysIn}天"
            } else {
                val nextStart = if (cycleLength > 0) {
                    val cyclesPassed = ((now - startMs) / (cycleLength * dayMs)).toInt()
                    startMs + (cyclesPassed + 1) * cycleLength * dayMs
                } else startMs + 28L * dayMs
                val daysLeft = ((nextStart - now) / dayMs).toInt().coerceAtLeast(0)
                "${daysLeft}天后来"
            }
        } catch (_: Exception) { "健康守护中" }
    }
}
