package com.kareem.nexus.domain.repository

import com.kareem.nexus.core.model.*
import kotlinx.coroutines.flow.Flow

interface NexusRepository {
    fun allObservations(): Flow<List<Observation>>
    fun observations(): Flow<List<Observation>>
    fun interests(): Flow<List<Interest>>
    fun discoveries(): Flow<List<Discovery>>
    fun readyActions(): Flow<List<PreparedAction>>
    fun actions(): Flow<List<PreparedAction>>
    fun actionEvents(): Flow<List<ActionEvent>>
    fun openLoops(): Flow<List<OpenLoop>>
    fun situationBriefs(): Flow<List<SituationBrief>>
    fun actionExecutions(): Flow<List<ActionExecution>>
    fun observationCount(): Flow<Int>
    fun interestCount(): Flow<Int>

    suspend fun captureObservation(type: ObservationType, rawText: String, source: String?, metadataJson: String = "{}")
    suspend fun pruneUsageSources(sources: List<String>)
    suspend fun rebuildUnderstanding()

    suspend fun approveAction(id: String)
    suspend fun deferAction(id: String)
    suspend fun rejectAction(id: String)
    suspend fun resolveAction(id: String)
    suspend fun startAction(id: String)
    suspend fun completeAction(id: String)
    suspend fun failAction(id: String)

    suspend fun snoozeOpenLoop(id: String, until: Long)
    suspend fun resolveOpenLoop(id: String)
    suspend fun dismissOpenLoop(id: String)
    suspend fun recordActionExecution(
        openLoopId: String?,
        actionKind: NexusActionKind,
        label: String,
        payload: String?,
        state: ExecutionState,
        message: String? = null,
    )

    suspend fun seedFirstRun()
}
