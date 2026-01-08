package com.aura.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: AppRuleEntity)

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName AND profileId = :profileId")
    suspend fun getRule(packageName: String, profileId: String): AppRuleEntity?
    
    // Using Flow for UI updates (Profile Specific)
    @Query("SELECT * FROM app_rules WHERE profileId = :profileId ORDER BY packageName ASC")
    fun getRulesForProfile(profileId: String): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules")
    fun getAllRulesRaw(): Flow<List<AppRuleEntity>>

    @Query("DELETE FROM app_rules WHERE packageName = :packageName AND profileId = :profileId")
    suspend fun deleteRule(packageName: String, profileId: String)
}
