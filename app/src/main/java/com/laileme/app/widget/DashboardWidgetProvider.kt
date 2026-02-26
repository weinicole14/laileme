package com.laileme.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import android.graphics.*
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.laileme.app.R
import com.laileme.app.MainActivity
import com.laileme.app.data.AppDatabase
import kotlinx.coroutines.*
import java.net.URL
import java.util.*

class DashboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
                                            appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, DashboardWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.laileme.app.DASHBOARD_REFRESH"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_dashboard)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(context, 100, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_dashboard_root, openPending)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 动态获取小部件实际尺寸
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val density = context.resources.displayMetrics.density
                    val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                    val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 150)
                    val w = (minW * density).toInt().coerceAtLeast(600)
                    val h = (maxH * density).toInt().coerceAtLeast(200)

                    val bitmap = renderWidget(context, w, h)
                    val v = RemoteViews(context.packageName, R.layout.widget_dashboard)
                    v.setImageViewBitmap(R.id.widget_canvas, bitmap)
                    v.setOnClickPendingIntent(R.id.widget_dashboard_root, openPending)
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

            // 先裁圆角，后续所有绘制都在圆角内
            val cornerR = hf * 0.08f
            val clipPath = Path().apply { addRoundRect(RectF(0f, 0f, wf, hf), cornerR, cornerR, Path.Direction.CW) }
            canvas.clipPath(clipPath)

            val font = try {
                ResourcesCompat.getFont(context, R.font.jiangcheng_yuanti) ?: Typeface.DEFAULT
            } catch (_: Exception) { Typeface.DEFAULT }
            val fontBold = Typeface.create(font, Typeface.BOLD)

            // ── 1. 背景 ──
            drawBackground(context, canvas, w, h)

            // ── 2. 数据 ──
            val authPrefs = context.getSharedPreferences("laileme_auth", Context.MODE_PRIVATE)
            val partnerPrefs = context.getSharedPreferences("laileme_partner_cache", Context.MODE_PRIVATE)

            val myNickname = authPrefs.getString("nickname", "") ?: ""
            val myAvatarUrl = authPrefs.getString("avatar_url", "") ?: ""
            val bound = partnerPrefs.getBoolean("bound", false)
            val boundAt = partnerPrefs.getLong("boundAt", 0L)
            val partnerNickname = partnerPrefs.getString("partnerNickname", "") ?: ""
            val partnerAvatarUrl = partnerPrefs.getString("partnerAvatarUrl", "") ?: ""

            // ── 3. 头像 — 相对位置 ──
            val avatarR = hf * 0.17f  // 头像占高度17%
            val avatarY = hf * 0.28f
            val leftX = wf * 0.22f
            val rightX = wf * 0.78f
            val nameSize = hf * 0.07f

            val myAvatar = loadAvatar(myAvatarUrl)
            drawAvatarCircle(canvas, myAvatar, leftX, avatarY, avatarR, Color.parseColor("#EC407A"))
            drawTextCentered(canvas, myNickname.ifEmpty { "我" }, leftX, avatarY + avatarR + nameSize + 4f, font, nameSize, Color.parseColor("#EC407A"))

            if (bound) {
                val partnerAvatar = loadAvatar(partnerAvatarUrl)
                drawAvatarCircle(canvas, partnerAvatar, rightX, avatarY, avatarR, Color.parseColor("#B39DDB"))
                drawTextCentered(canvas, partnerNickname.ifEmpty { "TA" }, rightX, avatarY + avatarR + nameSize + 4f, font, nameSize, Color.parseColor("#7E57C2"))
            }

            // ── 4. 中央：爱心 + 天数 ──
            val cx = wf / 2f
            if (bound && boundAt > 0) {
                val days = calcDays(boundAt)

                val heartSize = hf * 0.05f
                drawHeart(canvas, cx - heartSize * 1.5f, hf * 0.08f, heartSize, Color.parseColor("#FF6B9D"))
                drawHeart(canvas, cx + heartSize * 1.5f, hf * 0.06f, heartSize * 0.8f, Color.parseColor("#FFB0C8"))

                drawTextCentered(canvas, "相守", cx, hf * 0.25f, font, hf * 0.08f, Color.parseColor("#8E7A9A"))
                drawTextCentered(canvas, "$days", cx, hf * 0.50f, fontBold, hf * 0.22f, Color.parseColor("#6C5BAE"))
                drawTextCentered(canvas, "days", cx, hf * 0.58f, font, hf * 0.07f, Color.parseColor("#A090B8"))
            } else {
                drawTextCentered(canvas, "来了么", cx, hf * 0.30f, fontBold, hf * 0.12f, Color.parseColor("#EC407A"))
                drawTextCentered(canvas, "等待绑定伴侣", cx, hf * 0.45f, font, hf * 0.07f, Color.parseColor("#B0A0B8"))
            }

            // ── 5. 底部卡片 ──
            drawPeriodCard(context, canvas, w, h, font, fontBold)

            return bitmap
        }

        private fun drawBackground(context: Context, canvas: Canvas, w: Int, h: Int) {
            val wf = w.toFloat(); val hf = h.toFloat()
            try {
                val bg = BitmapFactory.decodeResource(context.resources, R.drawable.widget_bg_rabbit)
                if (bg != null) {
                    // 直接铺满，不加圆角裁剪，让系统小部件自动圆角
                    val scale = maxOf(wf / bg.width, hf / bg.height)
                    val sw = bg.width * scale; val sh = bg.height * scale
                    canvas.drawBitmap(bg, null, RectF((wf - sw) / 2f, (hf - sh) / 2f, (wf + sw) / 2f, (hf + sh) / 2f),
                        Paint(Paint.ANTI_ALIAS_FLAG))
                    bg.recycle()
                } else drawFallbackBg(canvas, w, h)
            } catch (_: Exception) { drawFallbackBg(canvas, w, h) }
        }

        private fun drawFallbackBg(canvas: Canvas, w: Int, h: Int) {
            val wf = w.toFloat(); val hf = h.toFloat()
            val p = Paint().apply {
                shader = LinearGradient(0f, 0f, wf, hf,
                    Color.parseColor("#F8E8F0"), Color.parseColor("#E0D0F8"), Shader.TileMode.CLAMP)
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, wf, hf, p)
        }

        private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
            val paint = Paint().apply { this.color = color; style = Paint.Style.FILL; isAntiAlias = true }
            val path = Path()
            path.moveTo(cx, cy + size * 0.4f)
            path.cubicTo(cx - size * 1.2f, cy - size * 0.6f, cx - size * 0.4f, cy - size * 1.2f, cx, cy - size * 0.4f)
            path.cubicTo(cx + size * 0.4f, cy - size * 1.2f, cx + size * 1.2f, cy - size * 0.6f, cx, cy + size * 0.4f)
            path.close()
            canvas.drawPath(path, paint)
        }

        private fun drawAvatarCircle(canvas: Canvas, avatar: Bitmap?, cx: Float, cy: Float, r: Float, borderColor: Int) {
            val whitePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
            canvas.drawCircle(cx, cy, r + 4f, whitePaint)

            val borderPaint = Paint().apply {
                color = borderColor; style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true
            }
            canvas.drawCircle(cx, cy, r + 4f, borderPaint)

            if (avatar != null) {
                canvas.save()
                val path = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
                canvas.clipPath(path)
                val scale = maxOf(r * 2f / avatar.width, r * 2f / avatar.height)
                val aw = avatar.width * scale; val ah = avatar.height * scale
                canvas.drawBitmap(avatar, null, RectF(cx - aw / 2f, cy - ah / 2f, cx + aw / 2f, cy + ah / 2f),
                    Paint(Paint.ANTI_ALIAS_FLAG))
                canvas.restore()
            } else {
                val defPaint = Paint().apply { color = borderColor; alpha = 100; style = Paint.Style.FILL; isAntiAlias = true }
                canvas.drawCircle(cx, cy - r * 0.15f, r * 0.28f, defPaint)
                canvas.drawRoundRect(RectF(cx - r * 0.35f, cy + r * 0.15f, cx + r * 0.35f, cy + r * 0.6f),
                    r * 0.18f, r * 0.18f, defPaint)
            }
        }

        private fun drawTextCentered(canvas: Canvas, text: String, x: Float, y: Float,
                                      typeface: Typeface, size: Float, color: Int) {
            val paint = TextPaint().apply {
                this.typeface = typeface; textSize = size; this.color = color
                textAlign = Paint.Align.CENTER; isAntiAlias = true
            }
            canvas.drawText(text, x, y, paint)
        }

        private suspend fun drawPeriodCard(context: Context, canvas: Canvas, w: Int, h: Int,
                                            font: Typeface, fontBold: Typeface) {
            val wf = w.toFloat(); val hf = h.toFloat()
            val cardPaint = Paint().apply { color = Color.parseColor("#C0FFFFFF"); isAntiAlias = true }
            val cardStroke = Paint().apply {
                color = Color.parseColor("#30B8A0D0"); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true
            }

            val cardH = hf * 0.24f
            val cardTop = hf * 0.68f
            val marginX = wf * 0.05f
            val r = hf * 0.05f
            val cardRect = RectF(marginX, cardTop, wf - marginX, cardTop + cardH)
            canvas.drawRoundRect(cardRect, r, r, cardPaint)
            canvas.drawRoundRect(cardRect, r, r, cardStroke)

            val (title, value) = getPeriodStatus(context)
            val titleSize = hf * 0.065f
            val valueSize = hf * 0.08f
            drawTextCentered(canvas, title, cardRect.centerX(), cardRect.centerY() - valueSize * 0.15f, font, titleSize, Color.parseColor("#8E7A8A"))
            drawTextCentered(canvas, value, cardRect.centerX(), cardRect.centerY() + titleSize + 4f, fontBold, valueSize, Color.parseColor("#EF5350"))
        }

        private suspend fun getPeriodStatus(context: Context): Pair<String, String> {
            return try {
                val db = AppDatabase.getDatabase(context)
                val latest = db.periodDao().getLatestCompletedRecord()
                    ?: db.periodDao().getActiveRecords().firstOrNull()
                    ?: return Pair("经期", "未记录")

                val now = System.currentTimeMillis()
                val completedCount = db.periodDao().getAllList().count { it.endDate != null }
                val isFirst = completedCount < 2
                val active = latest.endDate == null

                when {
                    isFirst && active -> Pair("经期中", "首次记录")
                    isFirst -> Pair("周期", "收集中")
                    active -> {
                        val d = ((now - latest.startDate) / (24L * 3600000)).toInt()
                        val left = (latest.periodLength - d - 1).coerceAtLeast(0)
                        Pair("经期中", "${left}天后结束")
                    }
                    else -> {
                        val d = ((now - latest.startDate) / (24L * 3600000)).toInt()
                        val cp = if (d >= 0) d / latest.cycleLength else 0
                        val nextMs = latest.startDate + (cp + 1) * latest.cycleLength * 24L * 3600000
                        val until = ((nextMs - now) / (24L * 3600000)).toInt().coerceAtLeast(0)
                        Pair("距下次经期", "${until}天")
                    }
                }
            } catch (_: Exception) {
                Pair("经期", "--")
            }
        }

        private suspend fun loadAvatar(url: String): Bitmap? {
            if (url.isBlank()) return null
            return try {
                val fullUrl = if (!url.startsWith("http")) "http://47.123.5.171:8080$url" else url
                withContext(Dispatchers.IO) {
                    val stream = URL(fullUrl).openStream()
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream.close()
                    bmp
                }
            } catch (_: Exception) { null }
        }

        private fun calcDays(boundAt: Long): Int {
            val cal1 = Calendar.getInstance().apply { timeInMillis = boundAt }
            val cal2 = Calendar.getInstance()
            val d1 = cal1.get(Calendar.YEAR) * 366L + cal1.get(Calendar.DAY_OF_YEAR)
            val d2 = cal2.get(Calendar.YEAR) * 366L + cal2.get(Calendar.DAY_OF_YEAR)
            return (d2 - d1).toInt() + 1
        }
    }
}