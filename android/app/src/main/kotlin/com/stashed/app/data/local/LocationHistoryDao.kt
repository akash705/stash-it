package com.stashed.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationHistoryDao {

    @Insert
    suspend fun insert(entry: LocationHistoryEntity)

    /** Last 5 locations for a given memory, newest first. */
    @Query("""
        SELECT * FROM location_history
        WHERE memory_id = :memoryId
        ORDER BY changed_at DESC
        LIMIT 5
    """)
    fun getHistory(memoryId: String): Flow<List<LocationHistoryEntity>>

    @Query("DELETE FROM location_history WHERE memory_id = :memoryId")
    suspend fun deleteForMemory(memoryId: String)
}
