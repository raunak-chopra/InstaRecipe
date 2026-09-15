package com.instarecipe.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal object VideoFileStore {
    private const val MAX_VIDEO_BYTES = 200L * 1024L * 1024L
    private const val REQUIRED_FREE_SPACE_BYTES = MAX_VIDEO_BYTES + 20L * 1024L * 1024L

    suspend fun copyToCache(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
        require(mimeType?.startsWith("video/") == true) { "The selected content is not a supported video." }

        val cacheDirectory = File(context.cacheDir, "shared_reels")
        require((cacheDirectory.exists() || cacheDirectory.mkdirs()) && cacheDirectory.usableSpace >= REQUIRED_FREE_SPACE_BYTES) {
            "There is not enough temporary storage to process this video."
        }

        val file = File(cacheDirectory, "shared_video_${System.currentTimeMillis()}.mp4")
        var complete = false
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("The selected video could not be opened.")
            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MAX_VIDEO_BYTES) { "Video is larger than the 200 MB limit." }
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(file.length() > 0) { "The selected video is empty." }
            complete = true
            file
        } finally {
            if (!complete) file.delete()
        }
    }
}
