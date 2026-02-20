package aura.notification.filter.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationRepository {
    fun getRulesForProfile(profileId: String): Flow<List<AppRuleEntity>>


    suspend fun getRuleForPackage(packageName: String): AppRuleEntity?
    suspend fun getRule(packageName: String, profileId: String): AppRuleEntity?
    suspend fun updateRule(rule: AppRuleEntity)
    suspend fun deleteRule(packageName: String, profileId: String)
    
    suspend fun logNotification(notification: NotificationEntity)
    fun getBlockedNotifications(): Flow<List<NotificationEntity>>
    fun getBlockedNotificationsForPackage(packageName: String): Flow<List<NotificationEntity>>
    suspend fun clearNotificationsForPackage(packageName: String)
    suspend fun deleteNotification(id: Int)
    suspend fun clearAllBlocked()
    suspend fun pruneOldNotifications(threshold: Long)
    
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    fun getAllRules(): Flow<List<AppRuleEntity>>
    suspend fun getAppsWithNotifications(): List<String>
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val appRuleDao: AppRuleDao,
    private val focusModeManager: FocusModeManager
) : NotificationRepository {

    override fun getRulesForProfile(profileId: String): Flow<List<AppRuleEntity>> = 
        appRuleDao.getRulesForProfile(profileId)
    


    override suspend fun getRuleForPackage(packageName: String): AppRuleEntity? {
        // Get current profile
        val currentProfile = focusModeManager.getMode().name
        return appRuleDao.getRule(packageName, currentProfile)
    }

    override suspend fun getRule(packageName: String, profileId: String): AppRuleEntity? {
        return appRuleDao.getRule(packageName, profileId)
    }

    override suspend fun updateRule(rule: AppRuleEntity) {
        appRuleDao.insertOrUpdate(rule)
    }

    override suspend fun deleteRule(packageName: String, profileId: String) {
        appRuleDao.deleteRule(packageName, profileId)
    }

    override suspend fun logNotification(notification: NotificationEntity) {
        notificationDao.insert(notification)
    }

    override fun getBlockedNotifications(): Flow<List<NotificationEntity>> = notificationDao.getBlockedNotifications()

    override fun getBlockedNotificationsForPackage(packageName: String): Flow<List<NotificationEntity>> =
        notificationDao.getBlockedNotificationsForPackage(packageName)

    override suspend fun clearNotificationsForPackage(packageName: String) {
        notificationDao.clearNotificationsForPackage(packageName)
    }

    override suspend fun deleteNotification(id: Int) {
        notificationDao.deleteNotification(id)
    }

    override suspend fun clearAllBlocked() {
        notificationDao.clearAllBlocked()
    }

    override suspend fun pruneOldNotifications(threshold: Long) {
        notificationDao.deleteOldNotifications(threshold)
    }

    override fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()

    override fun getAllRules(): Flow<List<AppRuleEntity>> = appRuleDao.getAllRulesRaw()
    
    override suspend fun getAppsWithNotifications(): List<String> = notificationDao.getDistinctPackageNames()
}
