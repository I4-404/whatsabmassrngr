package com.aa.autoresponder.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aa.autoresponder.ai.GeminiClient
import com.aa.autoresponder.data.AppDatabase
import com.aa.autoresponder.data.LogEntity
import com.aa.autoresponder.data.ReplyMode
import com.aa.autoresponder.data.RuleEntity
import com.aa.autoresponder.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * تلتقط هذه الخدمة إشعارات ماسنجر الواردة، وتستخدم زر "الرد السريع" الموجود
 * داخل الإشعار (RemoteInput) لإرسال رد تلقائي دون الحاجة لفتح التطبيق.
 * هذه هي نفس الطريقة التي تعمل بها تطبيقات AutoResponder المعروفة.
 */
class MessengerNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val targetPackages = setOf(
        "com.facebook.orca",   // Messenger
        "com.facebook.mlite"   // Messenger Lite
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in targetPackages) return

        val extras = sbn.notification.extras
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val message = extractMessageText(sbn.notification)

        if (sender.isBlank() || message.isBlank()) return
        // تجاهل الإشعارات المُجمّعة (ملخص عدة محادثات) والرسائل المرسلة من نفسي
        if (sender.equals("Messenger", ignoreCase = true)) return

        val replyAction = findReplyAction(sbn.notification) ?: run {
            Log.d(TAG, "لا يوجد زر رد سريع لهذا الإشعار من $sender")
            return
        }

        scope.launch {
            handleIncoming(sender, message, sbn, replyAction)
        }
    }

    private suspend fun handleIncoming(
        sender: String,
        message: String,
        sbn: StatusBarNotification,
        replyAction: Notification.Action
    ) {
        val ctx = applicationContext
        val masterEnabled = Prefs.masterEnabled(ctx).first()
        if (!masterEnabled) return

        val db = AppDatabase.get(ctx)
        val rule = db.ruleDao().findByName(sender) ?: RuleEntity(contactName = sender)
        if (!rule.enabled || rule.mode == ReplyMode.OFF) return

        val replyText: String = when (rule.mode) {
            ReplyMode.FIXED -> rule.fixedReply.ifBlank { return }
            ReplyMode.AI -> {
                val apiKey = Prefs.apiKey(ctx).first()
                val prompt = rule.customPrompt ?: Prefs.defaultPrompt(ctx).first()
                val result = GeminiClient.generateReply(apiKey, prompt, sender, message)
                result.getOrElse { err ->
                    Log.e(TAG, "خطأ Gemini: ${err.message}")
                    db.logDao().insert(
                        LogEntity(
                            contactName = sender,
                            incomingMessage = message,
                            replyMessage = "فشل توليد الرد: ${err.message}",
                            success = false
                        )
                    )
                    return
                }
            }
            ReplyMode.OFF -> return
        }

        val delaySec = if (rule.mode == ReplyMode.AI) rule.delaySeconds else rule.delaySeconds
        if (delaySec > 0) delay(delaySec * 1000L)

        val sent = sendReply(replyAction, replyText)

        db.logDao().insert(
            LogEntity(
                contactName = sender,
                incomingMessage = message,
                replyMessage = replyText,
                success = sent
            )
        )
    }

    /** يبحث عن الـ Action اللي بيحمل RemoteInput (زر "رد" السريع) */
    private fun findReplyAction(notification: Notification): Notification.Action? =
        notification.actions?.firstOrNull { action -> action.remoteInputs?.isNotEmpty() == true }

    /** يملأ RemoteInput بالنص المطلوب ويبعت الـ PendingIntent الخاص بالرد */
    private fun sendReply(action: Notification.Action, text: String): Boolean {
        return try {
            val remoteInputs = action.remoteInputs ?: return false
            val intent = Intent()
            val bundle = Bundle()
            for (ri in remoteInputs) {
                bundle.putCharSequence(ri.resultKey, text)
            }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            action.actionIntent.send(applicationContext, 0, intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "فشل إرسال الرد: ${e.message}")
            false
        }
    }

    /** استخراج نص الرسالة من الإشعار (يدعم أيضًا إشعارات النمط المحادثي MessagingStyle) */
    private fun extractMessageText(notification: Notification): String {
        val extras = notification.extras

        // نمط المحادثات (Messaging Style) - يحتوي على آخر الرسائل
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            val last = Notification.MessagingStyle.Message.getMessagesFromBundleArray(messages)
                .lastOrNull()
            if (last != null) return last.text?.toString().orEmpty()
        }

        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    }

    companion object {
        private const val TAG = "MsgAutoResponder"
    }
}
