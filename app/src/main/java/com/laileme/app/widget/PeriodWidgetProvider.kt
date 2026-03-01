package com.laileme.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.graphics.*
import android.os.Bundle
import android.text.TextPaint
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
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

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
                                            appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
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
            // 先设置点击事件
            val views = RemoteViews(context.packageName, R.layout.widget_period)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, openPending)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val density = context.resources.displayMetrics.density
                    val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                    val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 70)
                    val w = (minW * density).toInt().coerceAtLeast(400)
                    val h = (maxH * density).toInt().coerceAtLeast(100)

                    val bitmap = renderWidget(context, w, h)
                    val v = RemoteViews(context.packageName, R.layout.widget_period)
                    v.setImageViewBitmap(R.id.widget_canvas, bitmap)
                    v.setOnClickPendingIntent(R.id.widget_root, openPending)
                    appWidgetManager.updateAppWidget(appWidgetId, v)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private suspend fun renderWidget(context: Context, w: Int, h: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val wf = w.toFloat()
            val hf = h.toFloat()

            val font = try {
                ResourcesCompat.getFont(context, R.font.jiangcheng_yuanti) ?: Typeface.DEFAULT
            } catch (_: Exception) { Typeface.DEFAULT }
            val fontBold = Typeface.create(font, Typeface.BOLD)

            // ── 1. 白色圆角背景 ──
            val cornerR = hf * 0.18f
            val bgPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(0f, 0f, wf, hf), cornerR, cornerR, bgPaint)

            // 细边框
            val borderPaint = Paint().apply {
                color = Color.parseColor("#1AEC407A")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(0f, 0f, wf, hf), cornerR, cornerR, borderPaint)

            // ── 2. 获取数据 ──
            val data = getPeriodData(context)

            // ── 3. 左侧大数字 ──
            val daysSize = hf * 0.52f
            val daysPaint = TextPaint().apply {
                typeface = fontBold
                textSize = daysSize
                color = data.daysColor
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val daysX = wf * 0.22f
            val daysY = hf * 0.5f + daysSize * 0.35f
            canvas.drawText(data.days, daysX, daysY, daysPaint)

            // ── 4. 右侧文字 ──
            val rightX = wf * 0.62f

            // 标题
            val titleSize = hf * 0.17f
            val titlePaint = TextPaint().apply {
                typeface = font
                textSize = titleSize
                color = data.titleColor
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(data.title, rightX, hf * 0.30f, titlePaint)

            // 描述
            val descSize = hf * 0.14f
            val descPaint = TextPaint().apply {
                typeface = font
                textSize = descSize
                color = Color.parseColor("#FF9CA3AF")
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(data.desc, rightX, hf * 0.52f, descPaint)

            // 周期天数
            if (data.cycle.isNotEmpty()) {
                val cycleSize = hf * 0.12f
                val cyclePaint = TextPaint().apply {
                    typeface = font
                    textSize = cycleSize
                    color = Color.parseColor("#FFD1D5DB")
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(data.cycle, rightX, hf * 0.72f, cyclePaint)
            }

            return bitmap
        }

        private data class PeriodDisplayData(
            val title: String,
            val days: String,
            val desc: String,
            val cycle: String,
            val daysColor: Int,
            val titleColor: Int
        )

        private suspend fun getPeriodData(context: Context): PeriodDisplayData {
            return try {
                val db = AppDatabase.getDatabase(context)
                val latest = db.periodDao().getLatestCompletedRecord()
                    ?: db.periodDao().getActiveRecords().firstOrNull()

                if (latest != null) {
                    val now = System.currentTimeMillis()
                    val startMs = latest.startDate
                    val cycleLength = latest.cycleLength
                    val periodLength = latest.periodLength
                    val daysSinceStart = ((now - startMs) / (24L * 60 * 60 * 1000)).toInt()
                    val cycleDay = if (daysSinceStart >= 0) (daysSinceStart % cycleLength) + 1 else 0

                    val periodEndMs = startMs + periodLength * 24L * 60 * 60 * 1000
                    val isInPeriod = now in startMs..periodEndMs

                    if (isInPeriod) {
                        val daysLeft = ((periodEndMs - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                        PeriodDisplayData(
                            title = "经期中",
                            days = "$daysLeft",
                            desc = "天后结束",
                            cycle = "周期第 $cycleDay 天",
                            daysColor = 0xFFEF5350.toInt(),
                            titleColor = 0xFFEF5350.toInt()
                        )
                    } else {
                        val cyclesPassed = if (daysSinceStart >= 0) daysSinceStart / cycleLength else 0
                        val nextPeriodMs = startMs + (cyclesPassed + 1) * cycleLength * 24L * 60 * 60 * 1000
                        val daysUntil = ((nextPeriodMs - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                        PeriodDisplayData(
                            title = "预计",
                            days = "$daysUntil",
                            desc = "天后来",
                            cycle = "周期第 $cycleDay 天",
                            daysColor = 0xFFEC407A.toInt(),
                            titleColor = 0xFF9CA3AF.toInt()
                        )
                    }
                } else {
                    PeriodDisplayData(
                        title = "来了么",
                        days = "--",
                        desc = "点击记录经期",
                        cycle = "",
                        daysColor = 0xFF9CA3AF.toInt(),
                        titleColor = 0xFF9CA3AF.toInt()
                    )
                }
            } catch (_: Exception) {
                PeriodDisplayData(
                    title = "来了么",
                    days = "--",
                    desc = "点击记录经期",
                    cycle = "",
                    daysColor = 0xFF9CA3AF.toInt(),
                    titleColor = 0xFF9CA3AF.toInt()
                )
            }
        }
    }
}