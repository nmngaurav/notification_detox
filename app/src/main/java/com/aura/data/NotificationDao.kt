package com.aura.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>



    @Query("SELECT * FROM notifications WHERE isBlocked = 1 ORDER BY timestamp DESC")
    fun getBlockedNotifications(): Flow<List<NotificationEntity>>

    // Get notifications for a specific package, e.g. for the Digest view
    @Query("SELECT * FROM notifications WHERE packageName = :pkgName AND isBlocked = 1 ORDER BY timestamp DESC")
    fun getBlockedNotificationsForPackage(pkgName: String): Flow<List<NotificationEntity>>
    
    @Query("DELETE FROM notifications WHERE timestamp < :threshold")
    suspend fun deleteOldNotifications(threshold: Long)
    @Query("DELETE FROM notifications WHERE packageName = :pkgName")
    suspend fun clearNotificationsForPackage(pkgName: String)

    @Query("DELETE FROM notifications WHERE isBlocked = 1")
    suspend fun clearAllBlocked()
}
