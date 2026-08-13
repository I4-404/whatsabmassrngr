package com.aa.autoresponder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val incomingMessage: String,
    val replyMessage: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
