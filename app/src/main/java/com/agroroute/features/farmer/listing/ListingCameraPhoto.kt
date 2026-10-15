package com.agroroute.features.farmer.listing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.agroroute.core.util.CloudinaryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Input preparation only. Cloudinary remains the sole listing upload implementation. */
internal object ListingCameraPhoto {
    fun create(context: Context): String {
        val directory = File(context.filesDir, "listing_captures").apply { check(isDirectory || mkdirs()) }
        return File.createTempFile("harvest_", ".jpg", directory).name
    }

    private fun file(context: Context, name: String): File {
        require(name == File(name).name && name.startsWith("harvest_") && name.endsWith(".jpg"))
        return File(context.filesDir, "listing_captures/$name")
    }

    fun uri(context: Context, name: String): Uri = FileProvider.getUriForFile(
        context, "${context.packageName}.listing.photos", file(context, name))

    fun discard(context: Context, name: String) { runCatching { file(context, name).delete() } }

    suspend fun prepare(context: Context, name: String): Uri = withContext(Dispatchers.IO) {
        val source = file(context, name)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val decoded = requireNotNull(BitmapFactory.decodeFile(source.path,
            BitmapFactory.Options().apply { inSampleSize = sample }))
        var oriented: Bitmap? = null
        var scaled: Bitmap? = null
        val output = File.createTempFile("prepared_", ".jpg", source.parentFile)
        try {
            val orientation = ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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
            oriented = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0,
                decoded.width, decoded.height, matrix, true)
            val ratio = minOf(1f, 2048f / maxOf(oriented.width, oriented.height))
            scaled = if (ratio == 1f) oriented else Bitmap.createScaledBitmap(oriented,
                (oriented.width * ratio).toInt().coerceAtLeast(1), (oriented.height * ratio).toInt().coerceAtLeast(1), true)
            output.outputStream().use { check(scaled.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            check(output.length() in 1..CloudinaryConfig.MAX_PHOTO_BYTES)
            check(output.renameTo(source))
            uri(context, name)
        } finally {
            output.delete()
            if (scaled !== oriented && scaled !== decoded) scaled?.recycle()
            if (oriented !== decoded) oriented?.recycle()
            decoded.recycle()
        }
    }
}
