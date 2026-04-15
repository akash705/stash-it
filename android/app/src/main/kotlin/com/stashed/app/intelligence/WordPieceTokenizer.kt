package com.stashed.app.intelligence

import android.content.Context
import org.json.JSONObject

/**
 * WordPiece tokenizer matching the all-MiniLM-L6-v2 vocabulary.
 *
 * Reads tokenizer.json from assets (bundled at ~800 KB).
 * Converts a plain text string into token IDs + attention mask
 * suitable for direct input into the ONNX MiniLM model.
 *
 * Output:
 *   inputIds      — LongArray of size MAX_LENGTH (128), padded with 0
 *   attentionMask — LongArray of size MAX_LENGTH, 1 = real token, 0 = padding
 *   tokenTypeIds  — LongArray of size MAX_LENGTH, all 0 (single-sentence input)
 */
class WordPieceTokenizer(context: Context) {

    data class TokenizedInput(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray,
    )

    companion object {
        private const val MAX_LENGTH = 128
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"
        private const val UNK_TOKEN = "[UNK]"
        private const val PAD_ID = 0L
    }

    // vocab: token string → token ID
    private val vocab: Map<String, Long>
    private val clsId: Long
    private val sepId: Long
    private val unkId: Long

    init {
        val json = context.assets.open("tokenizer.json").bufferedReader().readText()
        val root = JSONObject(json)
        val vocabObj = root.getJSONObject("model").getJSONObject("vocab")
        val mutableVocab = HashMap<String, Long>(vocabObj.length())
        vocabObj.keys().forEach { key ->
            mutableVocab[key] = vocabObj.getLong(key)
        }
        vocab = mutableVocab
        clsId = vocab[CLS_TOKEN] ?: error("Missing [CLS] in vocabulary")
        sepId = vocab[SEP_TOKEN] ?: error("Missing [SEP] in vocabulary")
        unkId = vocab[UNK_TOKEN] ?: error("Missing [UNK] in vocabulary")
    }

    fun tokenize(text: String): TokenizedInput {
        val wordTokenIds = wordPieceEncode(text.lowercase().trim())

        // Reserve 2 slots for [CLS] and [SEP]
        val maxContent = MAX_LENGTH - 2
        val truncated = wordTokenIds.take(maxContent)

        val inputIds = LongArray(MAX_LENGTH) { PAD_ID }
        val attentionMask = LongArray(MAX_LENGTH) { 0L }
        val tokenTypeIds = LongArray(MAX_LENGTH) { 0L }

        // [CLS]
        inputIds[0] = clsId
        attentionMask[0] = 1L

        // Content tokens
        truncated.forEachIndexed { index, id ->
            inputIds[index + 1] = id
            attentionMask[index + 1] = 1L
        }

        // [SEP]
        val sepIndex = truncated.size + 1
        inputIds[sepIndex] = sepId
        attentionMask[sepIndex] = 1L

        return TokenizedInput(inputIds, attentionMask, tokenTypeIds)
    }

    private fun wordPieceEncode(text: String): List<Long> {
        val result = mutableListOf<Long>()

        // Split on whitespace and basic punctuation
        val words = text.split(Regex("[\\s\\p{Punct}]+")).filter { it.isNotBlank() }

        for (word in words) {
            val pieces = encodeWord(word)
            result.addAll(pieces)
        }

        return result
    }

    /**
     * Encode a single word using WordPiece subword splitting.
     * Tries to find the longest prefix in vocabulary, then continues
     * with "##" prefix for remaining characters.
     */
    private fun encodeWord(word: String): List<Long> {
        if (vocab.containsKey(word)) return listOf(vocab[word]!!)

        val subTokenIds = mutableListOf<Long>()
        var start = 0
        var isBad = false

        while (start < word.length) {
            var end = word.length
            var curSubStr: String? = null
            var curId: Long? = null

            while (start < end) {
                val subStr = if (start == 0) word.substring(start, end)
                             else "##" + word.substring(start, end)
                val id = vocab[subStr]
                if (id != null) {
                    curSubStr = subStr
                    curId = id
                    break
                }
                end--
            }

            if (curSubStr == null || curId == null) {
                isBad = true
                break
            }

            subTokenIds.add(curId)
            start = end
        }

        return if (isBad) listOf(unkId) else subTokenIds
    }
}
