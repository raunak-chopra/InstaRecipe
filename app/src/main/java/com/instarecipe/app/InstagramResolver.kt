package com.instarecipe.app

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class VideoResolutionResult(
    val videoFile: File?,
    val caption: String?,
    val creator: String?,
    val videoUrl: String?,
    val sourceUrl: String
)

private data class InstagramPageInfo(
    val caption: String?,
    val creator: String?,
    val videoUrl: String?
)

@Serializable
private data class ConfiguredResolverRequestDto(
    val url: String,
    val videoQuality: String = "720",
    val downloadMode: String = "auto",
    val profileUrl: String? = null
)

object InstagramResolver {
    internal fun shouldPreferCaptionOnly(caption: String?): Boolean {
        if (!GeminiRecipeExtractor.hasSubstantiveRecipeContent(caption)) return false
        val words = caption.orEmpty().split(Regex("\\s+")).count { it.isNotBlank() }
        return caption.orEmpty().trim().length >= 80 || words >= 12
    }

    private val publicOnlyDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            Dns.SYSTEM.lookup(hostname).also { addresses ->
                require(addresses.isNotEmpty() && addresses.none(InetAddress::isUnsafeForRemoteMedia)) {
                    "Resolver returned an unsafe network destination."
                }
            }
    }

    private val metadataClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .dns(publicOnlyDns)
        .build()

    private val mediaClient = metadataClient.newBuilder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    private val instagramUrlRegex = Pattern.compile(
        """https://(?:www\.)?instagram\.com/(?:reel|reels|p|tv|share/reel)/([A-Za-z0-9_-]+)""",
        Pattern.CASE_INSENSITIVE
    )

    fun extractShortcode(instagramUrl: String): String? {
        val matcher = instagramUrlRegex.matcher(instagramUrl)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun isInstagramProfileUrl(url: String): Boolean {
        val parsed = url.trim().toHttpUrlOrNull() ?: return false
        if (!parsed.isHttps || parsed.host.lowercase() !in setOf("instagram.com", "www.instagram.com")) return false
        val segments = parsed.pathSegments.filter(String::isNotBlank)
        return segments.size == 1 && segments[0] !in setOf("accounts", "explore", "reels", "direct")
    }

    fun shortcodeToMediaId(shortcode: String): Long {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        var mediaId = 0L
        for (character in shortcode) {
            val index = alphabet.indexOf(character)
            if (index >= 0) mediaId = mediaId * 64 + index
        }
        return mediaId
    }

    fun cleanCachedReelVideos(context: Context) {
        runCatching {
            val cacheDirectory = File(context.cacheDir, CACHE_DIRECTORY)
            val staleBefore = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            cacheDirectory.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < staleBefore) file.delete()
            }
        }
    }

    fun extractInstagramUrl(sharedText: String): String? {
        val matcher = instagramUrlRegex.matcher(sharedText)
        if (matcher.find()) return "https://www.instagram.com/reel/${matcher.group(1)}/"

        return Regex("""https://(?:www\.)?instagram\.com/\S+""", RegexOption.IGNORE_CASE)
            .find(sharedText)?.value
            ?.trimEnd('.', ',', ')', '?', '!', '"', '\'')
            ?.substringBefore('?')
            ?.let { if (it.endsWith('/')) it else "$it/" }
    }

    fun extractCaptionFromSharedText(sharedText: String, extractedUrl: String?): String? {
        val cleaned = stripInstagramImportFailureStatus(sharedText)
            .replace(Regex("""https://(?:www\.)?instagram\.com/\S+""", RegexOption.IGNORE_CASE), "")
            .lines().map(String::trim).filter(String::isNotBlank).joinToString("\n").trim()
        return cleaned.takeIf(GeminiRecipeExtractor::hasSubstantiveRecipeContent)
    }

    suspend fun resolveAndDownload(
        context: Context,
        instagramUrl: String,
        customResolverUrl: String? = null,
        preferredProfileUrl: String? = null,
        supplementaryText: String? = null,
        onStatusUpdate: (String) -> Unit = {}
    ): VideoResolutionResult = withContext(Dispatchers.IO) {
        val cleanUrl = extractInstagramUrl(instagramUrl)
            ?: throw IllegalArgumentException("Enter a valid HTTPS Instagram post or reel link.")
        var caption = extractCaptionFromSharedText(supplementaryText.orEmpty(), cleanUrl)
        var creator: String? = null
        var resolvedVideoUrl: String? = null

        // A shared caption already contains the useful recipe signal. Avoid the slowest and most
        // failure-prone path (Reel download + Files API upload) unless the caption is insufficient.
        if (shouldPreferCaptionOnly(caption)) {
            return@withContext VideoResolutionResult(
                videoFile = null,
                caption = caption,
                creator = profileName(preferredProfileUrl),
                videoUrl = null,
                sourceUrl = cleanUrl
            )
        }

        val sessionCookies = InstagramSessionManager.getCookies(context)
        val shortcode = extractShortcode(cleanUrl)
        if (!sessionCookies.isNullOrBlank() && !shortcode.isNullOrBlank()) {
            onStatusUpdate("Fetching Reel through your Instagram session...")
            resolveAuthenticatedMedia(context, cleanUrl, shortcode, sessionCookies, onStatusUpdate)?.let { authResult ->
                return@withContext authResult.copy(caption = caption ?: authResult.caption)
            }
        }

        val resolver = customResolverUrl?.trim()?.trimEnd('/')?.toHttpUrlOrNull()?.takeIf(HttpUrl::isHttps)
        if (resolver != null) {
            onStatusUpdate("Requesting video from your configured resolver...")
            try {
                resolvedVideoUrl = queryConfiguredResolver(resolver, cleanUrl, preferredProfileUrl)
                if (resolvedVideoUrl != null) {
                    onStatusUpdate("Downloading the resolved video...")
                    downloadVideoToCache(context, resolvedVideoUrl)?.let { video ->
                        return@withContext VideoResolutionResult(video, caption, null, resolvedVideoUrl, cleanUrl)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onStatusUpdate("The configured resolver was unavailable; checking the public caption...")
            }
        }

        onStatusUpdate("Checking the public post caption...")
        try {
            fetchInstagramPageInfo(cleanUrl).let { pageInfo ->
                if (caption.isNullOrBlank()) caption = pageInfo.caption
                creator = profileName(preferredProfileUrl) ?: pageInfo.creator
                if (shouldPreferCaptionOnly(caption)) {
                    return@withContext VideoResolutionResult(
                        videoFile = null,
                        caption = caption,
                        creator = creator,
                        videoUrl = pageInfo.videoUrl,
                        sourceUrl = cleanUrl
                    )
                }
                if (resolvedVideoUrl == null && pageInfo.videoUrl != null) {
                    onStatusUpdate("Downloading the public Reel video…")
                    resolvedVideoUrl = pageInfo.videoUrl
                    downloadVideoToCache(context, pageInfo.videoUrl)?.let { video ->
                        return@withContext VideoResolutionResult(video, caption, creator, pageInfo.videoUrl, cleanUrl)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Public metadata is optional; the user can attach a video or paste a caption.
        }

        VideoResolutionResult(null, caption, creator ?: profileName(preferredProfileUrl), resolvedVideoUrl, cleanUrl)
    }

    private suspend fun resolveAuthenticatedMedia(
        context: Context,
        cleanUrl: String,
        shortcode: String,
        cookies: String,
        onStatusUpdate: (String) -> Unit
    ): VideoResolutionResult? {
        val mediaId = shortcodeToMediaId(shortcode)
        val csrf = InstagramSessionManager.getCsrfToken(context).orEmpty()
        val request = Request.Builder()
            .url("https://i.instagram.com/api/v1/media/$mediaId/info/")
            .header("User-Agent", "Instagram 275.0.0.27.98 Android")
            .header("Cookie", cookies)
            .header("X-IG-App-ID", "936619743392459")
            .apply { if (csrf.isNotBlank()) header("X-CSRFToken", csrf) }
            .build()

        return runCatching {
            metadataClient.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful || response.body.contentLength() > MAX_METADATA_BYTES) return@use null
                val item = JSONObject(response.body.readTextLimited(MAX_METADATA_BYTES))
                    .optJSONArray("items")?.optJSONObject(0) ?: return@use null
                val caption = item.optJSONObject("caption")?.optString("text")?.takeIf(String::isNotBlank)
                val creator = item.optJSONObject("user")?.optString("username")?.takeIf(String::isNotBlank)
                val videoUrl = item.optJSONArray("video_versions")?.optJSONObject(0)?.optString("url")
                    ?.takeIf(String::isNotBlank)
                if (shouldPreferCaptionOnly(caption)) {
                    return@use VideoResolutionResult(null, caption, creator, videoUrl, cleanUrl)
                }
                if (videoUrl == null) return@use null
                onStatusUpdate("Downloading Reel video for recipe analysis...")
                downloadVideoToCache(context, videoUrl)?.let { file ->
                    VideoResolutionResult(file, caption, creator, videoUrl, cleanUrl)
                }
            }
        }.getOrNull()
    }

    private suspend fun queryConfiguredResolver(endpoint: HttpUrl, targetUrl: String, profileUrl: String?): String? {
        val request = configuredResolverRequest(endpoint, targetUrl, profileUrl)

        return metadataClient.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful || response.body.contentLength() > MAX_METADATA_BYTES) return null
            val json = JSONObject(response.body.readTextLimited(MAX_METADATA_BYTES))
            when {
                json.has("url") -> json.optString("url").takeIf(String::isNotBlank)
                json.has("picker") -> json.optJSONArray("picker")
                    ?.takeIf { it.length() > 0 }?.optJSONObject(0)?.optString("url")
                    ?.takeIf(String::isNotBlank)
                else -> null
            }
        }
    }

    internal fun configuredResolverRequest(
        endpoint: HttpUrl,
        targetUrl: String,
        profileUrl: String?
    ): Request {
        val payload = ConfiguredResolverRequestDto(
            url = targetUrl,
            profileUrl = profileUrl?.takeIf { it.isNotBlank() }
        )
        return Request.Builder()
            .url(endpoint)
            .header("Accept", "application/json")
            .post(GeminiJson.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private suspend fun downloadVideoToCache(context: Context, initialUrl: String): File? {
        var currentUrl = initialUrl.toHttpUrlOrNull()?.takeIf(HttpUrl::isHttps) ?: return null
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val response = mediaClient.newCall(
                Request.Builder().url(currentUrl).header("User-Agent", USER_AGENT).build()
            ).awaitResponse()

            if (response.isRedirect) {
                val nextUrl = response.header("Location")?.let(currentUrl::resolve)
                response.close()
                currentUrl = nextUrl?.takeIf(HttpUrl::isHttps) ?: return null
                if (redirectCount == MAX_REDIRECTS) return null
                return@repeat
            }

            return response.use { finalResponse ->
                if (!finalResponse.isSuccessful) return@use null
                val body = finalResponse.body
                val contentType = body.contentType()
                val contentTypeName = contentType?.type
                val pathLooksLikeMp4 = currentUrl.encodedPath.endsWith(".mp4", ignoreCase = true)
                val isVideo = contentTypeName == "video" ||
                    (pathLooksLikeMp4 && (contentTypeName == null || contentTypeName == "application" || contentTypeName == "binary"))
                if (!isVideo) return@use null
                if (body.contentLength() > MAX_VIDEO_BYTES) return@use null

                val cacheDirectory = File(context.cacheDir, CACHE_DIRECTORY)
                if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) return@use null
                if (cacheDirectory.usableSpace < REQUIRED_FREE_SPACE_BYTES) return@use null

                val temporaryFile = File(cacheDirectory, "reel_${System.currentTimeMillis()}.mp4")
                var complete = false
                try {
                    FileOutputStream(temporaryFile).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var totalBytes = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                if (totalBytes > MAX_VIDEO_BYTES) return@use null
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    complete = temporaryFile.length() > MIN_VIDEO_BYTES
                    temporaryFile.takeIf { complete }
                } finally {
                    if (!complete) temporaryFile.delete()
                }
            }
        }
        return null
    }

    private suspend fun fetchInstagramPageInfo(instagramUrl: String): InstagramPageInfo {
        var currentUrl = instagramUrl.toHttpUrlOrNull() ?: return InstagramPageInfo(null, null, null)
        repeat(MAX_PAGE_REDIRECTS + 1) { redirectCount ->
            val response = metadataClient.newCall(
                Request.Builder().url(currentUrl).header("User-Agent", USER_AGENT).build()
            ).awaitResponse()
            if (response.isRedirect) {
                val next = response.header("Location")?.let(currentUrl::resolve)
                response.close()
                currentUrl = next?.takeIf(HttpUrl::isHttps) ?: return InstagramPageInfo(null, null, null)
                if (redirectCount == MAX_PAGE_REDIRECTS) return InstagramPageInfo(null, null, null)
                return@repeat
            }
            return response.use { finalResponse ->
                if (!finalResponse.isSuccessful || finalResponse.body.contentLength() > MAX_METADATA_BYTES) {
                    return@use InstagramPageInfo(null, null, null)
                }
                val html = finalResponse.body.readTextLimited(MAX_METADATA_BYTES)
                InstagramPageInfo(
                    caption = extractMetaContent(html, "og:description") ?: extractMetaContent(html, "description"),
                    creator = extractMetaContent(html, "og:title"),
                    videoUrl = extractMetaContent(html, "og:video:secure_url")
                        ?: extractMetaContent(html, "og:video")
                )
            }
        }
        return InstagramPageInfo(null, null, null)
    }

    private fun extractMetaContent(html: String, key: String): String? = META_TAG
        .findAll(html)
        .map { it.value }
        .firstNotNullOfOrNull { tag ->
            val name = META_NAME.find(tag)?.groupValues?.getOrNull(1)
                ?: META_PROPERTY.find(tag)?.groupValues?.getOrNull(1)
            if (!name.equals(key, ignoreCase = true)) null
            else META_CONTENT.find(tag)?.groupValues?.getOrNull(1)?.decodeHtml()
        }

    private fun profileName(profileUrl: String?): String? = profileUrl
        ?.trimEnd('/')
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() && !it.contains('.') }

    private fun ResponseBody.readTextLimited(maxBytes: Long): String {
        val output = StringBuilder()
        val buffer = CharArray(4_096)
        var estimatedBytes = 0L
        charStream().use { reader ->
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                estimatedBytes += count * 4L
                require(estimatedBytes <= maxBytes) { "Remote metadata exceeded the allowed size." }
                output.append(buffer, 0, count)
            }
        }
        return output.toString()
    }

    private fun String.decodeHtml(): String = replace("&amp;", "&")
        .replace("&quot;", "\"").replace("&#39;", "'").trim()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val META_TAG = Regex("<meta\\s+[^>]*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val META_NAME = Regex("(?:name|property)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", RegexOption.IGNORE_CASE)
    private val META_PROPERTY = Regex("property\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", RegexOption.IGNORE_CASE)
    private val META_CONTENT = Regex("content\\s*=\\s*[\\\"'](.*?)[\\\"']", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private const val USER_AGENT = "InstaRecipe/0.3 (Android)"
    private const val CACHE_DIRECTORY = "shared_reels"
    private const val MAX_REDIRECTS = 4
    private const val MAX_PAGE_REDIRECTS = 4
    private const val MIN_VIDEO_BYTES = 1_024L
    private const val MAX_VIDEO_BYTES = 200L * 1024L * 1024L
    private const val MAX_METADATA_BYTES = 1L * 1024L * 1024L
    private const val REQUIRED_FREE_SPACE_BYTES = MAX_VIDEO_BYTES + 20L * 1024L * 1024L
}

private fun InetAddress.isUnsafeForRemoteMedia(): Boolean {
    val firstByte = address.firstOrNull()?.toInt()?.and(0xff) ?: return true
    val isIpv6UniqueLocal = address.size == 16 && firstByte and 0xfe == 0xfc
    return isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress ||
        isMulticastAddress || isIpv6UniqueLocal
}
