package com.stashed.app.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stashed.app.data.repository.MemoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that runs the full save pipeline from the widget.
 * Triggered by WorkManager so the widget doesn't need to start the full app.
 *
 * Input: "raw_text" key in inputData.
 */
@HiltWorker
class SaveMemoryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: MemoryRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val rawText = inputData.getString("raw_text") ?: return Result.failure()
        return try {
            repository.saveMemory(rawText)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "save_memory_widget"
    }
}
