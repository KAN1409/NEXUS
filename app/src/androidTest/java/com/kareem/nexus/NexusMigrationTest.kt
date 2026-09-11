package com.kareem.nexus

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kareem.nexus.data.local.NexusDatabase
import com.kareem.nexus.data.local.NexusMigrations
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NexusMigrationTest {
    private lateinit var context: Context
    private val dbName = "nexus-migration-test.db"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration1To2_preservesLegacyData_andCreatesIntelligenceTables() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createVersion1Schema(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO observations(id,type,rawText,normalizedText,source,metadataJson,createdAt) VALUES(?,?,?,?,?,?,?)",
            arrayOf("legacy-1", "MANUAL", "Keep me", "keep me", "NEXUS", "{}", 1234L),
        )
        helper.close()

        val db = Room.databaseBuilder(context, NexusDatabase::class.java, dbName)
            .addMigrations(NexusMigrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val sql = db.openHelper.writableDatabase
        sql.query("SELECT rawText FROM observations WHERE id='legacy-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Keep me", cursor.getString(0))
        }
        assertTrue(tableExists(sql, "observation_understanding"))
        assertTrue(tableExists(sql, "situations"))
        assertTrue(tableExists(sql, "situation_members"))
        db.close()
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS observations (id TEXT NOT NULL, type TEXT NOT NULL, rawText TEXT NOT NULL, normalizedText TEXT NOT NULL, source TEXT, metadataJson TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_observations_createdAt ON observations(createdAt)")

        db.execSQL("CREATE TABLE IF NOT EXISTS interests (id TEXT NOT NULL, label TEXT NOT NULL, affinity REAL NOT NULL, momentum REAL NOT NULL, confidence REAL NOT NULL, saturation REAL NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_interests_updatedAt ON interests(updatedAt)")

        db.execSQL("CREATE TABLE IF NOT EXISTS entities (id TEXT NOT NULL, type TEXT NOT NULL, canonicalName TEXT NOT NULL, aliasesJson TEXT NOT NULL, firstSeenAt INTEGER NOT NULL, lastSeenAt INTEGER NOT NULL, PRIMARY KEY(id))")

        db.execSQL("CREATE TABLE IF NOT EXISTS memories (id TEXT NOT NULL, observationId TEXT NOT NULL, summary TEXT NOT NULL, searchableText TEXT NOT NULL, importance REAL NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_createdAt ON memories(createdAt)")

        db.execSQL("CREATE TABLE IF NOT EXISTS discoveries (id TEXT NOT NULL, type TEXT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, whyThis TEXT NOT NULL, sourceUrl TEXT, score REAL NOT NULL, dismissed INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discoveries_score ON discoveries(score)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_discoveries_createdAt ON discoveries(createdAt)")

        db.execSQL("CREATE TABLE IF NOT EXISTS actions (id TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, state TEXT NOT NULL, payloadJson TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_actions_state ON actions(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_actions_createdAt ON actions(createdAt)")

        db.execSQL("CREATE TABLE IF NOT EXISTS feedback (id TEXT NOT NULL, targetId TEXT NOT NULL, signal TEXT NOT NULL, value REAL NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_targetId ON feedback(targetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_createdAt ON feedback(createdAt)")
    }
}
