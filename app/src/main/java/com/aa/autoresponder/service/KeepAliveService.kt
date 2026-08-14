package com.aa.autoresponder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aa.autoresponder.MainActivity

/**
 * خدمة بسيطة تعمل في المقدمة (Foreground Service) بإشعار ثابت وغير مزعج.
 * وجودها بيقلل احتمالية إن النظام يقفل التطبيق تمامًا من الذاكرة،
 * لكنها لا تضمن 100% - لازم كمان استثناء توفير البطارية + تفعيل
 * "بدء تلقائي / Autostart" يدويًا من إعدادات الجهاز (خصوصًا شاومي/أوبو/هواوي).
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "keep_alive_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                channelId,
                "تشغيل الرد التلقائي",
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("الرد التلقائي شغال")
            .setContentText("بيراقب رسائل ماسنجر في الخلفية")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 501
    }
}
