package com.instarecipe.app

import org.junit.Assert.*
import org.junit.Test

class InstagramRecipeCommentsTest {
    private val creatorRecipe = "Mix 2 cups flour with water. Add salt and cook for 10 minutes."
    private val communityRecipe = "Use 3 cups flour instead, mix with water and fry for 5 minutes."

    @Test fun creatorRecipesTakePriorityOverConflictingCommunitySuggestions() {
        val json = """{"user":{"username":"chef"},"preview_comments":[{"text":"$communityRecipe","user":{"username":"fan"}},{"text":"$creatorRecipe","user":{"username":"chef"}}]}"""
        val result = InstagramRecipeComments.fromMediaJson(json)!!
        assertTrue(result.contains(creatorRecipe))
        assertTrue(result.contains("Creator comment (@chef)"))
        assertFalse(result.contains(communityRecipe))
    }

    @Test fun communityRecipeTextIsLabelledUnverified() {
        val result = InstagramRecipeComments.fromMediaJson("""{"preview_comments":[{"text":"$communityRecipe","user":{"username":"fan"}}]}""")!!
        assertTrue(result.contains("Community comment — unverified"))
    }

    @Test fun pageCommentsMustBelongToTheRequestedReel() {
        val html = """<script type="application/json">{"posts":[{"shortcode":"other","owner":{"username":"chef"},"edge_media_to_parent_comment":{"edges":[{"node":{"text":"$creatorRecipe","owner":{"username":"chef"}}}]}}]}</script>"""
        assertNull(InstagramRecipeComments.fromHtml(html, "wanted"))
        assertTrue(InstagramRecipeComments.fromHtml(html, "other")!!.contains(creatorRecipe))
    }

    @Test fun malformedAndNonRecipeCommentsDoNotBecomeCookingInstructions() {
        assertNull(InstagramRecipeComments.fromMediaJson("not JSON"))
        assertNull(InstagramRecipeComments.fromMediaJson("""{"preview_comments":[{"text":"Looks great! Please send me the recipe, I would really love to make this."}]}"""))
        assertEquals("Caption", InstagramRecipeComments.combine("Caption", null))
    }
}
