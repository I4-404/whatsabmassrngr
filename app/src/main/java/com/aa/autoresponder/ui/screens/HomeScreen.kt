package com.aa.autoresponder.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aa.autoresponder.data.AppDatabase
import com.aa.autoresponder.data.LogEntity
import com.aa.autoresponder.service.KeepAliveService
import com.aa.autoresponder.util.Prefs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("hh:mm a", Locale("ar"))
private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val masterEnabled by Prefs.masterEnabled(ctx).collectAsState(initial = false)

    val db = remember { AppDatabase.get(ctx) }
    val logs by db.logDao().observeRecent().collectAsState(initial = emptyList())
    val totalReplies = remember(logs) { logs.count { it.success } }
    val lastLog = remember(logs) { logs.firstOrNull() }

    var guideExpanded by remember { mutableStateOf(false) }

    // لما التفعيل العام يبقى شغال، شغّل خدمة البقاء في الخلفية تلقائيًا
    LaunchedEffect(masterEnabled) {
        if (masterEnabled) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, KeepAliveService::class.java))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ===== الترويسة =====
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "الرد التلقائي لماسنجر",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "يقرأ التطبيق إشعارات ماسنجر الجديدة، ويرد تلقائيًا باستخدام Gemini أو رسالة ثابتة حسب القاعدة المحددة لكل جهة اتصال.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ===== كارت التفعيل الرئيسي =====
        StatusCard(masterEnabled = masterEnabled) { value ->
            scope.launch { Prefs.setMasterEnabled(ctx, value) }
        }

        // ===== صف الإحصائيات =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                iconTint = MaterialTheme.colorScheme.primary,
                value = totalReplies.toString(),
                label = "ردود تم إرسالها"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.NotificationsActive,
                iconTint = MaterialTheme.colorScheme.tertiary,
                value = logs.size.toString(),
                label = "إشعارات مسجّلة"
            )
        }

        // ===== كارت آخر إشعار وصل =====
        LastNotificationCard(lastLog)

        Divider()

        // ===== دليل الإعداد (قابل للطي) =====
        SetupGuideSection(
            expanded = guideExpanded,
            onToggle = { guideExpanded = !guideExpanded },
            ctx = ctx
        )

        // ===== ملاحظة مهمة =====
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ملاحظة مهمة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "الرد يتم عن طريق زر \"الرد السريع\" الموجود داخل إشعار ماسنجر نفسه. " +
                            "لو ماسنجر ما بيظهرش زر رد سريع لمحادثة معينة (مثلاً أول رسالة أو محادثة جماعية جديدة) هيتم تجاهلها.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(masterEnabled: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (masterEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (masterEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text("تفعيل الرد التلقائي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (masterEnabled) "الخدمة شغالة الآن" else "الخدمة متوقفة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = masterEnabled,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LastNotificationCard(lastLog: LogEntity?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Filled.MarkChatUnread,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text("آخر إشعار وصل", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            if (lastLog == null) {
                Text(
                    "لسه ما وصلش أي إشعار",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lastLog.contactName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    StatusChip(success = lastLog.success)
                }

                Text(
                    lastLog.incomingMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(timeFormatter.format(Date(lastLog.timestamp)), style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateFormatter.format(Date(lastLog.timestamp)), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(success: Boolean) {
    val bg = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val fg = if (success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (success) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp)
        )
        Text(if (success) "تم الرد" else "فشل", style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun SetupGuideSection(expanded: Boolean, onToggle: () -> Unit, ctx: android.content.Context) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Rule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("دليل الإعداد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "طي" else "توسيع"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StepCard(
                        number = 1,
                        icon = Icons.Filled.NotificationsActive,
                        title = "منح صلاحية الوصول للإشعارات",
                        description = "لازم تسمح للتطبيق بقراءة إشعارات ماسنجر عشان يعرف الرسائل الجديدة.",
                        buttonText = "فتح إعدادات الإشعارات"
                    ) {
                        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }

                    StepCard(
                        number = 2,
                        icon = Icons.Filled.BatteryChargingFull,
                        title = "إيقاف توفير البطارية للتطبيق",
                        description = "أهم خطوة عشان التطبيق ميتقفلش من النظام في الخلفية. دوس الزرار واختار \"عدم التقييد / Unrestricted\" أو \"السماح\".",
                        buttonText = "طلب استثناء البطارية"
                    ) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${ctx.packageName}")
                        }
                        try {
                            ctx.startActivity(intent)
                        } catch (e: Exception) {
                            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }

                    StepCard(
                        number = 3,
                        icon = Icons.Filled.Sync,
                        title = "تفعيل التشغيل التلقائي (Autostart)",
                        description = "لو جهازك شاومي/أوبو/فيفو/هواوي، لازم كمان تفتح إعدادات الجهاز يدويًا: التطبيقات ← الرد التلقائي لماسنجر ← فعّل \"بدء تلقائي/Autostart\" ووسّع صلاحيات البطارية لـ\"بدون قيود\". كمان افتح شاشة التطبيقات الأخيرة واعمل قفل (أيقونة القفل) على التطبيق عشان النظام ميقفلوش.",
                        buttonText = null
                    ) {}

                    StepCard(
                        number = 4,
                        icon = Icons.Filled.VpnKey,
                        title = "إضافة مفتاح Gemini API",
                        description = "من تبويب الإعدادات، أضف مفتاح Gemini API الخاص بيك عشان يقدر يولّد الردود.",
                        buttonText = null
                    ) {}

                    StepCard(
                        number = 5,
                        icon = Icons.Filled.Rule,
                        title = "تحديد قواعد الردود",
                        description = "من تبويب \"قواعد الردود\" حدد لكل شخص: رد بالذكاء الاصطناعي، رد ثابت، أو تعطيل الرد.",
                        buttonText = null
                    ) {}

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String?,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Text(description, style = MaterialTheme.typography.bodySmall)
                if (buttonText != null) {
                    OutlinedButton(onClick = onClick) { Text(buttonText) }
                }
            }
        }
    }
}