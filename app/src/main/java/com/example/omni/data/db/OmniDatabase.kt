package com.example.omni.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        ProjectFileEntity::class,
        AgentDecisionEntity::class,
        SnapshotEntity::class,
        BuildHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OmniDatabase : RoomDatabase() {
    abstract fun omniDao(): OmniDao

    companion object {
        @Volatile
        private var INSTANCE: OmniDatabase? = null

        fun getInstance(context: Context): OmniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmniDatabase::class.java,
                    "omni_swarm_builder.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
