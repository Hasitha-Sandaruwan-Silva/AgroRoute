package com.agroroute.core.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import com.agroroute.core.util.CloudinaryConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ProfilePhotoUploader {
    suspend fun upload(uri: Uri, userId: String): String
}

@Singleton
class ProfilePhotoStorage @Inject constructor(
    private val cloudinary: CloudinaryPhotoStorage,
    @ApplicationContext private val context: Context
) : ProfilePhotoUploader {
    /** Decode a bounded avatar off the main thread; Keep prepared avatars below 5 MB. */
    override suspend fun upload(uri: Uri, userId: String): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot read this photo. Please choose another image." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Cannot open the selected photo. Please select it again.")
        val orientation = resolver.openInputStream(uri)?.use {
            runCatching { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        val oriented = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val file = File.createTempFile("avatar-", ".jpg", context.cacheDir)
        try {
            file.outputStream().use { check(oriented.compress(Bitmap.CompressFormat.JPEG, 88, it)) { "Could not prepare your photo." } }
            require(file.length() in 1 until 5L * 1024 * 1024) { "Photo is too large. Please choose a smaller image." }
            val preparedUri = Uri.fromFile(file)
            // Reuse the deployed unsigned upload backend; never silently fall back to Firebase.
            cloudinary.uploadPhoto(preparedUri, "profile_photos/$userId", CloudinaryConfig.UNSIGNED_PRESET)
                .also { require(it.startsWith("https://")) { "Photo service returned an insecure URL" } }
        } finally {
            if (oriented !== bitmap) oriented.recycle()
            bitmap.recycle()
            file.delete()
        }
    }
}
