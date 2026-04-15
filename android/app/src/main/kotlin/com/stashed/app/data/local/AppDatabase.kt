package com.stashed.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Root Room database.
 *
 * Two companion virtual tables are created by [DatabaseCallback] after the
 * database is first opened:
 *
 *   1. memories_vec  — sqlite-vec virtual table for cosine similarity search
 *   2. memories_fts  — SQLite FTS5 table for keyword search (fallback)
 *
 * Both virtual tables are linked to [MemoryEntity] by the memory's [id].
 * Every save/update/delete touches all three tables in a single transaction
 * (handled by [MemoryRepository]).
 *
 * Migration strategy: destructive fallback enabled for now (dev builds only).
 * Add proper migrations before beta release.
 */
@Database(
    entities = [MemoryEntity::class, LocationHistoryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun locationHistoryDao(): LocationHistoryDao

    companion object {
        const val DATABASE_NAME = "stashed.db"

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE memories ADD COLUMN media_paths TEXT DEFAULT NULL")
            }
        }

        /**
         * SQL executed once after the database is created.
         * Creates the sqlite-vec and FTS5 virtual tables.
         *
         * sqlite-vec must be loaded via System.loadLibrary("vec0") before this runs.
         */
        val CREATE_VIRTUAL_TABLES = listOf(
            // Vector search table — indexed by memory id
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS memories_vec USING vec0(
                id TEXT PRIMARY KEY,
                embedding FLOAT[384]
            )
            """.trimIndent(),

            // FTS5 full-text search table — standalone (not content-sync'd,
            // because the memories table uses a TEXT primary key, not integer rowid).
            // The repository manually keeps this in sync on insert/update/delete.
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS memories_fts USING fts5(
                item,
                location,
                raw_text
            )
            """.trimIndent(),
        )
    }
}
