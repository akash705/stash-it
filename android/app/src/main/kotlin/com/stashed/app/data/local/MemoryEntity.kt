package com.stashed.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Persisted memory entry.
 *
 * The [embedding] field stores the 384-dim float32 vector as a raw BLOB
 * (384 * 4 bytes = 1,536 bytes per row). The companion sqlite-vec virtual
 * table (memories_vec) holds the same vector for fast cosine distance search.
 *
 * Both tables are written in a single transaction via [MemoryDao.insertMemory].
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "raw_text")
    val rawText: String,

    @ColumnInfo(name = "item")
    val item: String,

    @ColumnInfo(name = "location")
    val location: String,

    @ColumnInfo(name = "emoji")
    val emoji: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /** Raw bytes of a float32[384] vector. Null if embedding unavailable (FTS5-only mode). */
    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray? = null,

    /** Pipe-delimited absolute file paths to attached photos/videos, e.g. "/data/.../media/abc.jpg|/data/.../media/xyz.mp4". Null if no attachments. */
    @ColumnInfo(name = "media_paths")
    val mediaPaths: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
