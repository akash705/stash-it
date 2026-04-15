package com.stashed.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    // ── Insert ────────────────────────────────────────────────────────────────

    @Insert
    suspend fun insert(memory: MemoryEntity)

    // ── Queries ───────────────────────────────────────────────────────────────

    /** All memories ordered newest first — drives the memory list screen. */
    @Query("SELECT * FROM memories ORDER BY created_at DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MemoryEntity?

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getCount(): Int

    /** All memories whose embedding is null — need to be (re-)embedded. */
    @Query("SELECT * FROM memories WHERE embedding IS NULL")
    suspend fun getUnembedded(): List<MemoryEntity>

    // ── FTS5 keyword search (used before MiniLM model is available) ───────────
    // Uses @RawQuery because memories_fts is a virtual table created at runtime,
    // not through Room annotations, so Room can't verify it at compile time.

    @RawQuery
    suspend fun keywordSearch(query: SupportSQLiteQuery): List<MemoryEntity>

    // ── Vector search (delegates to sqlite-vec via RawQuery) ─────────────────

    /**
     * Used by [MemoryRepository] to run the vec_distance_cosine query.
     * Called with a manually constructed SupportSQLiteQuery that binds the
     * query embedding BLOB as a parameter.
     */
    @RawQuery
    suspend fun vectorSearch(query: SupportSQLiteQuery): List<MemoryEntity>

    // ── Update ────────────────────────────────────────────────────────────────

    @Update
    suspend fun update(memory: MemoryEntity)

    /** Patch just the embedding (e.g. during background batch re-embedding). */
    @Query("UPDATE memories SET embedding = :embedding, updated_at = :now WHERE id = :id")
    suspend fun updateEmbedding(id: String, embedding: ByteArray, now: Long = System.currentTimeMillis())

    // ── Delete ────────────────────────────────────────────────────────────────

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: String)
}
