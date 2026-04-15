package com.stashed.app.intelligence

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * On-device text embedding using all-MiniLM-L6-v2 (INT8 quantized, ~23 MB).
 * Runs via ONNX Runtime Mobile (~12 MB).
 *
 * The OrtSession is a singleton — loaded once on first use and kept in memory.
 * Loading takes ~500ms; subsequent calls take ~100ms on a mid-range phone.
 *
 * Output: FloatArray of size 384 (unit-length via L2 normalization).
 *
 * Critical processing steps after inference:
 *   1. Mean pooling across all non-padding token embeddings
 *   2. L2 normalization of the pooled vector
 * Without these, cosine similarity scores are meaningless.
 */
class MiniLMEmbedder(
    private val context: Context,
    private val tokenizer: WordPieceTokenizer,
) {

    companion object {
        private const val MODEL_FILE = "minilm-l6-v2-int8.onnx"
        private const val EMBEDDING_DIM = 384
    }

    private var session: OrtSession? = null
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    /**
     * Load the ONNX model from assets. Called lazily on first embed() call.
     * Safe to call multiple times — only loads once.
     */
    private fun ensureSessionLoaded() {
        if (session != null) return
        val modelBytes = context.assets.open(MODEL_FILE).readBytes()
        session = env.createSession(modelBytes)
    }

    /**
     * Convert a plain text string into a 384-dimensional semantic embedding vector.
     * Runs on Dispatchers.Default (CPU-intensive, not IO).
     *
     * @return FloatArray of size 384, L2-normalized to unit length.
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        ensureSessionLoaded()

        val tokens = tokenizer.tokenize(text)

        // Wrap input arrays as 2D tensors: shape [1, MAX_LENGTH]
        val inputIds = OnnxTensor.createTensor(
            env,
            arrayOf(tokens.inputIds),
        )
        val attentionMask = OnnxTensor.createTensor(
            env,
            arrayOf(tokens.attentionMask),
        )
        val tokenTypeIds = OnnxTensor.createTensor(
            env,
            arrayOf(tokens.tokenTypeIds),
        )

        val results = session!!.run(
            mapOf(
                "input_ids" to inputIds,
                "attention_mask" to attentionMask,
                "token_type_ids" to tokenTypeIds,
            )
        )

        // Output: last_hidden_state, shape [1, 128, 384]
        // Extract [0] to get shape [128, 384]
        @Suppress("UNCHECKED_CAST")
        val tokenEmbeddings = (results[0].value as Array<Array<FloatArray>>)[0]

        // Mean pool over all non-padding tokens, then L2-normalize
        val pooled = meanPool(tokenEmbeddings, tokens.attentionMask)
        l2Normalize(pooled)

        // Cleanup ONNX tensors
        inputIds.close()
        attentionMask.close()
        tokenTypeIds.close()
        results.close()

        pooled
    }

    /**
     * Average all non-padding token embeddings into a single vector.
     * Padding tokens (attentionMask == 0) are excluded from the average.
     */
    private fun meanPool(tokenEmbeddings: Array<FloatArray>, mask: LongArray): FloatArray {
        val pooled = FloatArray(EMBEDDING_DIM)
        var count = 0

        for (i in tokenEmbeddings.indices) {
            if (mask[i] == 1L) {
                for (d in 0 until EMBEDDING_DIM) {
                    pooled[d] += tokenEmbeddings[i][d]
                }
                count++
            }
        }

        if (count > 0) {
            for (d in 0 until EMBEDDING_DIM) {
                pooled[d] /= count
            }
        }

        return pooled
    }

    /**
     * Scale a vector to unit length (L2 norm = 1.0).
     * Required for cosine similarity to produce values in [-1, 1].
     * Modifies the array in-place.
     */
    private fun l2Normalize(vector: FloatArray) {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
    }

    fun close() {
        session?.close()
        session = null
    }
}
