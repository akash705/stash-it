package com.stashed.app.data.local

/** Decode a pipe-delimited media_paths string back to a list of absolute file paths. */
fun decodePaths(raw: String?): List<String> =
    raw?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
