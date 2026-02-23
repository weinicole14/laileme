package com.laileme.app.notification

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ══════════════════════════════════════════
// Worker 定义（每个都双重检查：总开关 + 分开关）
// ══════════════════════════════════════════

class WaterReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("laileme_reminders", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notification_master", false) && prefs.getBoolean("water_remind", false)) {
            NotificationHelper.sendWaterReminder(applicationContext)
        }
        return Result.success()
    }
}

class SleepReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("laileme_reminders", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notification_master", false) && prefs.getBoolean("sleep_remind", false)) {
            NotificationHelper.sendSleepReminder(applicationContext)
        }
        return Result.success()
    }
}

class MedicationReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("laileme_reminders", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notification_master", false) && prefs.getBoolean("med_remind", false)) {
            NotificationHelper.sendMedicationReminder(applicationContext)
        }
        return Result.success()
    }
}

class PeriodReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("laileme_reminders", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notification_master", false) && prefs.getBoolean("period_remind", true)) {
            NotificationHelper.sendPeriodReminder(applicationContext)
        }
        return Result.success()
    }
}

class WarmTipWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("laileme_reminders", Context.MODE_PRIVATE)
        if (prefs.getBoolean("notification_master", false)) {
            NotificationHelper.sendWarmTip(applicationContext)
        }
        return Result.success()
    }
}

// ══════════════════════════════════════════
// 调度管理器
// ══════════════════════════════════════════

object NotificationScheduler {

    private const val WATER_WORK = "laileme_water_reminder"
    private const val SLEEP_WORK = "laileme_sleep_reminder"
    private const val MED_WORK = "laileme_med_reminder"
    private const val PERIOD_WORK = "laileme_period_reminder"
    private const val TIP_WORK = "laileme_warm_tip"

    /**
     * 计算从当前时间到目标时刻 (hour:minute) 的延迟毫秒数。
     * 如果今天的目标时刻已过，则延迟到明天同一时刻。
     */
    private fun delayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    /**
     * 初始化所有定时通知任务。
     * 在 MainActivity.onCreate 及设置变更后调用。
     */
    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences("laileme_reminders", Context.MODE_PRIVATE)
        val wm = WorkManager.getInstance(context)

        // 总开关关闭 → 取消所有任务
        if (!prefs.getBoolean("notification_master", false)) {
            cancelAll(context)
            return
        }

        // ── 喝水提醒：每 N 小时，首次延迟 N 小时后才触发 ──
        if (prefs.getBoolean("water_remind", false)) {
            val interval = prefs.getInt("water_interval", 2).toLong().coerceIn(1, 8)
            val work = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                interval, TimeUnit.HOURS
            ).setInitialDelay(interval, TimeUnit.HOURS)
                .addTag(WATER_WORK)
                .build()
            wm.enqueueUniquePeriodicWork(WATER_WORK, ExistingPeriodicWorkPolicy.KEEP, work)
        } else {
            wm.cancelUniqueWork(WATER_WORK)
        }

        // ── 睡眠提醒：每天在用户设定的时间提醒 ──
        if (prefs.getBoolean("sleep_remind", false)) {
            val sleepTime = prefs.getString("sleep_time", "22:00") ?: "22:00"
            val parts = sleepTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 22
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val delay = delayUntil(hour, minute)
            val work = PeriodicWorkRequestBuilder<SleepReminderWorker>(
                24, TimeUnit.HOURS
            ).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(SLEEP_WORK)
                .build()
            wm.enqueueUniquePeriodicWork(SLEEP_WORK, ExistingPeriodicWorkPolicy.KEEP, work)
        } else {
            wm.cancelUniqueWork(SLEEP_WORK)
        }

        // ── 用药提醒：每天早上 9:00 ──
        if (prefs.getBoolean("med_remind", false)) {
            val delay = delayUntil(9, 0)
            val work = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
                24, TimeUnit.HOURS
            ).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(MED_WORK)
                .build()
            wm.enqueueUniquePeriodicWork(MED_WORK, ExistingPeriodicWorkPolicy.KEEP, work)
        } else {
            wm.cancelUniqueWork(MED_WORK)
        }

        // ── 经期提醒：每天在用户设定的时间检查 ──
        if (prefs.getBoolean("period_remind", true)) {
            val periodTime = prefs.getString("period_time", "09:00") ?: "09:00"
            val parts = periodTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val delay = delayUntil(hour, minute)
            val work = PeriodicWorkRequestBuilder<PeriodReminderWorker>(
                24, TimeUnit.HOURS
            ).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(PERIOD_WORK)
                .build()
            wm.enqueueUniquePeriodicWork(PERIOD_WORK, ExistingPeriodicWorkPolicy.KEEP, work)
        } else {
            wm.cancelUniqueWork(PERIOD_WORK)
        }

        // ── 暖心小贴士：每 4 小时，首次延迟 1 小时 ──
        val tipWork = PeriodicWorkRequestBuilder<WarmTipWorker>(
            4, TimeUnit.HOURS
        ).setInitialDelay(1, TimeUnit.HOURS)
            .addTag(TIP_WORK)
            .build()
        wm.enqueueUniquePeriodicWork(TIP_WORK, ExistingPeriodicWorkPolicy.KEEP, tipWork)
    }

    /** 取消所有通知任务 */
    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WATER_WORK)
        wm.cancelUniqueWork(SLEEP_WORK)
        wm.cancelUniqueWork(MED_WORK)
        wm.cancelUniqueWork(PERIOD_WORK)
        wm.cancelUniqueWork(TIP_WORK)
    }

    /** 当用户修改提醒配置后，重新调度 */
    fun reschedule(context: Context) {
        cancelAll(context)
        scheduleAll(context)
    }
}
