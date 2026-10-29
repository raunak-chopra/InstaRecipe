package com.instarecipe.app

import kotlinx.serialization.json.*

/** Uses only comments present in the requested media's returned metadata, never unrelated posts. */
internal object InstagramRecipeComments {
    private val containers = setOf("preview_comments", "comments", "edge_media_preview_comment", "edge_media_to_parent_comment", "edge_media_to_comment")
    private val quantity = Regex("""(?i)\b\d+(?:[./]\d+)?\s*(?:cups?|tbsp|tsp|teaspoons?|tablespoons?|grams?|g|kg|ml|litres?|minutes?|mins?|eggs?|cloves?)\b""")
    private val method = Regex("""(?i)\b(ingredients|method|instructions)\b""")
    private val action = Regex("""(?i)\b(mix|add|cook|bake|boil|fry|stir|roast|simmer|blend)\b""")
    private const val MARKER = "Recipe comments from Instagram"

    fun fromMediaJson(json: String): String? = parse(json)?.let(::fromMedia)

    fun fromHtml(html: String, shortcode: String?): String? {
        if (shortcode.isNullOrBlank()) return null
        var visited = 0
        fun find(element: JsonElement, depth: Int): String? {
            if (depth > 20 || ++visited > 5000) return null
            return when (element) {
                is JsonObject -> {
                    val code = element.text("shortcode") ?: element.text("code")
                    if (code == shortcode) fromMedia(element)
                    else element.values.firstNotNullOfOrNull { find(it, depth + 1) }
                }
                is JsonArray -> element.firstNotNullOfOrNull { find(it, depth + 1) }
                else -> null
            }
        }
        return Regex("""(?is)<script\b[^>]*>(.*?)</script>""").findAll(html).take(40).firstNotNullOfOrNull { script ->
            val text = script.groupValues[1].trim().removePrefix("window._sharedData =").trim().trimEnd(';')
            parse(text)?.let { find(it, 0) }
        }
    }

    fun combine(caption: String?, comments: String?): String? = listOfNotNull(caption?.takeIf(String::isNotBlank), comments)
        .distinct().joinToString("\n\n").takeIf(String::isNotBlank)

    fun includesComments(text: String?): Boolean = text?.contains(MARKER) == true

    private data class Comment(val text: String, val author: String?, val creator: Boolean, val pinned: Boolean)

    private fun fromMedia(element: JsonElement): String? {
        val media = element as? JsonObject ?: return null
        val creator = ((media["user"] ?: media["owner"]) as? JsonObject)?.text("username")
        val comments = mutableListOf<Comment>()
        var visited = 0
        fun collect(value: JsonElement, depth: Int) {
            if (depth > 8 || ++visited > 500 || comments.size >= 50) return
            when (value) {
                is JsonArray -> value.take(50).forEach { collect(it, depth + 1) }
                is JsonObject -> {
                    val body = value.text("text")?.trim()?.take(4000)
                    if (body != null) {
                        val author = ((value["user"] ?: value["owner"]) as? JsonObject)?.text("username")
                        if (body.length >= 40 && (quantity.containsMatchIn(body) ||
                            (method.containsMatchIn(body) && action.containsMatchIn(body)))) {
                            comments += Comment(body, author, !creator.isNullOrBlank() && creator.equals(author, true),
                                (value["is_pinned"] as? JsonPrimitive)?.booleanOrNull == true)
                        }
                    } else value.filterKeys { it in setOf("edges", "node", "data", "items") }.values.forEach { collect(it, depth + 1) }
                }
                else -> Unit
            }
        }
        media.filterKeys { it in containers }.values.forEach { collect(it, 0) }
        val selected = comments.distinctBy { it.text }.let { all -> all.filter { it.creator }.ifEmpty { all } }
            .sortedByDescending { it.pinned }.take(5)
        if (selected.isEmpty()) return null
        return buildString {
            append(MARKER).append(" (review before saving):\n")
            selected.forEach {
                append(if (it.creator) "Creator comment" else "Community comment — unverified")
                it.author?.let { author -> append(" (@").append(author.take(60)).append(')') }
                append(":\n").append(it.text).append("\n\n")
            }
        }.trim()
    }

    private fun parse(value: String): JsonElement? = if (value.length > 1_048_576) null else
        runCatching { GeminiJson.parseToJsonElement(value) }.getOrNull()
    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
}
