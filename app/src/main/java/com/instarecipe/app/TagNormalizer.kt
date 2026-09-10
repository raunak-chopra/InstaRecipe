package com.instarecipe.app

import java.util.Locale

object TagNormalizer {
    const val MaxTags = 12
    const val MaxTagLength = 30

    private val aliases = mapOf(
        "high protein" to "High Protein",
        "high-protein" to "High Protein",
        "protein" to "High Protein",
        "veg" to "Vegetarian",
        "vegetarian" to "Vegetarian",
        "quick recipe" to "Quick",
        "quick recipes" to "Quick",
        "quick" to "Quick"
    )

    fun key(tag: String): String = clean(tag).lowercase(Locale.ROOT)

    fun normalize(tag: String): String? {
        val cleaned = clean(tag).take(MaxTagLength)
        if (cleaned.isBlank()) return null
        return aliases[cleaned.lowercase(Locale.ROOT)] ?: cleaned
    }

    fun normalizeAll(tags: Iterable<String>): List<String> {
        val seen = mutableSetOf<String>()
        return tags.mapNotNull(::normalize)
            .filter { seen.add(key(it)) }
            .take(MaxTags)
    }

    fun parse(input: String): List<String> = normalizeAll(input.split(',', '\n'))

    fun matches(tags: Iterable<String>, expected: String): Boolean {
        val expectedKey = key(expected)
        return tags.any { key(it) == expectedKey }
    }

    private fun clean(tag: String): String = tag
        .trim()
        .removePrefix("#")
        .trim()
        .replace(Regex("\\s+"), " ")
}
