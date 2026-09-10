package com.kareem.nexus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NexusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObservation(entity: ObservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInterest(entity: InterestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiscovery(entity: DiscoveryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAction(entity: ActionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFeedback(entity: FeedbackEntity)

    @Query("SELECT * FROM observations ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentObservations(limit: Int = 100): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM interests ORDER BY (affinity * confidence + momentum) DESC LIMIT :limit")
    fun observeTopInterests(limit: Int = 30): Flow<List<InterestEntity>>

    @Query("SELECT * FROM discoveries WHERE dismissed = 0 ORDER BY score DESC, createdAt DESC LIMIT :limit")
    fun observeFeed(limit: Int = 25): Flow<List<DiscoveryEntity>>

    @Query("SELECT * FROM actions WHERE state = 'READY_FOR_APPROVAL' ORDER BY createdAt DESC")
    fun observeReadyActions(): Flow<List<ActionEntity>>

    @Query("SELECT COUNT(*) FROM observations")
    fun observeObservationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM interests")
    fun observeInterestCount(): Flow<Int>

    @Query("SELECT * FROM observations ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentObservationsOnce(limit: Int = 200): List<ObservationEntity>

    @Query("DELETE FROM interests")
    suspend fun clearInterests()

    @Query("DELETE FROM discoveries")
    suspend fun clearDiscoveries()

    @Query("DELETE FROM observations WHERE type = 'APP_USAGE' AND source = :source AND id != :keepId")
    suspend fun deleteOtherUsageSnapshots(source: String, keepId: String)
}
