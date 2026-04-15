package com.stashed.app.intelligence

/**
 * Rule-based natural language parser.
 * Extracts a structured item + location from a plain English sentence.
 *
 * Tries 3 patterns in order of specificity (first match wins):
 *   1. [verb] [item] [preposition] [location]   → "put passport in wardrobe"
 *   2. [item] is/are [preposition] [location]   → "passport is in the wardrobe"
 *   3. [item] [preposition] [location]          → "passport in wardrobe"
 *
 * If nothing matches, the entire input is stored as the item (no location).
 * The embedding is still generated from raw text, so semantic search still works.
 */
object NLParser {

    data class ParsedMemory(
        val item: String,
        val location: String,
        val rawText: String,
    )

    private val VERBS = listOf("put", "placed", "left", "stored", "kept", "tossed", "stashed", "moved", "set")
    private val PREPOSITIONS = listOf(
        "on top of", "next to", "in front of", "out of",
        "inside", "behind", "beside", "beneath", "above", "under",
        "near", "at", "on", "in",
    )
    private val FILLER_WORDS = Regex("^(my|the|a|an)\\s+", RegexOption.IGNORE_CASE)

    // Pattern 1: verb + item + preposition + location
    // e.g. "put my passport in the wardrobe top shelf"
    private val verbPattern: Regex by lazy {
        val verbs = VERBS.joinToString("|")
        val preps = PREPOSITIONS.joinToString("|") { Regex.escape(it) }
        Regex(
            "(?:$verbs)\\s+(?:my\\s+|the\\s+|a\\s+)?(.+?)\\s+($preps)\\s+(?:the\\s+|my\\s+)?(.+)",
            RegexOption.IGNORE_CASE,
        )
    }

    // Pattern 2: item + is/are + preposition + location
    // e.g. "glasses are on the bedside table"
    private val isPattern: Regex by lazy {
        val preps = PREPOSITIONS.joinToString("|") { Regex.escape(it) }
        Regex(
            "(?:my\\s+|the\\s+)?(.+?)\\s+(?:is|are)\\s+($preps)\\s+(?:the\\s+|my\\s+)?(.+)",
            RegexOption.IGNORE_CASE,
        )
    }

    // Pattern 3: item + preposition + location (simplest)
    // e.g. "toilet key in office desk drawer"
    private val simplePattern: Regex by lazy {
        val preps = PREPOSITIONS.joinToString("|") { Regex.escape(it) }
        Regex(
            "(?:my\\s+|the\\s+)?(.+?)\\s+($preps)\\s+(?:the\\s+|my\\s+)?(.+)",
            RegexOption.IGNORE_CASE,
        )
    }

    fun parse(input: String): ParsedMemory {
        val trimmed = input.trim().trimEnd('.', '!', '?')

        for (pattern in listOf(verbPattern, isPattern, simplePattern)) {
            val match = pattern.find(trimmed) ?: continue
            val item = match.groupValues[1].trim().replace(FILLER_WORDS, "")
            val location = match.groupValues[3].trim().replace(FILLER_WORDS, "")
            if (item.isNotBlank() && location.isNotBlank()) {
                return ParsedMemory(
                    item = item.lowercase().replaceFirstChar { it.uppercase() },
                    location = location.lowercase().replaceFirstChar { it.uppercase() },
                    rawText = trimmed,
                )
            }
        }

        // Fallback: no pattern matched — store entire input as item
        return ParsedMemory(
            item = trimmed.replace(FILLER_WORDS, "")
                .lowercase().replaceFirstChar { it.uppercase() },
            location = "",
            rawText = trimmed,
        )
    }
}
