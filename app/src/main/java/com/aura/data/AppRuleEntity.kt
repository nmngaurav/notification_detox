package com.aura.data

import androidx.room.Entity

@Entity(tableName = "app_rules", primaryKeys = ["packageName", "profileId"])
data class AppRuleEntity(
    val packageName: String,
    val profileId: String = "STANDARD",
    
    // V2 Fields
    val shieldLevel: ShieldLevel = ShieldLevel.SMART,
    val filterTemplate: FilterTemplate = FilterTemplate.NONE,
    val customKeywords: String = "", // Comma-separated allow/block keywords
    
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class ShieldLevel {
    OPEN,       // 0%: Allow All
    SMART,      // 50%: AI Filtering
    FORTRESS,    // 100%: Block All
    NONE        // Fallback
}

enum class FilterTemplate(val displayName: String) {
    NONE("None"),
    MESSAGING("Vital Messaging"),    
    TRANSACTIONAL("Transactional"),  
    SOCIAL("Social Focus"),         
    WORK("Deep Work"),
    GAMES("Gaming")
}
