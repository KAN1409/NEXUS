package com.kareem.nexus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ObservationEntity::class,
        InterestEntity::class,
        KnowledgeEntity::class,
        MemoryEntity::class,
        DiscoveryEntity::class,
        ActionEntity::class,
        FeedbackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun nexusDao(): NexusDao
}
