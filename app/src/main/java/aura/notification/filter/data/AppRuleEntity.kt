package aura.notification.filter.data

import androidx.room.Entity

@Entity(tableName = "app_rules", primaryKeys = ["packageName", "profileId"])
data class AppRuleEntity(
    val packageName: String,
    val profileId: String = "STANDARD",
    
    // V2/V3 Fields
    val shieldLevel: ShieldLevel = ShieldLevel.SMART,
    val filterTemplate: DetoxCategory = DetoxCategory.SOCIAL, // Legacy for single template
    val activeCategories: String = "", // Comma-separated DetoxCategory names
    val customKeywords: String = "", // Comma-separated allow/block keywords
    
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class ShieldLevel {
    OPEN,       // 0%: Allow All
    SMART,      // 50%: AI Filtering
    FORTRESS,    // 100%: Block All
    NONE        // Fallback
}

enum class DetoxCategory(val displayName: String) {
    SOCIAL("Social"),
    FINANCES("Finances"),
    LOGISTICS("Deliveries"),
    EVENTS("Events"),
    GAMIFICATION("Gamification"),
    PROMOS("Promos"),
    UPDATES("Updates")
}
