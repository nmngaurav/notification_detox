package aura.notification.filter.data

import android.graphics.drawable.Drawable

data class DigestItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val summary: String?, // Nullable: null = show list
    val count: Int,
    val notifications: List<NotificationEntity>
)
