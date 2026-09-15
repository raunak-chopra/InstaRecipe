package com.instarecipe.app

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

object InstagramResolver {
    private val publicOnlyDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            Dns.SYSTEM.lookup(hostname).also { addresses ->
                require(addresses.isNotEmpty() && addresses.none(InetAddress::isUnsafeForRemoteMedia)) {
                    "Resolver returned an unsafe network destination."
                }
            }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .dns(publicOnlyDns)
        .build()

    private val instagramUrlRegex = Pattern.compile(
        """https://(?:www\.)?instagram\.com/(?:reel|reels|p|tv|share/reel)/([A-Za-z0-9_-]+)""",
        Pattern.CASE_INSENSITIVE
    )

    fun extractShortcode(instagramUrl: String): String? {
        val matcher = instagramUrlRegex.matcher(instagramUrl)
        return if (matcher.find()) matcher.group(1) else null
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
        val url = extractedUrl ?: extractInstagramUrl(sharedText)
        val cleaned = sharedText.replace(url.orEmpty(), "")
            .lines().map(String::trim).filter(String::isNotBlank).joinToString("\n").trim()
        return cleaned.takeIf(GeminiRecipeExtractor::hasSubstantiveRecipeContent)
    }

    suspend fun resolveAndDownload(
        context: Context,
        instagramUrl: String,
        customResolverUrl: String? = null,
        supplementaryText: String? = null,
        onStatusUpdate: (String) -> Unit = {}
    ): VideoResolutionResult = withContext(Dispatchers.IO) {
        val cleanUrl = extractInstagramUrl(instagramUrl)
            ?: throw IllegalArgumentException("Enter a valid HTTPS Instagram post or reel link.")
        var caption = extractCaptionFromSharedText(supplementaryText.orEmpty(), cleanUrl)
        var creator: String? = null
        var resolvedVideoUrl: String? = null

        val resolver = customResolverUrl?.trim()?.trimEnd('/')?.toHttpUrlOrNull()?.takeIf(HttpUrl::isHttps)
        if (resolver != null) {
            onStatusUpdate("Requesting video from your configured resolver...")
            try {
                resolvedVideoUrl = queryConfiguredResolver(resolver, cleanUrl)
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
                if (caption.isNullOrBlank()) caption = pageInfo.first
                creator = pageInfo.second
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Public metadata is optional; the user can attach a video or paste a caption.
        }

        VideoResolutionResult(null, caption, creator, resolvedVideoUrl, cleanUrl)
    }

    private suspend fun queryConfiguredResolver(endpoint: HttpUrl, targetUrl: String): String? {
        val payload = JSONObject().apply {
            put("url", targetUrl)
            put("videoQuality", "720")
            put("downloadMode", "auto")
        }
        val request = Request.Builder()
            .url(endpoint.newBuilder().encodedPath("/").build())
            .header("Accept", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return client.newCall(request).awaitResponse().use { response ->
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

    private suspend fun downloadVideoToCache(context: Context, initialUrl: String): File? {
        var currentUrl = initialUrl.toHttpUrlOrNull()?.takeIf(HttpUrl::isHttps) ?: return null
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val response = client.newCall(
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
                if (body.contentType()?.type != "video") return@use null
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

    private suspend fun fetchInstagramPageInfo(instagramUrl: String): Pair<String?, String?> {
        val request = Request.Builder().url(instagramUrl).header("User-Agent", USER_AGENT).build()
        return client.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful || response.body.contentLength() > MAX_METADATA_BYTES) {
                return Pair(null, null)
            }
            val html = response.body.readTextLimited(MAX_METADATA_BYTES)
            val description = META_DESCRIPTION.find(html)?.groupValues?.getOrNull(1)?.decodeHtml()
            val title = META_TITLE.find(html)?.groupValues?.getOrNull(1)?.decodeHtml()
            Pair(description, title)
        }
    }

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
    private val META_DESCRIPTION = Regex(
        """<meta\s+(?:property|name)=["'](?:og:description|description)["']\s+content=["'](.*?)["']""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val META_TITLE = Regex(
        """<meta\s+(?:property|name)=["']og:title["']\s+content=["'](.*?)["']""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private const val USER_AGENT = "InstaRecipe/0.3 (Android)"
    private const val CACHE_DIRECTORY = "shared_reels"
    private const val MAX_REDIRECTS = 4
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
