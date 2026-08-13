package com.aa.autoresponder.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * خدمة احتياطية: الرد الفعلي بيحصل من خلال MessengerNotificationListener
 * باستخدام زر "الرد السريع" في الإشعار (RemoteInput)، وهي الطريقة الأكثر
 * ثباتًا ولا تحتاج فتح واجهة ماسنجر.
 *
 * تم ترك هذه الخدمة كنقطة توسّع مستقبلية: لو حبيت تضيف قراءة سياق إضافي
 * من شاشة المحادثة المفتوحة فعليًا (مثلاً آخر 5 رسائل بدل رسالة واحدة)
 * تقدر تستخدم onAccessibilityEvent هنا مع rootInActiveWindow لتحليل شجرة
 * العناصر الظاهرة على الشاشة.
 */
class MessengerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // متروكة فارغة حاليًا - نقطة توسع مستقبلية
    }

    override fun onInterrupt() {
        Log.d("MsgAutoResponder", "تم إيقاف خدمة Accessibility")
    }
}
