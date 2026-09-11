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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObservationUnderstanding(entity: ObservationUnderstandingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSituation(entity: SituationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSituationMembers(entities: List<SituationMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOpenLoop(entity: OpenLoopEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSituationSnapshot(entity: SituationSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActionExecution(entity: ActionExecutionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(entity: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKnowledge(entity: KnowledgeEntity)

    @Query("SELECT * FROM entities WHERE id = :id LIMIT 1")
    suspend fun knowledgeById(id: String): KnowledgeEntity?

    @Query("SELECT * FROM observation_understanding ORDER BY priority DESC, analyzedAt DESC")
    fun observeObservationUnderstandings(): Flow<List<ObservationUnderstandingEntity>>

    @Query("SELECT * FROM situations WHERE state IN ('OPEN','SNOOZED','IN_PROGRESS') ORDER BY priority DESC, lastUpdatedAt DESC")
    fun observeOpenSituations(): Flow<List<SituationEntity>>

    @Query("SELECT * FROM situation_members WHERE situationId = :situationId")
    suspend fun situationMembers(situationId: String): List<SituationMemberEntity>

    @Query("SELECT * FROM open_loops ORDER BY priority DESC, updatedAt DESC")
    fun observeOpenLoops(): Flow<List<OpenLoopEntity>>

    @Query("SELECT * FROM open_loops ORDER BY priority DESC, updatedAt DESC")
    suspend fun openLoopsOnce(): List<OpenLoopEntity>

    @Query("SELECT * FROM open_loops WHERE id = :id LIMIT 1")
    suspend fun openLoopById(id: String): OpenLoopEntity?

    @Query("UPDATE open_loops SET state = :state, snoozedUntil = :snoozedUntil, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOpenLoopState(id: String, state: String, snoozedUntil: Long?, updatedAt: Long)

    @Query("UPDATE open_loops SET state = 'OPEN', snoozedUntil = NULL, updatedAt = :updatedAt WHERE state = 'SNOOZED' AND snoozedUntil IS NOT NULL AND snoozedUntil <= :now")
    suspend fun wakeSnoozedOpenLoops(now: Long, updatedAt: Long)

    @Query("DELETE FROM open_loops WHERE state NOT IN ('RESOLVED','DISMISSED')")
    suspend fun clearActiveOpenLoops()

    @Query("DELETE FROM open_loops WHERE id NOT IN (:ids) AND state NOT IN ('RESOLVED','DISMISSED')")
    suspend fun deleteActiveOpenLoopsNotIn(ids: List<String>)

    @Query("SELECT * FROM situation_snapshots ORDER BY priority DESC, lastUpdatedAt DESC")
    fun observeSituationSnapshots(): Flow<List<SituationSnapshotEntity>>

    @Query("DELETE FROM situation_snapshots")
    suspend fun clearSituationSnapshots()

    @Query("SELECT * FROM action_executions ORDER BY createdAt DESC LIMIT :limit")
    fun observeActionExecutions(limit: Int = 200): Flow<List<ActionExecutionEntity>>

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

    @Query("SELECT * FROM actions ORDER BY createdAt DESC")
    suspend fun actionsOnce(): List<ActionEntity>

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
            AND feedback.signal IN ('DEFERRED','SAVED')
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
    suspend fun recentObservationsOnce(limit: Int = 300): List<ObservationEntity>

    @Query("DELETE FROM interests")
    suspend fun clearInterests()

    @Query("DELETE FROM discoveries")
    suspend fun clearDiscoveries()

    @Query("DELETE FROM observation_understanding")
    suspend fun clearObservationUnderstandings()

    @Query("DELETE FROM situation_members")
    suspend fun clearSituationMembers()

    @Query("DELETE FROM situations")
    suspend fun clearSituations()

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
