package com.aura.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [NotificationEntity::class, AppRuleEntity::class], version = 4, exportSchema = false)
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
        fun fromFilterTemplate(template: FilterTemplate): String = template.name
        @TypeConverter
        fun toFilterTemplate(value: String): FilterTemplate = try {
            FilterTemplate.valueOf(value)
        } catch (e: Exception) { FilterTemplate.NONE }
    }
}
