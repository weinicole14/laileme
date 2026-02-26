package com.laileme.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import com.laileme.app.R
import com.laileme.app.MainActivity
import com.laileme.app.data.AppDatabase
import kotlinx.coroutines.*

class PeriodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null) {
                onUpdate(context, appWidgetManager, ids)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.laileme.app.WIDGET_REFRESH"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val latest = db.periodDao().getLatestCompletedRecord()
                        ?: db.periodDao().getActiveRecords().firstOrNull()

                    val views = RemoteViews(context.packageName, R.layout.widget_period)

                    if (latest != null) {
                        val now = System.currentTimeMillis()
                        val startMs = latest.startDate
                        val cycleLength = latest.cycleLength
                        val periodLength = latest.periodLength
                        val daysSinceStart = ((now - startMs) / (24L * 60 * 60 * 1000)).toInt()
                        val cycleDay = if (daysSinceStart >= 0) (daysSinceStart % cycleLength) + 1 else 0

                        val periodEndMs = startMs + periodLength * 24L * 60 * 60 * 1000
                        val isInPeriod = latest.endDate == null && now in startMs..periodEndMs
                                || now in startMs..periodEndMs

                        if (isInPeriod) {
                            val daysLeft = ((periodEndMs - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                            views.setTextViewText(R.id.widget_title, "经期中")
                            views.setTextViewText(R.id.widget_days, "$daysLeft")
                            views.setTextViewText(R.id.widget_desc, "天后结束")
                            views.setTextColor(R.id.widget_days, 0xFFEF5350.toInt()) // PeriodRed
                            views.setTextColor(R.id.widget_title, 0xFFEF5350.toInt())
                        } else {
                            val cyclesPassed = if (daysSinceStart >= 0) daysSinceStart / cycleLength else 0
                            val nextPeriodMs = startMs + (cyclesPassed + 1) * cycleLength * 24L * 60 * 60 * 1000
                            val daysUntil = ((nextPeriodMs - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                            views.setTextViewText(R.id.widget_title, "预计")
                            views.setTextViewText(R.id.widget_days, "$daysUntil")
                            views.setTextViewText(R.id.widget_desc, "天后来")
                            views.setTextColor(R.id.widget_days, 0xFFEC407A.toInt()) // PrimaryPink
                            views.setTextColor(R.id.widget_title, 0xFF9CA3AF.toInt())
                        }
                        views.setTextViewText(R.id.widget_cycle, "周期第 $cycleDay 天")
                    } else {
                        views.setTextViewText(R.id.widget_title, "来了么")
                        views.setTextViewText(R.id.widget_days, "--")
                        views.setTextViewText(R.id.widget_desc, "点击记录经期")
                        views.setTextViewText(R.id.widget_cycle, "")
                        views.setTextColor(R.id.widget_days, 0xFF9CA3AF.toInt())
                    }

                    // 点击打开app
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}