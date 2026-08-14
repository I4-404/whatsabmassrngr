package com.aa.autoresponder.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object Prefs {
    private val KEY_API = stringPreferencesKey("gemini_api_key")
    private val KEY_MASTER_ENABLED = booleanPreferencesKey("master_enabled")
    private val KEY_DEFAULT_PROMPT = stringPreferencesKey("default_prompt")
    private val KEY_DEFAULT_DELAY = intPreferencesKey("default_delay_seconds")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

    fun themeMode(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_THEME_MODE] ?: "SYSTEM" }

    suspend fun setThemeMode(ctx: Context, value: String) {
        ctx.dataStore.edit { it[KEY_THEME_MODE] = value }
    }

    fun apiKey(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_API] ?: "" }

    suspend fun setApiKey(ctx: Context, value: String) {
        ctx.dataStore.edit { it[KEY_API] = value }
    }

    fun masterEnabled(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { it[KEY_MASTER_ENABLED] ?: false }

    suspend fun setMasterEnabled(ctx: Context, value: Boolean) {
        ctx.dataStore.edit { it[KEY_MASTER_ENABLED] = value }
    }

    fun defaultPrompt(ctx: Context): Flow<String> =
        ctx.dataStore.data.map {
            it[KEY_DEFAULT_PROMPT]
                ?: "أنت تتحدث نيابة عني على ماسنجر. رد برسالة قصيرة، ودودة، ومناسبة للسياق، بدون مقدمات."
        }

    suspend fun setDefaultPrompt(ctx: Context, value: String) {
        ctx.dataStore.edit { it[KEY_DEFAULT_PROMPT] = value }
    }

    fun defaultDelaySeconds(ctx: Context): Flow<Int> =
        ctx.dataStore.data.map { it[KEY_DEFAULT_DELAY] ?: 3 }

    suspend fun setDefaultDelaySeconds(ctx: Context, value: Int) {
        ctx.dataStore.edit { it[KEY_DEFAULT_DELAY] = value }
    }
}