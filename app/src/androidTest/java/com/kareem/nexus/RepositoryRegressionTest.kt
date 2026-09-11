package com.kareem.nexus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kareem.nexus.core.model.*
import com.kareem.nexus.data.local.*
import com.kareem.nexus.data.repository.OfflineNexusRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryRegressionTest {
    private lateinit var db: NexusDatabase
    private lateinit var repository: OfflineNexusRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), NexusDatabase::class.java).build()
        repository = OfflineNexusRepository(db.nexusDao(), db)
    }

    @After fun close() { db.close() }

    @Test fun concurrentApprovalIsRecordedExactlyOnce() = runBlocking {
        repository.captureObservation(ObservationType.NOTIFICATION, "Please send quotation today", "mail")
        repository.rebuildUnderstanding()
        val id = repository.actions().first().single().id
        coroutineScope { repeat(12) { launch(Dispatchers.IO) { repository.approveAction(id) } } }
        assertEquals(ActionState.APPROVED, repository.actions().first().single().state)
        assertEquals(1, repository.actionEvents().first().count { it.signal == FeedbackSignal.APPROVED })
        repository.startAction(id)
        repository.completeAction(id)
        repository.rebuildUnderstanding()
        assertEquals(ActionState.COMPLETED, repository.actions().first().single().state)
    }

    @Test fun dismissalAndDedupSurviveUnderstandingRefresh() = runBlocking {
        repository.captureObservation(ObservationType.MANUAL, "Please review invoice", "NEXUS")
        repository.rebuildUnderstanding()
        val before = repository.allObservations().first().single()
        val action = repository.actions().first().single()
        repository.rejectAction(action.id)
        repeat(3) {
            repository.captureObservation(ObservationType.MANUAL, "Please review invoice", "NEXUS")
            repository.rebuildUnderstanding()
        }
        assertEquals(before, repository.allObservations().first().single())
        assertEquals(ActionState.REJECTED, repository.actions().first().single().state)
    }

    @Test fun fullMemoryIncludesOlderThanOneHundredRecords() = runBlocking {
        repeat(125) { repository.captureObservation(ObservationType.MANUAL, "Memory $it", "NEXUS") }
        assertEquals(125, repository.allObservations().first().size)
        assertEquals(100, repository.observations().first().size)
    }

    @Test fun deferredActionOnlyResurfacesAfterWindow() = runBlocking {
        repository.captureObservation(ObservationType.MANUAL, "Please confirm meeting", "NEXUS")
        repository.rebuildUnderstanding()
        val id = repository.actions().first().single().id
        repository.deferAction(id)
        repository.rebuildUnderstanding()
        assertEquals(ActionState.DRAFT, repository.actions().first().single().state)
        db.openHelper.writableDatabase.execSQL("UPDATE feedback SET createdAt = 1 WHERE signal = 'DEFERRED'")
        repository.rebuildUnderstanding()
        assertEquals(ActionState.READY_FOR_APPROVAL, repository.actions().first().single().state)
    }

    @Test fun resolvedOpenLoopStaysResolvedAfterRebuild() = runBlocking {
        repository.captureObservation(ObservationType.MANUAL, "Ahmed — Please send the quotation today", "NEXUS")
        repository.rebuildUnderstanding()
        val loop = repository.openLoops().first().single()
        assertEquals(OpenLoopKind.NEEDS_REPLY, loop.kind)
        repository.resolveOpenLoop(loop.id)
        repository.rebuildUnderstanding()
        val after = repository.openLoops().first().single()
        assertEquals(OpenLoopState.RESOLVED, after.state)
    }

    @Test fun snoozedOpenLoopStaysHiddenUntilItsWakeTime() = runBlocking {
        repository.captureObservation(ObservationType.MANUAL, "Please confirm meeting tomorrow", "NEXUS")
        repository.rebuildUnderstanding()
        val loop = repository.openLoops().first().single()
        val future = System.currentTimeMillis() + 6 * 60 * 60 * 1000L
        repository.snoozeOpenLoop(loop.id, future)
        repository.rebuildUnderstanding()
        assertEquals(OpenLoopState.SNOOZED, repository.openLoops().first().single().state)
    }

    @Test fun databaseReopenPreservesExistingRecords() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "upgrade-preservation-test.db"
        context.deleteDatabase(name)
        var persistent = Room.databaseBuilder(context, NexusDatabase::class.java, name).build()
        try {
            OfflineNexusRepository(persistent.nexusDao(), persistent)
                .captureObservation(ObservationType.MANUAL, "Existing user memory", "NEXUS")
            persistent.close()
            persistent = Room.databaseBuilder(context, NexusDatabase::class.java, name).build()
            assertEquals("Existing user memory", persistent.nexusDao().recentObservationsOnce().single().rawText)
        } finally {
            persistent.close()
            context.deleteDatabase(name)
        }
    }
}
