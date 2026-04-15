package com.stashed.app.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.stashed.app.data.local.AppDatabase
import com.stashed.app.data.local.LocationHistoryEntity
import com.stashed.app.data.local.MemoryEntity
import com.stashed.app.data.local.decodePaths
import com.stashed.app.intelligence.EmojiMapper
import com.stashed.app.intelligence.MiniLMEmbedder
import com.stashed.app.intelligence.NLParser
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for all memory operations.
 *
 * Orchestrates the full save and search pipelines:
 *   Save:   NLParser → EmojiMapper → MiniLMEmbedder → Room + sqlite-vec
 *   Search: MiniLMEmbedder → sqlite-vec cosine search (or FTS5 fallback)
 *
 * All DB operations that touch multiple tables use a single transaction
 * via [AppDatabase.runInTransaction] to guarantee consistency.
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val db: AppDatabase,
    private val embedder: MiniLMEmbedder,
) {
    private val dao = db.memoryDao()
    private val historyDao = db.locationHistoryDao()

    // ── Reactive data ─────────────────────────────────────────────────────────

    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemories()

    suspend fun getCount(): Int = dao.getCount()

    suspend fun getById(id: String): MemoryEntity? = dao.getById(id)

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Full save pipeline: parse → emoji → embed → store.
     * Inserts into memories, memories_vec, and memories_fts in one transaction.
     *
     * @param rawInput    Plain English text from the user (typed or spoken).
     * @param mediaPaths  Optional list of absolute file paths to attached photos/videos.
     * @return            The saved [MemoryEntity].
     */
    suspend fun saveMemory(rawInput: String, mediaPaths: List<String> = emptyList()): MemoryEntity {
        val parsed = NLParser.parse(rawInput)
        val emoji = EmojiMapper.getEmoji(parsed.item)
        val embedding = embedder.embed(rawInput)
        val embeddingBlob = floatArrayToBlob(embedding)

        val entity = MemoryEntity(
            rawText = parsed.rawText,
            item = parsed.item,
            location = parsed.location,
            emoji = emoji,
            embedding = embeddingBlob,
            mediaPaths = mediaPaths.joinToString("|").ifEmpty { null },
        )

        db.withTransaction {
            dao.insert(entity)
            insertIntoVec(entity.id, embeddingBlob)
            insertIntoFts(entity)
        }

        return entity
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Semantic vector search using MiniLM + sqlite-vec.
     * Returns up to 5 results ranked by cosine similarity (closest first).
     *
     * Falls back to FTS5 keyword search if the embedder is unavailable.
     */
    /**
     * Search pipeline with three tiers:
     *   1. Semantic vector search (sqlite-vec) — best quality, needs native lib
     *   2. In-memory cosine similarity — same quality, loads all embeddings into RAM
     *   3. LIKE search on the memories table — keyword fallback, always works
     */
    suspend fun search(query: String, limit: Int = 5): List<SearchResult> {
        return if (isVecTableAvailable()) {
            try {
                vectorSearch(query, limit)
            } catch (_: Exception) {
                embeddingSearch(query, limit)
            }
        } else {
            embeddingSearch(query, limit)
        }
    }

    private fun isVecTableAvailable(): Boolean = try {
        db.openHelper.readableDatabase
            .query("SELECT name FROM sqlite_master WHERE type='table' AND name='memories_vec'")
            .use { it.moveToFirst() }
    } catch (_: Exception) {
        false
    }

    private suspend fun vectorSearch(query: String, limit: Int): List<SearchResult> {
        val queryEmbedding = embedder.embed(query)
        val blob = floatArrayToBlob(queryEmbedding)

        // sqlite-vec cosine distance query: lower score = closer match
        val sql = """
            SELECT m.*, v.distance
            FROM memories m
            JOIN (
                SELECT id, distance
                FROM memories_vec
                WHERE embedding MATCH ?
                  AND k = $limit
                ORDER BY distance
            ) v ON m.id = v.id
            ORDER BY v.distance ASC
        """.trimIndent()

        val rawQuery = SimpleSQLiteQuery(sql, arrayOf(blob))
        val entities = dao.vectorSearch(rawQuery)

        // Re-compute actual cosine similarity against each returned entity
        return entities.mapNotNull { entity ->
            val storedEmbedding = blobToFloatArray(entity.embedding ?: return@mapNotNull null)
            val sim = cosineSimilarity(queryEmbedding, storedEmbedding)
            SearchResult(entity = entity, similarity = sim)
        }.filter { it.similarity != null && it.similarity > 0.2f }
    }

    /**
     * In-memory cosine similarity search. Loads all embedded memories and ranks
     * by similarity to the query embedding. Used when sqlite-vec is unavailable.
     */
    private suspend fun embeddingSearch(query: String, limit: Int): List<SearchResult> {
        val queryEmbedding = embedder.embed(query)
        val allMemories = dao.getAllWithEmbeddings()

        if (allMemories.isEmpty()) {
            return likeSearch(query, limit)
        }

        return allMemories
            .mapNotNull { entity ->
                val storedEmbedding = blobToFloatArray(entity.embedding ?: return@mapNotNull null)
                val sim = cosineSimilarity(queryEmbedding, storedEmbedding)
                SearchResult(entity = entity, similarity = sim)
            }
            .filter { it.similarity != null && it.similarity > 0.2f }
            .sortedByDescending { it.similarity }
            .take(limit)
    }

    private suspend fun likeSearch(query: String, limit: Int): List<SearchResult> {
        val pattern = "%${query.trim()}%"
        val sql = """
            SELECT * FROM memories
            WHERE item LIKE ? OR location LIKE ? OR raw_text LIKE ?
            ORDER BY created_at DESC
            LIMIT $limit
        """.trimIndent()
        val rawQuery = SimpleSQLiteQuery(sql, arrayOf(pattern, pattern, pattern))
        val entities = dao.vectorSearch(rawQuery)
        return entities.map { SearchResult(entity = it, similarity = null) }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Update an existing memory's item and/or location.
     * Re-embeds the new text and updates both the memories and memories_vec tables.
     * Archives the old location in location_history if it changed.
     */
    suspend fun updateMemory(id: String, newItem: String, newLocation: String) {
        val existing = dao.getById(id) ?: return
        val newRaw = if (newLocation.isNotBlank()) "$newItem in $newLocation" else newItem
        val emoji = EmojiMapper.getEmoji(newItem)
        val embedding = embedder.embed(newRaw)
        val blob = floatArrayToBlob(embedding)

        // Archive old location if it changed
        val locationChanged = existing.location.isNotBlank() &&
            existing.location != newLocation

        val updated = existing.copy(
            item = newItem,
            location = newLocation,
            rawText = newRaw,
            emoji = emoji,
            embedding = blob,
            updatedAt = System.currentTimeMillis(),
        )

        db.withTransaction {
            if (locationChanged) {
                historyDao.insert(
                    LocationHistoryEntity(
                        memoryId = id,
                        previousLocation = existing.location,
                    )
                )
            }
            dao.update(updated)
            updateVec(id, blob)
            updateFts(updated)
        }
    }

    /** Get location history for a memory. */
    fun getLocationHistory(memoryId: String) = historyDao.getHistory(memoryId)

    // ── Delete ────────────────────────────────────────────────────────────────

    suspend fun deleteMemory(id: String) {
        val entity = dao.getById(id)
        db.withTransaction {
            dao.deleteById(id)
            deleteFromVec(id)
            deleteFromFts(id)
        }
        // Delete attached media files outside the DB transaction
        entity?.let { decodePaths(it.mediaPaths).forEach { path -> File(path).delete() } }
    }

    // ── Background batch embedding ────────────────────────────────────────────

    /**
     * Re-embed all memories that were saved without an embedding
     * (i.e., during FTS5-only mode before the MiniLM model was available).
     * Called after the model downloads successfully.
     */
    suspend fun batchEmbedUnembedded() {
        val unembedded = dao.getUnembedded()
        for (memory in unembedded) {
            val embedding = embedder.embed(memory.rawText)
            val blob = floatArrayToBlob(embedding)
            dao.updateEmbedding(memory.id, blob)
            insertIntoVec(memory.id, blob)
        }
    }

    // ── Private helpers — virtual table operations ────────────────────────────
    // These use raw SQL because Room doesn't manage virtual tables.

    private fun insertIntoVec(id: String, blob: ByteArray) {
        try {
            db.openHelper.writableDatabase.execSQL(
                "INSERT OR REPLACE INTO memories_vec(id, embedding) VALUES (?, ?)",
                arrayOf(id, blob),
            )
        } catch (_: Exception) {
            // sqlite-vec not available — vector search disabled, FTS5 fallback active
        }
    }

    private fun updateVec(id: String, blob: ByteArray) {
        try {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE memories_vec SET embedding = ? WHERE id = ?",
                arrayOf(blob, id),
            )
        } catch (_: Exception) {
            // sqlite-vec not available
        }
    }

    private fun deleteFromVec(id: String) {
        try {
            db.openHelper.writableDatabase.execSQL(
                "DELETE FROM memories_vec WHERE id = ?",
                arrayOf(id),
            )
        } catch (_: Exception) {
            // sqlite-vec not available
        }
    }

    private fun insertIntoFts(entity: MemoryEntity) {
        try {
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO memories_fts(item, location, raw_text) VALUES (?, ?, ?)",
                arrayOf(entity.item, entity.location, entity.rawText),
            )
        } catch (_: Exception) {
            // FTS5 table not yet created — keyword search unavailable
        }
    }

    private fun updateFts(entity: MemoryEntity) {
        try {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE memories_fts SET item = ?, location = ?, raw_text = ? WHERE rowid = (SELECT rowid FROM memories WHERE id = ?)",
                arrayOf(entity.item, entity.location, entity.rawText, entity.id),
            )
        } catch (_: Exception) {
            // FTS5 table not available
        }
    }

    private fun deleteFromFts(id: String) {
        try {
            db.openHelper.writableDatabase.execSQL(
                "DELETE FROM memories_fts WHERE rowid = (SELECT rowid FROM memories WHERE id = ?)",
                arrayOf(id),
            )
        } catch (_: Exception) {
            // FTS5 table not available
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun floatArrayToBlob(floats: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        floats.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun blobToFloatArray(blob: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(blob.size / 4) { buffer.getFloat() }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }
}

data class SearchResult(
    val entity: MemoryEntity,
    /** Cosine similarity in [0, 1]. Null for keyword search results. */
    val similarity: Float?,
)
