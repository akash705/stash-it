package com.stashed.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Historical location record for a memory.
 * Created automatically when a memory's location is updated via edit.
 * Keeps the last N locations so users can trace where an item has been.
 */
@Entity(
    tableName = "location_history",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("memory_id")],
)
data class LocationHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "memory_id")
    val memoryId: String,

    @ColumnInfo(name = "previous_location")
    val previousLocation: String,

    @ColumnInfo(name = "changed_at")
    val changedAt: Long = System.currentTimeMillis(),
)
