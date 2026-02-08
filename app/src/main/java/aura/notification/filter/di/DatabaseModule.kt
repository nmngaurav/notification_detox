package aura.notification.filter.di

import android.content.Context
import androidx.room.Room
import aura.notification.filter.data.AppRuleDao
import aura.notification.filter.data.AuraDatabase
import aura.notification.filter.data.MIGRATION_5_6
import aura.notification.filter.data.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuraDatabase {
        return Room.databaseBuilder(
            context,
            AuraDatabase::class.java,
            "aura_database"
        )
        .addMigrations(MIGRATION_5_6)
        .fallbackToDestructiveMigration() // Keep fallback just in case, but migration covers 5->6
         .build()
    }

    @Provides
    fun provideNotificationDao(database: AuraDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun provideAppRuleDao(database: AuraDatabase): AppRuleDao {
        return database.appRuleDao()
    }
}
