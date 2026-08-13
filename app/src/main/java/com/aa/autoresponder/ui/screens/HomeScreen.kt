package com.aa.autoresponder.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aa.autoresponder.util.Prefs
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val masterEnabled by Prefs.masterEnabled(ctx).collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("الرد التلقائي لماسنجر", style = MaterialTheme.typography.headlineSmall)
        Text(
            "يقرأ التطبيق إشعارات ماسنجر الجديدة، ويرد تلقائيًا باستخدام Gemini أو رسالة ثابتة حسب القاعدة المحددة لكل جهة اتصال.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("تفعيل الرد التلقائي", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = masterEnabled,
                    onCheckedChange = { value ->
                        scope.launch { Prefs.setMasterEnabled(ctx, value) }
                    }
                )
            }
        }

        Divider()

        Text("الخطوات المطلوبة للتشغيل:", style = MaterialTheme.typography.titleMedium)

        StepCard(
            number = 1,
            title = "منح صلاحية الوصول للإشعارات",
            description = "لازم تسمح للتطبيق بقراءة إشعارات ماسنجر عشان يعرف الرسائل الجديدة.",
            buttonText = "فتح إعدادات الإشعارات"
        ) {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        StepCard(
            number = 2,
            title = "إضافة مفتاح Gemini API",
            description = "من تبويب الإعدادات، أضف مفتاح Gemini API الخاص بيك عشان يقدر يولّد الردود.",
            buttonText = null
        ) {}

        StepCard(
            number = 3,
            title = "تحديد قواعد الردود",
            description = "من تبويب \"قواعد الردود\" حدد لكل شخص: رد بالذكاء الاصطناعي، رد ثابت، أو تعطيل الرد.",
            buttonText = null
        ) {}

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ملاحظة مهمة", style = MaterialTheme.typography.titleSmall)
                Text(
                    "الرد يتم عن طريق زر \"الرد السريع\" الموجود داخل إشعار ماسنجر نفسه. " +
                        "لو ماسنجر ما بيظهرش زر رد سريع لمحادثة معينة (مثلاً أول رسالة أو محادثة جماعية جديدة) هيتم تجاهلها.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    description: String,
    buttonText: String?,
    onClick: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$number. $title", style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (buttonText != null) {
                Button(onClick = onClick) { Text(buttonText) }
            }
        }
    }
}
