package aura.notification.filter.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [NotificationEntity::class, AppRuleEntity::class], version = 5, exportSchema = false)
@TypeConverters(AuraDatabase.Converters::class)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appRuleDao(): AppRuleDao

    class Converters {
        @TypeConverter
        fun fromShieldLevel(level: ShieldLevel): String = level.name
        @TypeConverter
        fun toShieldLevel(value: String): ShieldLevel = try {
            ShieldLevel.valueOf(value)
        } catch (e: Exception) { ShieldLevel.SMART }

        @TypeConverter
        fun fromDetoxCategory(template: DetoxCategory): String = template.name
        @TypeConverter
        fun toDetoxCategory(value: String): DetoxCategory = try {
            DetoxCategory.valueOf(value)
        } catch (e: Exception) { DetoxCategory.SOCIAL }
    }
}
