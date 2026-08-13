package com.aa.autoresponder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aa.autoresponder.util.Prefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var defaultPrompt by remember { mutableStateOf("") }
    var defaultDelay by remember { mutableStateOf("3") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apiKey = Prefs.apiKey(ctx).first()
        defaultPrompt = Prefs.defaultPrompt(ctx).first()
        defaultDelay = Prefs.defaultDelaySeconds(ctx).first().toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("الإعدادات", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; saved = false },
            label = { Text("مفتاح Gemini API") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = defaultPrompt,
            onValueChange = { defaultPrompt = it; saved = false },
            label = { Text("برومبت الرد الافتراضي (الشخصية/الأسلوب)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        OutlinedTextField(
            value = defaultDelay,
            onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 3) { defaultDelay = v; saved = false } },
            label = { Text("مدة الانتظار قبل الرد (بالثواني)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(onClick = {
            scope.launch {
                Prefs.setApiKey(ctx, apiKey.trim())
                Prefs.setDefaultPrompt(ctx, defaultPrompt)
                Prefs.setDefaultDelaySeconds(ctx, defaultDelay.toIntOrNull() ?: 3)
                saved = true
            }
        }) {
            Text("حفظ الإعدادات")
        }

        if (saved) {
            Text("تم الحفظ ✓", style = MaterialTheme.typography.bodySmall)
        }

        Text(
            "تقدر تحصل على مفتاح Gemini API مجانًا من Google AI Studio.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
