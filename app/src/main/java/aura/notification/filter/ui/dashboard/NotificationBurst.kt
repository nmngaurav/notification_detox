package aura.notification.filter.ui.dashboard

import aura.notification.filter.data.NotificationEntity

data class NotificationBurst(
    val id: String, // Unique ID for the burst (e.g. package + timestamp)
    val packageName: String,
    val notifications: List<NotificationEntity>,
    val timestamp: Long,
    val size: Int = notifications.size
)
