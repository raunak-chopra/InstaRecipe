package com.instarecipe.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Read only the selected image; keep requests and decoded bitmap memory bounded. */
internal object RecipeScreenshot {
    suspend fun read(context: Context, uri: Uri): GeminiInlineDataDto = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Choose a screenshot from your device." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 10 * 1024 * 1024) { "Choose a screenshot smaller than 10 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Couldn't open that screenshot. Choose it again.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0 &&
            bounds.outWidth.toLong() * bounds.outHeight <= 40_000_000L) { "Choose a valid PNG, JPEG or WebP screenshot." }
        require(bounds.outMimeType in setOf("image/png", "image/jpeg", "image/webp")) { "Choose a PNG, JPEG or WebP screenshot." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2560) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Couldn't read that screenshot. Choose another image.")
        try {
            val encoded = ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) { "Couldn't prepare the screenshot." }
                output.toByteArray()
            }
            require(encoded.size <= 8 * 1024 * 1024) { "Choose a smaller screenshot." }
            GeminiInlineDataDto("image/jpeg", Base64.getEncoder().encodeToString(encoded))
        } finally { bitmap.recycle() }
    }
}
