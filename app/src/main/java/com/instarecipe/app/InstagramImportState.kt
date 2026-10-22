package com.instarecipe.app

import java.security.MessageDigest

private const val IMPORT_STATUS_SEPARATOR = "\n\n---\nImport status: "
private val CURRENT_IMPORT_STATUS_SUFFIX = Regex(
    """\n\n---\nImport status: (?:We couldn't create this recipe:|Connect a primary or backup Auth key|Instagram did not provide an accessible video or recipe caption\.|Instagram blocked video access\.)[^\r\n]+\s*$"""
)
private val LEGACY_IMPORT_STATUS_SUFFIX = Regex(
    """(?s)\n\n\[(?:We couldn't create this recipe:|Add a primary or backup Gemini API key|Connect a primary or backup Auth key|Connect a primary or backup recipe-creation key|Connect a free or paid recipe-creation key|Instagram did not provide an accessible video or recipe caption\.|Instagram blocked video access\.).*]\s*$"""
)

internal fun Recipe.isFailedInstagramImport(): Boolean {
    if (TagNormalizer.matches(tags, INSTAGRAM_PROCESSING_TAG)) return false
    val currentFailure = normalizedSourceUrl(sourceUrl) != null &&
        TagNormalizer.matches(tags, "Instagram") &&
        TagNormalizer.matches(tags, "Needs review") &&
        hasRecognizedImportFailure(notes)
    return currentFailure || isUnavailableInstagramImport()
}

internal fun Recipe.canExtractFromLinkAgain(): Boolean = isFailedInstagramImport()

/**
 * Older builds sometimes promoted an unsuccessful Instagram import to Saved without the
 * structured failure marker used by current builds. Keep this deliberately narrow so a real,
 * user-authored recipe with sparse content is never moved out of the cookbook by accident.
 */
internal fun Recipe.isUnavailableInstagramImport(): Boolean {
    val unavailableTitle = title.trim().lowercase() in setOf(
        "unavailable recipe",
        "recipe information unavailable",
        "unknown recipe",
        "untitled recipe",
        "instagram recipe",
        "instagram recipe draft",
        "creating recipe…",
        "creating recipe...",
        "recipe needs review"
    )
    return normalizedSourceUrl(sourceUrl) != null &&
        unavailableTitle &&
        ingredients.isEmpty() &&
        steps.isEmpty()
}

internal fun Recipe.belongsInImports(): Boolean =
    !isCookbookReady()

internal fun Recipe.persistedFingerprint(): String {
    val canonical = buildString {
        appendField(id.toString())
        appendField(title)
        appendField(sourceUrl)
        appendField(normalizedSourceUrl(sourceUrl).orEmpty())
        appendField(creator)
        appendField(category)
        appendList(TagNormalizer.normalizeAll(tags))
        appendList(ingredients)
        appendList(steps)
        appendField(notes)
        appendField(favorite.toString())
        appendField(cooked.toString())
        appendField(status.name)
        appendField(savedDate)
        appendField(totalTimeMinutes?.toString().orEmpty())
        appendField(activeTimeMinutes?.toString().orEmpty())
        appendField(yield)
        appendField(skillLevel)
        appendField(dietType.name)
    }
    return canonical.sha256()
}

internal fun instagramRetryText(recipe: Recipe): String {
    return stripInstagramImportFailureStatus(recipe.notes)
        .trim()
        .ifBlank { recipe.sourceUrl }
}

internal fun stripInstagramImportFailureStatus(text: String): String =
    CURRENT_IMPORT_STATUS_SUFFIX.replace(text, "")
        .let { LEGACY_IMPORT_STATUS_SUFFIX.replace(it, "") }

internal fun failedInstagramNotes(sharedText: String, message: String): String = buildString {
    append(sharedText)
    append(IMPORT_STATUS_SEPARATOR)
    append(message)
}

private fun hasRecognizedImportFailure(notes: String): Boolean =
    CURRENT_IMPORT_STATUS_SUFFIX.containsMatchIn(notes) || LEGACY_IMPORT_STATUS_SUFFIX.containsMatchIn(notes)

private fun StringBuilder.appendField(value: String) {
    append(value.length).append(':').append(value)
}

private fun StringBuilder.appendList(values: List<String>) {
    append(values.size).append('[')
    values.forEach(::appendField)
    append(']')
}

internal fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
