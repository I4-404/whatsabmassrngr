package com.aa.autoresponder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReplyMode { AI, FIXED, OFF }

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val contactName: String,
    val mode: ReplyMode = ReplyMode.AI,
    val fixedReply: String = "",
    val customPrompt: String? = null, // لو null بيستخدم البرومبت الافتراضي
    val delaySeconds: Int = 3,
    val enabled: Boolean = true
)
