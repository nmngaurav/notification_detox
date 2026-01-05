package com.aura.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val title: String,
    val content: String, // Potentially hashed or encrypted in future for stricter privacy
    val timestamp: Long,
    val category: String, // "promotional", "social", "update", "urgent"
    val isBlocked: Boolean
)
