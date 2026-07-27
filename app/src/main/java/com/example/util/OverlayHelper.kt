package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.db.AppDatabase
import com.example.db.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * إشعار عائم (Overlay) يظهر فوق أي تطبيق آخر عند اكتشاف جهاز جديد على الشبكة،
 * مع أزرار لإضافة الوقت مباشرة بدون فتح التطبيق.
 */
object OverlayHelper {

    fun canShowOverlay(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun showNewDeviceOverlay(context: Context, ip: String, name: String) {
        if (!AppPrefs.isOverlayEnabled(context)) return
        if (!canShowOverlay(context)) return

        try {
            val appContext = context.applicationContext
            val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val ipSuffix = ip.substringAfterLast(".")

            val container = LinearLayout(appContext).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(36, 28, 36, 28)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E61C1C2E"))
                    cornerRadius = 26f
                }
            }

            val title = TextView(appContext).apply {
                text = "جهاز جديد: $name  (...$ipSuffix)"
                setTextColor(Color.WHITE)
                textSize = 15f
            }
            container.addView(title)

            val buttonsRow = LinearLayout(appContext).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 20, 0, 0)
            }

            var rootView: View? = null
            val safeRemove: () -> Unit = {
                rootView?.let {
                    try { windowManager.removeView(it) } catch (_: Exception) {}
                }
            }

            for (hours in AppPrefs.getQuickTimes(appContext)) {
                val btn = Button(appContext).apply {
                    text = AppPrefs.formatHoursLabel(hours)
                    textSize = 12f
                    setOnClickListener {
                        applyTime(appContext, ip, name, hours)
                        safeRemove()
                    }
                }
                buttonsRow.addView(btn)
            }

            val dismissBtn = Button(appContext).apply {
                text = "تجاهل"
                textSize = 12f
                setOnClickListener { safeRemove() }
            }
            buttonsRow.addView(dismissBtn)
            container.addView(buttonsRow)
            rootView = container

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = 90
            }

            windowManager.addView(container, params)

            // إخفاء تلقائي بعد 25 ثانية لو ما تفاعل معاه المستخدم
            Handler(Looper.getMainLooper()).postDelayed({ safeRemove() }, 25000)
        } catch (_: Exception) {
            // لو فشل عرض الـ overlay لأي سبب (إذن ملغى، أو خطأ نظام)، نتجاهل بصمت
            // ويبقى الإشعار العادي (Notification) شغال كبديل.
        }
    }

    private fun applyTime(context: Context, ip: String, name: String, hours: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val existing = db.deviceDao().getByIp(ip)
            val additionalMillis = (hours * 3600000.0).toLong()
            if (existing != null) {
                val newEndTime = if (existing.isPaused) {
                    System.currentTimeMillis() + existing.remainingWhenPaused + additionalMillis
                } else {
                    maxOf(System.currentTimeMillis(), existing.endTime) + additionalMillis
                }
                db.deviceDao().update(existing.copy(endTime = newEndTime, isPaused = false, remainingWhenPaused = 0))
            } else {
                db.deviceDao().insert(
                    Device(ip = ip, name = "$name ($ip)", endTime = System.currentTimeMillis() + additionalMillis)
                )
            }
        }
    }
}
