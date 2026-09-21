package com.instarecipe.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Keeps personal cook photographs in app-private storage, separate from editorial imagery. */
object CookPhotoStore {
    private const val CAPTURE_DIRECTORY = "cook-capture"
    private const val PHOTO_DIRECTORY = "cook-photos"

    fun createCaptureFile(context: Context): File {
        val directory = File(context.cacheDir, CAPTURE_DIRECTORY).apply { mkdirs() }
        return File.createTempFile("capture-", ".jpg", directory)
    }

    fun captureUri(context: Context, file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.cookphoto",
        file
    )

    fun copyFromUri(context: Context, source: Uri): String? = runCatching {
        context.contentResolver.openInputStream(source)?.use { input ->
            val directory = File(context.filesDir, PHOTO_DIRECTORY).apply { mkdirs() }
            val destination = File(directory, "cook-${UUID.randomUUID()}.jpg")
            destination.outputStream().use { output -> input.copyTo(output) }
            destination.absolutePath
        }
    }.getOrNull()

    fun copyFromFile(context: Context, source: File): String? = runCatching {
        val directory = File(context.filesDir, PHOTO_DIRECTORY).apply { mkdirs() }
        val destination = File(directory, "cook-${UUID.randomUUID()}.jpg")
        source.inputStream().use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
        destination.absolutePath
    }.getOrNull()

    fun delete(path: String) {
        if (path.isNotBlank()) File(path).delete()
    }
}

@Composable
fun PersonalCookPhoto(
    path: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val image = remember(path) {
        path.takeIf(String::isNotBlank)
            ?.let(BitmapFactory::decodeFile)
            ?.asImageBitmap()
    }
    image?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
