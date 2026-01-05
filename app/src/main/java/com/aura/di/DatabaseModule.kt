package com.aura.di

import android.content.Context
import androidx.room.Room
import com.aura.data.AppRuleDao
import com.aura.data.AuraDatabase
import com.aura.data.NotificationDao
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
        ).fallbackToDestructiveMigration() // For MVP dev speed
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
