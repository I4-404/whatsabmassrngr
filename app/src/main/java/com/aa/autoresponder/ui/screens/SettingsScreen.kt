package com.aa.autoresponder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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

    val themeMode by Prefs.themeMode(ctx).collectAsState(initial = "SYSTEM")

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

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("مظهر التطبيق", style = MaterialTheme.typography.titleSmall)
                ThemeOption("تلقائي (حسب النظام)", Icons.Default.Brightness4, themeMode == "SYSTEM") {
                    scope.launch { Prefs.setThemeMode(ctx, "SYSTEM") }
                }
                ThemeOption("فاتح", Icons.Default.LightMode, themeMode == "LIGHT") {
                    scope.launch { Prefs.setThemeMode(ctx, "LIGHT") }
                }
                ThemeOption("غامق", Icons.Default.DarkMode, themeMode == "DARK") {
                    scope.launch { Prefs.setThemeMode(ctx, "DARK") }
                }
            }
        }

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

@Composable
private fun ThemeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}