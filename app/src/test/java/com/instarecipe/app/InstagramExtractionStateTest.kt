package com.instarecipe.app

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramExtractionStateTest {
    private val sourceUrl = "https://www.instagram.com/reel/test/"

    @Test
    fun successfulBackgroundExtractionStaysInImportsForReview() {
        val completed = completedInstagramRecipe(
            pending = recipe(41L, RecipeStatus.Draft, listOf("Instagram", INSTAGRAM_PROCESSING_TAG)),
            extracted = ExtractedRecipeData(
                title = "Soup",
                creator = "Chef",
                category = "Dinner",
                tags = listOf("Quick"),
                ingredients = listOf("Tomato"),
                steps = listOf("Simmer"),
                notes = "Serve warm",
                sourceUrl = sourceUrl
            )
        )

        assertEquals(41L, completed.id)
        assertEquals(RecipeStatus.Draft, completed.status)
        assertTrue(completed.tags.contains("Instagram"))
        assertTrue(completed.tags.contains("Needs review"))
        assertTrue(INSTAGRAM_PROCESSING_TAG !in completed.tags)
    }

    @Test
    fun successfulExtractionPreservesCapturedTextForReview() {
        val completed = completedInstagramRecipe(
            pending = recipe(42L, RecipeStatus.Draft, listOf("Instagram", INSTAGRAM_PROCESSING_TAG))
                .copy(notes = "https://www.instagram.com/reel/test/\nCaption with quantities"),
            extracted = ExtractedRecipeData(
                title = "Soup",
                creator = "",
                category = "Dinner",
                tags = emptyList(),
                ingredients = listOf("Tomato"),
                steps = listOf("Simmer"),
                notes = "Serve warm",
                sourceUrl = sourceUrl
            )
        )

        assertEquals(RecipeStatus.Draft, completed.status)
        assertTrue(completed.notes.contains("Caption with quantities"))
        assertTrue(completed.notes.contains("Serve warm"))
    }

    @Test
    fun cookbookReadyRequiresSavedRecipeWithTitleIngredientsAndSteps() {
        val valid = recipe(43L, RecipeStatus.Saved, listOf("Dinner")).copy(
            title = "Tomato soup",
            ingredients = listOf("Tomatoes"),
            steps = listOf("Simmer until soft")
        )
        val blank = valid.copy(title = "Untitled Recipe", ingredients = emptyList(), steps = emptyList())
        val draft = valid.copy(status = RecipeStatus.Draft)

        assertTrue(valid.isCookbookReady())
        assertFalse(blank.isCookbookReady())
        assertFalse(draft.isCookbookReady())
        assertTrue(blank.belongsInImports())
    }

    @Test
    fun dietTypeDrivesVegAndNonVegFilters() {
        val veg = recipe(44L, RecipeStatus.Saved, listOf("Dinner")).copy(
            title = "Chickpea curry",
            ingredients = listOf("Chickpeas"),
            steps = listOf("Simmer"),
            dietType = DietType.Vegetarian
        )
        val nonVeg = veg.copy(id = 45L, title = "Chicken curry", dietType = DietType.NonVegetarian)

        assertTrue(veg.matchesHomeCategory("category:veg"))
        assertFalse(veg.matchesHomeCategory("category:nonveg"))
        assertTrue(nonVeg.matchesHomeCategory("category:nonveg"))
        assertFalse(nonVeg.matchesHomeCategory("category:veg"))
    }

    @Test
    fun failedDraftIsReclaimedWhenLinkIsSharedAgain() = runTest {
        val existing = recipe(8L, RecipeStatus.Draft, listOf("Instagram", "Needs review"))
        val store = ExtractionRecipeStore(existing)

        val pending = claimInstagramExtraction(store, sourceUrl, "shared caption")

        assertEquals(8L, pending?.id)
        assertEquals(RecipeStatus.Draft, pending?.status)
        assertTrue(pending?.tags.orEmpty().contains(INSTAGRAM_PROCESSING_TAG))
        assertEquals("Creating recipe…", pending?.title)
        assertEquals(1, store.upsertCount)
    }

    @Test
    fun savedRecipeIsNotQueuedAgain() = runTest {
        val store = ExtractionRecipeStore(recipe(9L, RecipeStatus.Saved, listOf("Dinner")))

        val pending = claimInstagramExtraction(store, sourceUrl, "shared caption")

        assertNull(pending)
        assertEquals(0, store.upsertCount)
    }

    @Test
    fun failedDraftStillQueuesWhenLinkIsSharedAgain() {
        val failedDraft = recipe(10L, RecipeStatus.Draft, listOf("Instagram", "Needs review"))

        assertTrue(shouldQueueInstagramExtraction(listOf(failedDraft), sourceUrl))
    }

    @Test
    fun savedRecipePreventsDuplicateBackgroundWork() {
        val saved = recipe(11L, RecipeStatus.Saved, listOf("Dinner"))

        assertTrue(!shouldQueueInstagramExtraction(listOf(saved), sourceUrl))
    }

    @Test
    fun savedRecipeWithEquivalentInstagramUrlPreventsDuplicateBackgroundWork() {
        val saved = recipe(111L, RecipeStatus.Saved, listOf("Dinner")).copy(
            sourceUrl = "https://instagram.com/reel/test/?igsh=tracking"
        )

        assertFalse(shouldQueueInstagramExtraction(listOf(saved), sourceUrl))
    }

    @Test
    fun cardRetryReusesOriginalShareTextWithoutFailureNotice() {
        val failed = recipe(12L, RecipeStatus.Draft, listOf("Instagram", "Needs review")).copy(
            notes = "Tasty tomato pasta https://www.instagram.com/reel/test/\n\n[We couldn't create this recipe: timeout]"
        )

        assertEquals(
            "Tasty tomato pasta https://www.instagram.com/reel/test/",
            instagramRetryText(failed)
        )
    }

    @Test
    fun cardRetryPreservesBracketedCaptionSections() {
        val failed = recipe(13L, RecipeStatus.Draft, listOf("Instagram", "Needs review")).copy(
            notes = "Pasta\n\n[Ingredients]\nTomatoes and basil\n\n---\nImport status: We couldn't create this recipe: The network timed out."
        )

        assertEquals("Pasta\n\n[Ingredients]\nTomatoes and basil", instagramRetryText(failed))
    }

    @Test
    fun retryPreservesFavoriteAndCookedState() = runTest {
        val existing = recipe(14L, RecipeStatus.Draft, listOf("Instagram", "Needs review")).copy(
            favorite = true,
            cooked = true
        )
        val store = ExtractionRecipeStore(existing)

        val pending = claimInstagramExtraction(store, sourceUrl, "shared caption")

        assertTrue(pending?.favorite == true)
        assertTrue(pending?.cooked == true)
    }

    @Test
    fun onlyRecognizedFailedInstagramImportsCanRetry() {
        val failureNotes = "Shared caption\n\n---\nImport status: We couldn't create this recipe: Network unavailable"
        val eligible = recipe(15L, RecipeStatus.Saved, listOf("Instagram", "Needs review"))
            .copy(notes = failureNotes)
        val userAuthored = eligible.copy(id = 16L, tags = listOf("Needs review"))
        val processing = eligible.copy(id = 17L, tags = listOf("Instagram", "Needs review", INSTAGRAM_PROCESSING_TAG))

        assertTrue(eligible.canExtractFromLinkAgain())
        assertFalse(userAuthored.canExtractFromLinkAgain())
        assertFalse(processing.canExtractFromLinkAgain())
    }

    @Test
    fun savedRetryUsesExactFingerprintAndDoesNotWritePendingDraft() = runTest {
        val saved = recipe(18L, RecipeStatus.Saved, listOf("Instagram", "Needs review")).copy(
            notes = "Shared caption\n\n---\nImport status: We couldn't create this recipe: Network unavailable",
            ingredients = listOf("Keep me")
        )
        val store = ExtractionRecipeStore(saved)

        val claim = claimInstagramRetry(
            repository = store,
            targetId = saved.id,
            sourceUrl = saved.sourceUrl,
            sharedText = instagramRetryText(saved),
            expectedFingerprint = saved.persistedFingerprint()
        )

        assertTrue(claim is InstagramExtractionClaim.Existing)
        assertEquals(listOf("Keep me"), claim?.recipe?.ingredients)
        assertEquals(0, store.upsertCount)
    }

    @Test
    fun savedRetryRejectsStaleFingerprint() = runTest {
        val saved = recipe(19L, RecipeStatus.Saved, listOf("Instagram", "Needs review")).copy(
            notes = "Shared caption\n\n---\nImport status: We couldn't create this recipe: Network unavailable"
        )
        val store = ExtractionRecipeStore(saved.copy(title = "Edited after retry"))

        val claim = claimInstagramRetry(
            repository = store,
            targetId = saved.id,
            sourceUrl = saved.sourceUrl,
            sharedText = instagramRetryText(saved),
            expectedFingerprint = saved.persistedFingerprint()
        )

        assertNull(claim)
        assertEquals(0, store.upsertCount)
    }

    @Test
    fun explicitDraftRetryAlsoKeepsFailedContentUntilSuccess() = runTest {
        val draft = recipe(20L, RecipeStatus.Draft, listOf("Instagram", "Needs review")).copy(
            notes = "Shared caption\n\n---\nImport status: We couldn't create this recipe: Network unavailable",
            ingredients = listOf("User-entered ingredient")
        )
        val store = ExtractionRecipeStore(draft)

        val claim = claimInstagramRetry(
            repository = store,
            targetId = draft.id,
            sourceUrl = draft.sourceUrl,
            sharedText = instagramRetryText(draft),
            expectedFingerprint = draft.persistedFingerprint()
        )

        assertTrue(claim is InstagramExtractionClaim.Existing)
        assertEquals(listOf("User-entered ingredient"), claim?.recipe?.ingredients)
        assertEquals(0, store.upsertCount)
    }

    @Test
    fun fingerprintChangesWhenAnyPersistedContentChanges() {
        val base = recipe(21L, RecipeStatus.Saved, listOf("Instagram", "Needs review")).copy(
            notes = "Shared caption\n\n---\nImport status: We couldn't create this recipe: Network unavailable"
        )
        val changedRecipes = listOf(
            base.copy(title = "Changed"),
            base.copy(sourceUrl = "https://www.instagram.com/reel/other/"),
            base.copy(creator = "Changed"),
            base.copy(category = "Changed"),
            base.copy(tags = base.tags + "Changed"),
            base.copy(ingredients = listOf("Changed")),
            base.copy(steps = listOf("Changed")),
            base.copy(notes = "Changed"),
            base.copy(favorite = true),
            base.copy(cooked = true),
            base.copy(status = RecipeStatus.Draft),
            base.copy(savedDate = "2026-09-17")
        )

        changedRecipes.forEach { changed ->
            assertNotEquals(base.persistedFingerprint(), changed.persistedFingerprint())
        }
    }

    @Test
    fun arbitraryOrEmbeddedStatusTextDoesNotAuthorizeRetry() {
        val base = recipe(22L, RecipeStatus.Saved, listOf("Instagram", "Needs review"))
        val embedded = base.copy(notes = "Ordinary note\n\n---\nImport status: We couldn't create this recipe: old\nMore user text")
        val arbitrary = base.copy(notes = "Ordinary note\n\n---\nImport status: My own reminder")
        val empty = base.copy(notes = "Ordinary note\n\n---\nImport status: ")

        assertFalse(embedded.canExtractFromLinkAgain())
        assertFalse(arbitrary.canExtractFromLinkAgain())
        assertFalse(empty.canExtractFromLinkAgain())
    }

    @Test
    fun legacyUnavailableSavedRecipeReturnsToImportsAndCanRetry() {
        val legacyFailure = recipe(23L, RecipeStatus.Saved, emptyList()).copy(
            title = "Recipe Information Unavailable"
        )

        assertTrue(legacyFailure.isUnavailableInstagramImport())
        assertTrue(legacyFailure.belongsInImports())
        assertTrue(legacyFailure.canExtractFromLinkAgain())
    }

    @Test
    fun sparseUserRecipeIsNotMistakenForUnavailableImport() {
        val userRecipe = recipe(24L, RecipeStatus.Saved, emptyList()).copy(title = "Grandma's soup")

        assertFalse(userRecipe.isUnavailableInstagramImport())
        assertTrue(userRecipe.belongsInImports())
        assertFalse(userRecipe.canExtractFromLinkAgain())
    }

    @Test
    fun emptyPlaceholderInstagramRecipeReturnsToImportsForRetry() {
        val placeholder = recipe(25L, RecipeStatus.Saved, emptyList()).copy(title = "Unknown Recipe")

        assertTrue(placeholder.isUnavailableInstagramImport())
        assertTrue(placeholder.belongsInImports())
        assertTrue(placeholder.canExtractFromLinkAgain())
    }

    private fun recipe(id: Long, status: RecipeStatus, tags: List<String>) = Recipe(
        id = id,
        title = "Existing",
        sourceUrl = sourceUrl,
        creator = "",
        category = "Saved to try",
        tags = tags,
        ingredients = emptyList(),
        steps = emptyList(),
        notes = "",
        favorite = false,
        cooked = false,
        status = status,
        savedDate = "2026-09-14"
    )
}

private class ExtractionRecipeStore(initial: Recipe?) : RecipeStore {
    override val recipes: Flow<List<Recipe>> = emptyFlow()
    override fun searchIds(query: String): Flow<Set<Long>> = emptyFlow()
    private var current = initial
    var upsertCount = 0

    override suspend fun upsert(recipe: Recipe): Recipe {
        upsertCount++
        current = recipe
        return recipe
    }

    override suspend fun update(id: Long, transform: Recipe.() -> Recipe) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun findById(id: Long): Recipe? = current
    override suspend fun findBySourceUrl(sourceUrl: String): Recipe? = current
    override suspend fun createOrGetBySourceUrl(recipe: Recipe): Recipe = recipe.copy(id = 1L).also { current = it }
    override suspend fun completeInstagramRetry(
        id: Long,
        expectedFingerprint: String,
        replacement: Recipe
    ): Boolean {
        val existing = current ?: return false
        if (existing.id != id || existing.persistedFingerprint() != expectedFingerprint) return false
        current = replacement
        return true
    }
    override suspend fun migrateLegacyPreferences(context: Context) = Unit
}
