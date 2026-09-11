package com.kareem.nexus.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object NexusMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS observation_understanding (
                    observationId TEXT NOT NULL PRIMARY KEY,
                    kind TEXT NOT NULL,
                    title TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    factsJson TEXT NOT NULL,
                    actionsJson TEXT NOT NULL,
                    priority REAL NOT NULL,
                    confidence REAL NOT NULL,
                    isNoise INTEGER NOT NULL,
                    analyzedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_observation_understanding_kind ON observation_understanding(kind)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_observation_understanding_priority ON observation_understanding(priority)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_observation_understanding_analyzedAt ON observation_understanding(analyzedAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS situations (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    state TEXT NOT NULL,
                    factsJson TEXT NOT NULL,
                    actionsJson TEXT NOT NULL,
                    priority REAL NOT NULL,
                    confidence REAL NOT NULL,
                    createdAt INTEGER NOT NULL,
                    lastUpdatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_situations_state ON situations(state)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_situations_priority ON situations(priority)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_situations_lastUpdatedAt ON situations(lastUpdatedAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS situation_members (
                    situationId TEXT NOT NULL,
                    observationId TEXT NOT NULL,
                    PRIMARY KEY(situationId, observationId)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_situation_members_situationId ON situation_members(situationId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_situation_members_observationId ON situation_members(observationId)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS open_loops (
                    id TEXT NOT NULL PRIMARY KEY,
                    observationId TEXT NOT NULL,
                    situationId TEXT,
                    kind TEXT NOT NULL,
                    title TEXT NOT NULL,
                    detail TEXT NOT NULL,
                    party TEXT,
                    source TEXT,
                    state TEXT NOT NULL,
                    priority REAL NOT NULL,
                    dueAt INTEGER,
                    snoozedUntil INTEGER,
                    actionsJson TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_open_loops_state ON open_loops(state)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_open_loops_kind ON open_loops(kind)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_open_loops_priority ON open_loops(priority)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_open_loops_updatedAt ON open_loops(updatedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_open_loops_situationId ON open_loops(situationId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS situation_snapshots (
                    situationId TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    currentState TEXT NOT NULL,
                    whatChanged TEXT NOT NULL,
                    nextStep TEXT,
                    openLoopCount INTEGER NOT NULL,
                    evidenceCount INTEGER NOT NULL,
                    priority REAL NOT NULL,
                    lastUpdatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_situation_snapshots_priority ON situation_snapshots(priority)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_situation_snapshots_lastUpdatedAt ON situation_snapshots(lastUpdatedAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS action_executions (
                    id TEXT NOT NULL PRIMARY KEY,
                    openLoopId TEXT,
                    actionKind TEXT NOT NULL,
                    label TEXT NOT NULL,
                    payload TEXT,
                    state TEXT NOT NULL,
                    message TEXT,
                    createdAt INTEGER NOT NULL,
                    completedAt INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_action_executions_openLoopId ON action_executions(openLoopId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_action_executions_state ON action_executions(state)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_action_executions_createdAt ON action_executions(createdAt)")
        }
    }
}
