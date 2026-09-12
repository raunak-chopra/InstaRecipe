package com.instarecipe.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class VideoResolutionResult(
    val videoFile: File?,
    val caption: String?,
    val creator: String?,
    val videoUrl: String?,
    val sourceUrl: String
)

object InstagramResolver {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Community Cobalt instances with fallback
    val DEFAULT_RESOLVER_ENDPOINTS = listOf(
        "https://nuko-c.meowing.de",
        "https://bergung-api.hoffnungfuerdiezukunft.net"
    )

    private val INSTAGRAM_URL_REGEX = Pattern.compile(
        """https?://(?:www\.)?instagram\.com/(?:reel|reels|p|tv|share/reel)/([A-Za-z0-9_-]+)""",
        Pattern.CASE_INSENSITIVE
    )

    fun extractShortcode(instagramUrl: String): String? {
        val matcher = INSTAGRAM_URL_REGEX.matcher(instagramUrl)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Converts an Instagram alphanumeric shortcode to its internal numeric media ID.
     */
    fun shortcodeToMediaId(shortcode: String): Long {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        var mediaId = 0L
        for (ch in shortcode) {
            val index = alphabet.indexOf(ch)
            if (index >= 0) {
                mediaId = mediaId * 64 + index
            }
        }
        return mediaId
    }

    /**
     * Deletes any temporary video files cached in shared_reels to free device storage.
     */
    fun cleanCachedReelVideos(context: Context) {
        runCatching {
            val cacheDir = File(context.cacheDir, "shared_reels")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                val staleBefore = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < staleBefore) file.delete()
                }
            }
        }
    }

    /**
     * Extracts and cleans the standard Instagram URL from shared text.
     */
    fun extractInstagramUrl(sharedText: String): String? {
        val matcher = INSTAGRAM_URL_REGEX.matcher(sharedText)
        return if (matcher.find()) {
            val shortCode = matcher.group(1)
            "https://www.instagram.com/reel/$shortCode/"
        } else {
            val fallback = Regex("""https?://(?:www\.)?instagram\.com/\S+""", RegexOption.IGNORE_CASE)
                .find(sharedText)?.value?.trimEnd('.', ',', ')', '?', '!', '"', '\'')
            fallback?.split('?')?.firstOrNull()?.let { if (it.endsWith("/")) it else "$it/" }
        }
    }

    /**
     * Extracts any non-URL recipe caption or notes text that was shared along with the link.
     */
    fun extractCaptionFromSharedText(sharedText: String, extractedUrl: String?): String? {
        val url = extractedUrl ?: extractInstagramUrl(sharedText)
        var text = sharedText
        if (url != null) {
            text = text.replace(url, "")
        }
        // Remove any other URLs
        val cleaned = text.replace(Regex("""https?://\S+"""), "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()

        return cleaned.takeIf { GeminiRecipeExtractor.hasSubstantiveRecipeContent(it) }
    }

    /**
     * Resolves the Instagram URL to a direct video or extracts caption & details.
     */
    suspend fun resolveAndDownload(
        context: Context,
        instagramUrl: String,
        customResolverUrl: String? = null,
        supplementaryText: String? = null,
        onStatusUpdate: (String) -> Unit = {}
    ): VideoResolutionResult = withContext(Dispatchers.IO) {
        val cleanUrl = extractInstagramUrl(instagramUrl) ?: instagramUrl
        val sharedCaption = extractCaptionFromSharedText(supplementaryText.orEmpty(), cleanUrl)

        onStatusUpdate("Checking video stream and post details...")

        val shortcode = extractShortcode(cleanUrl)

        // 1. Try Authenticated In-App Instagram Session (Direct MP4 download, 100% reliable)
        val sessionCookies = InstagramSessionManager.getCookies(context)
        if (!sessionCookies.isNullOrBlank() && !shortcode.isNullOrBlank()) {
            onStatusUpdate("Fetching reel via authenticated Instagram session...")
            val authResult = resolveAuthenticatedMedia(
                context = context,
                cleanUrl = cleanUrl,
                shortcode = shortcode,
                cookies = sessionCookies,
                onStatusUpdate = onStatusUpdate
            )
            if (authResult?.videoFile != null && authResult.videoFile.exists() && authResult.videoFile.length() > 0) {
                val finalCaption = if (!sharedCaption.isNullOrBlank()) sharedCaption else authResult.caption
                return@withContext authResult.copy(caption = finalCaption)
            }
        }

        // 2. Try Community / Custom Cobalt Resolvers (Unauthenticated Fallback)
        val candidateEndpoints = mutableListOf<String>()
        if (!customResolverUrl.isNullOrBlank()) {
            candidateEndpoints.add(customResolverUrl.trimEnd('/'))
        }
        candidateEndpoints.addAll(DEFAULT_RESOLVER_ENDPOINTS)

        var resolvedVideoUrl: String? = null
        var extractedCaption: String? = sharedCaption
        var extractedCreator: String? = null

        // Try resolvers
        for (endpoint in candidateEndpoints) {
            try {
                val direct = queryCobaltResolver(endpoint, cleanUrl)
                if (!direct.isNullOrBlank()) {
                    resolvedVideoUrl = direct
                    break
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }

        // If cobalt resolver succeeded, download video
        if (!resolvedVideoUrl.isNullOrBlank()) {
            onStatusUpdate("Downloading reel video for AI analysis...")
            try {
                val downloadedFile = downloadVideoToCache(context, resolvedVideoUrl)
                if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0) {
                    return@withContext VideoResolutionResult(
                        videoFile = downloadedFile,
                        caption = extractedCaption,
                        creator = extractedCreator,
                        videoUrl = resolvedVideoUrl,
                        sourceUrl = cleanUrl
                    )
                }
            } catch (e: Exception) {
                onStatusUpdate("Download failed, inspecting post caption...")
            }
        }

        // Check caption and post metadata directly
        onStatusUpdate("Checking post caption and creator details...")
        try {
            val pageInfo = fetchInstagramPageInfo(cleanUrl)
            if (extractedCaption.isNullOrBlank() && !pageInfo.first.isNullOrBlank()) {
                extractedCaption = pageInfo.first
            }
            if (extractedCreator.isNullOrBlank() && !pageInfo.second.isNullOrBlank()) {
                extractedCreator = pageInfo.second
            }
        } catch (_: Exception) {}

        VideoResolutionResult(
            videoFile = null,
            caption = extractedCaption,
            creator = extractedCreator,
            videoUrl = resolvedVideoUrl,
            sourceUrl = cleanUrl
        )
    }

    private fun resolveAuthenticatedMedia(
        context: Context,
        cleanUrl: String,
        shortcode: String,
        cookies: String,
        onStatusUpdate: (String) -> Unit
    ): VideoResolutionResult? {
        val mediaId = shortcodeToMediaId(shortcode)
        val csrfToken = InstagramSessionManager.getCsrfToken(context).orEmpty()

        // 1. Try Mobile Private API: /api/v1/media/{media_id}/info/
        try {
            onStatusUpdate("Fetching direct video via Instagram session...")
            val apiUrl = "https://i.instagram.com/api/v1/media/$mediaId/info/"
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", "Instagram 275.0.0.27.98 Android (30/11; 480dpi; 1080x2400; samsung; SM-G973U; beyond1; qcom; en_US)")
                .addHeader("Cookie", cookies)
                .addHeader("X-IG-App-ID", "936619743392459")
                .addHeader("X-ASBD-ID", "198387")
                .addHeader("X-IG-WWW-Claim", "0")
                .apply {
                    if (csrfToken.isNotBlank()) addHeader("X-CSRFToken", csrfToken)
                }
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val items = json.optJSONArray("items")
                        if (items != null && items.length() > 0) {
                            val item = items.getJSONObject(0)
                            val user = item.optJSONObject("user")
                            val username = user?.optString("username")
                            val captionObj = item.optJSONObject("caption")
                            val captionText = captionObj?.optString("text")

                            var videoUrl: String? = null
                            val videoVersions = item.optJSONArray("video_versions")
                            if (videoVersions != null && videoVersions.length() > 0) {
                                videoUrl = videoVersions.getJSONObject(0).optString("url")
                            } else {
                                val carousel = item.optJSONArray("carousel_media")
                                if (carousel != null && carousel.length() > 0) {
                                    for (i in 0 until carousel.length()) {
                                        val cItem = carousel.getJSONObject(i)
                                        val cVideos = cItem.optJSONArray("video_versions")
                                        if (cVideos != null && cVideos.length() > 0) {
                                            videoUrl = cVideos.getJSONObject(0).optString("url")
                                            break
                                        }
                                    }
                                }
                            }

                            if (!videoUrl.isNullOrBlank()) {
                                onStatusUpdate("Downloading reel video for AI analysis...")
                                val videoFile = downloadVideoToCache(context, videoUrl)
                                if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                                    return VideoResolutionResult(
                                        videoFile = videoFile,
                                        caption = captionText,
                                        creator = username,
                                        videoUrl = videoUrl,
                                        sourceUrl = cleanUrl
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Try GraphQL logged-in query
        try {
            val queryUrl = "https://www.instagram.com/graphql/query/?doc_id=8845758582119845&variables=%7B%22shortcode%22%3A%22$shortcode%22%7D"
            val request = Request.Builder()
                .url(queryUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 11; SAMSUNG SM-G973U) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/14.2 Chrome/87.0.4280.141 Mobile Safari/537.36")
                .addHeader("Cookie", cookies)
                .addHeader("X-IG-App-ID", "1217981644879628")
                .apply {
                    if (csrfToken.isNotBlank()) addHeader("X-CSRFToken", csrfToken)
                }
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val media = json.optJSONObject("data")?.optJSONObject("xdt_shortcode_media")
                        if (media != null) {
                            val videoUrl = media.optString("video_url").takeIf { it.isNotBlank() }
                            val owner = media.optJSONObject("owner")?.optString("username")
                            val edgeMediaToCaption = media.optJSONObject("edge_media_to_caption")
                            val edges = edgeMediaToCaption?.optJSONArray("edges")
                            val caption = if (edges != null && edges.length() > 0) {
                                edges.getJSONObject(0).optJSONObject("node")?.optString("text")
                            } else null

                            if (!videoUrl.isNullOrBlank()) {
                                onStatusUpdate("Downloading reel video for AI analysis...")
                                val videoFile = downloadVideoToCache(context, videoUrl)
                                if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                                    return VideoResolutionResult(
                                        videoFile = videoFile,
                                        caption = caption,
                                        creator = owner,
                                        videoUrl = videoUrl,
                                        sourceUrl = cleanUrl
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Try Authenticated HTML fetch (og:video tag)
        try {
            val pageReq = Request.Builder()
                .url("https://www.instagram.com/reel/$shortcode/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Cookie", cookies)
                .build()

            client.newCall(pageReq).execute().use { res ->
                if (res.isSuccessful) {
                    val html = res.body?.string().orEmpty()
                    val videoMatcher = Pattern.compile("""<meta\s+property=["']og:video["']\s+content=["'](.*?)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
                    if (videoMatcher.find()) {
                        val rawUrl = videoMatcher.group(1)?.replace("&amp;", "&")
                        if (!rawUrl.isNullOrBlank()) {
                            val videoFile = downloadVideoToCache(context, rawUrl)
                            if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                                return VideoResolutionResult(
                                    videoFile = videoFile,
                                    caption = null,
                                    creator = null,
                                    videoUrl = rawUrl,
                                    sourceUrl = cleanUrl
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun queryCobaltResolver(endpoint: String, targetUrl: String): String? {
        if (!endpoint.startsWith("https://", ignoreCase = true)) return null
        val jsonPayload = JSONObject().apply {
            put("url", targetUrl)
            put("videoQuality", "720")
            put("downloadMode", "auto")
        }

        val request = Request.Builder()
            .url(if (endpoint.endsWith("/")) endpoint else "$endpoint/")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "InstaRecipe/1.0")
            .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val json = JSONObject(bodyStr)

            when {
                json.has("url") -> json.getString("url")
                json.has("picker") -> {
                    val picker = json.getJSONArray("picker")
                    if (picker.length() > 0) picker.getJSONObject(0).optString("url") else null
                }
                else -> null
            }
        }
    }

    private fun downloadVideoToCache(context: Context, videoUrl: String): File? {
        val request = Request.Builder()
            .url(videoUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null

            val cacheDir = File(context.cacheDir, "shared_reels")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val tempFile = File(cacheDir, "reel_${System.currentTimeMillis()}.mp4")
            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 1024) tempFile else null
        }
    }

    private fun fetchInstagramPageInfo(instagramUrl: String): Pair<String?, String?> {
        // Try oEmbed endpoint first
        val oembedUrl = "https://api.instagram.com/oembed/?url=$instagramUrl&omitscript=true"
        val request = Request.Builder()
            .url(oembedUrl)
            .addHeader("User-Agent", "InstaRecipe/1.0")
            .build()

        var oembedResult: Pair<String?, String?> = Pair(null, null)
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val title = json.optString("title").takeIf { it.isNotBlank() }
                        val author = json.optString("author_name").takeIf { it.isNotBlank() }
                        oembedResult = Pair(title, author)
                    }
                }
            }
        } catch (_: Exception) {}

        if (oembedResult.first != null || oembedResult.second != null) {
            return oembedResult
        }

        // Fallback: fetch page HTML to inspect title tag and open graph metadata
        return try {
            val pageReq = Request.Builder()
                .url(instagramUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(pageReq).execute().use { res ->
                if (!res.isSuccessful) return Pair(null, null)
                val html = res.body?.string().orEmpty()

                // Check title tag: "<title>Creator on Instagram: "Caption text""
                val titleMatcher = Pattern.compile(
                    """<title>(.*?)\s+on\s+Instagram:\s*[“"](.*?)[”"]\s*</title>""",
                    Pattern.CASE_INSENSITIVE or Pattern.DOTALL
                ).matcher(html)

                if (titleMatcher.find()) {
                    val creator = titleMatcher.group(1)?.trim()
                    val caption = titleMatcher.group(2)?.trim()
                    return Pair(caption, creator)
                }

                // Check og:description
                val descMatcher = Pattern.compile(
                    """<meta\s+(?:property|name)=["'](?:og:description|description)["']\s+content=["'](.*?)["']""",
                    Pattern.CASE_INSENSITIVE
                ).matcher(html)
                val desc = if (descMatcher.find()) descMatcher.group(1)?.trim() else null

                // Check og:title
                val titleMetaMatcher = Pattern.compile(
                    """<meta\s+(?:property|name)=["']og:title["']\s+content=["'](.*?)["']""",
                    Pattern.CASE_INSENSITIVE
                ).matcher(html)
                val titleMeta = if (titleMetaMatcher.find()) titleMetaMatcher.group(1)?.trim() else null

                Pair(desc, titleMeta)
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }
}
