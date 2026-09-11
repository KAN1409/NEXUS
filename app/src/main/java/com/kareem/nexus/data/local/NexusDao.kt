package com.kareem.nexus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NexusDao {
    @Query("SELECT * FROM observations WHERE id = :id")
    suspend fun observationById(id: String): ObservationEntity?

    @Query("SELECT * FROM observations ORDER BY createdAt DESC")
    fun observeAllObservations(): Flow<List<ObservationEntity>>

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

    @Query("SELECT * FROM actions ORDER BY createdAt DESC LIMIT :limit")
    fun observeAllActions(limit: Int = Int.MAX_VALUE): Flow<List<ActionEntity>>

    @Query("SELECT * FROM feedback ORDER BY createdAt DESC LIMIT :limit")
    fun observeFeedback(limit: Int = 200): Flow<List<FeedbackEntity>>

    @Query("DELETE FROM actions WHERE state = 'READY_FOR_APPROVAL'")
    suspend fun clearGeneratedActions()

    @Query("UPDATE actions SET state = 'REJECTED', updatedAt = :updatedAt WHERE id LIKE 'action_focus_%' AND state IN ('READY_FOR_APPROVAL','DRAFT')")
    suspend fun retireLegacyFocusActions(updatedAt: Long)

    @Query("SELECT * FROM actions WHERE id = :id LIMIT 1")
    suspend fun actionById(id: String): ActionEntity?

    @Query("UPDATE actions SET state = :state, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateActionState(id: String, state: String, updatedAt: Long)

    @Query("UPDATE actions SET state = 'DRAFT', updatedAt = :updatedAt WHERE id = :id")
    suspend fun deferAction(id: String, updatedAt: Long)

    @Query("""
        SELECT actions.* FROM actions
        WHERE actions.state = 'DRAFT'
        AND EXISTS (
            SELECT 1 FROM feedback
            WHERE feedback.targetId = actions.id
            AND feedback.signal = 'DEFERRED'
            GROUP BY feedback.targetId
            HAVING MAX(feedback.createdAt) <= :cutoff
        )
    """)
    suspend fun deferredActionsReadyToResurface(cutoff: Long): List<ActionEntity>

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

    @Query("DELETE FROM observations WHERE type = 'APP_USAGE' AND source NOT IN (:sources)")
    suspend fun deleteUsageOutsideSources(sources: List<String>)

    @Query("""DELETE FROM observations
        WHERE type = 'APP_USAGE'
        AND id NOT IN (
            SELECT o.id FROM observations o
            WHERE o.type = 'APP_USAGE'
            AND o.createdAt = (
                SELECT MAX(i.createdAt) FROM observations i
                WHERE i.type = 'APP_USAGE' AND i.source = o.source
            )
        )""")
    suspend fun removeDuplicateUsageSnapshots()
}
