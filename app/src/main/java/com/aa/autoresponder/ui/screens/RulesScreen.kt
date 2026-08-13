package com.aa.autoresponder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aa.autoresponder.data.AppDatabase
import com.aa.autoresponder.data.ReplyMode
import com.aa.autoresponder.data.RuleEntity
import kotlinx.coroutines.launch

@Composable
fun RulesScreen() {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()
    val rules by db.ruleDao().observeAll().collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RuleEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRule = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة قاعدة")
            }
        }
    ) { padding ->
        if (rules.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "لا توجد قواعد بعد. اضغط + لإضافة اسم جهة اتصال وتحديد طريقة الرد عليها.",
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(padding)) {
                items(rules) { rule ->
                    RuleRow(
                        rule = rule,
                        onClick = { editingRule = rule; showDialog = true },
                        onDelete = { scope.launch { db.ruleDao().delete(rule) } }
                    )
                }
            }
        }
    }

    if (showDialog) {
        RuleDialog(
            initial = editingRule,
            onDismiss = { showDialog = false },
            onSave = { rule ->
                scope.launch { db.ruleDao().upsert(rule) }
                showDialog = false
            }
        )
    }
}

@Composable
private fun RuleRow(rule: RuleEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).clickableSafe(onClick)) {
                Text(rule.contactName, style = MaterialTheme.typography.titleSmall)
                Text(
                    when (rule.mode) {
                        ReplyMode.AI -> "رد بالذكاء الاصطناعي" + if (!rule.enabled) " (معطّل)" else ""
                        ReplyMode.FIXED -> "رد ثابت: ${rule.fixedReply}" + if (!rule.enabled) " (معطّل)" else ""
                        ReplyMode.OFF -> "بدون رد"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف")
            }
        }
    }
}

// اختصار بسيط لجعل العمود قابلاً للنقر بدون استيراد إضافي معقد
private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))

@Composable
private fun RuleDialog(
    initial: RuleEntity?,
    onDismiss: () -> Unit,
    onSave: (RuleEntity) -> Unit
) {
    var contactName by remember { mutableStateOf(initial?.contactName ?: "") }
    var mode by remember { mutableStateOf(initial?.mode ?: ReplyMode.AI) }
    var fixedReply by remember { mutableStateOf(initial?.fixedReply ?: "") }
    var customPrompt by remember { mutableStateOf(initial?.customPrompt ?: "") }
    var delaySeconds by remember { mutableStateOf((initial?.delaySeconds ?: 3).toString()) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة قاعدة رد" else "تعديل القاعدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("اسم جهة الاتصال (كما يظهر في الإشعار)") },
                    enabled = initial == null,
                    singleLine = true
                )

                Text("طريقة الرد:", style = MaterialTheme.typography.labelMedium)
                ModeOption("رد بالذكاء الاصطناعي (Gemini)", mode == ReplyMode.AI) { mode = ReplyMode.AI }
                ModeOption("رد ثابت", mode == ReplyMode.FIXED) { mode = ReplyMode.FIXED }
                ModeOption("بدون رد", mode == ReplyMode.OFF) { mode = ReplyMode.OFF }

                if (mode == ReplyMode.FIXED) {
                    OutlinedTextField(
                        value = fixedReply,
                        onValueChange = { fixedReply = it },
                        label = { Text("نص الرد الثابت") },
                        minLines = 2
                    )
                }

                if (mode == ReplyMode.AI) {
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text("برومبت مخصص لهذا الشخص (اختياري)") },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = delaySeconds,
                        onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 3) delaySeconds = v },
                        label = { Text("مدة الانتظار قبل الرد (ثواني)") },
                        singleLine = true
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("تفعيل هذه القاعدة")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (contactName.isNotBlank()) {
                    onSave(
                        RuleEntity(
                            contactName = contactName.trim(),
                            mode = mode,
                            fixedReply = fixedReply,
                            customPrompt = customPrompt.ifBlank { null },
                            delaySeconds = delaySeconds.toIntOrNull() ?: 3,
                            enabled = enabled
                        )
                    )
                }
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun ModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickableSafe(onClick)) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}
