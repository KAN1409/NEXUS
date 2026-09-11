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
}
