package com.kareem.nexus.domain.repository

import com.kareem.nexus.core.model.ActionEvent
import com.kareem.nexus.core.model.Discovery
import com.kareem.nexus.core.model.Interest
import com.kareem.nexus.core.model.Observation
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.core.model.PreparedAction
import kotlinx.coroutines.flow.Flow

interface NexusRepository {
    fun allObservations(): Flow<List<Observation>>
    fun observations(): Flow<List<Observation>>
    fun interests(): Flow<List<Interest>>
    fun discoveries(): Flow<List<Discovery>>
    fun readyActions(): Flow<List<PreparedAction>>
    fun actions(): Flow<List<PreparedAction>>
    fun actionEvents(): Flow<List<ActionEvent>>
    fun observationCount(): Flow<Int>
    fun interestCount(): Flow<Int>
    suspend fun captureObservation(type: ObservationType, rawText: String, source: String?, metadataJson: String = "{}")
    suspend fun pruneUsageSources(sources: List<String>)
    suspend fun rebuildUnderstanding()
    suspend fun approveAction(id: String)
    suspend fun deferAction(id: String)
    suspend fun rejectAction(id: String)
    suspend fun startAction(id: String)
    suspend fun completeAction(id: String)
    suspend fun failAction(id: String)
    suspend fun seedFirstRun()
}

