package com.agroroute.core.remote

import android.content.Context
import android.net.Uri
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.repository.ListingPhotoStorageException
import com.agroroute.core.util.CloudinaryConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [ListingPhotoStorage] implementation that uploads listing photos to Cloudinary
 * using an **unsigned upload preset** — no API secret is ever embedded in the app.
 *
 * Upload flow:
 *  1. Resolve the Android `content://` URI and read its bytes via [ContentResolver].
 *  2. Validate MIME type and file size against [CloudinaryConfig] limits.
 *  3. POST a multipart form-data request to the Cloudinary unsigned upload endpoint.
 *  4. Parse the JSON response and return the `secure_url` string.
 *
 * On any failure a [ListingPhotoStorageException] is thrown so the caller can
 * present a useful error and abort saving the listing with a local URI.
 *
 * Firebase Storage is not touched by this class.
 */
@Singleton
class CloudinaryPhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : ListingPhotoStorage {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // ── ListingPhotoStorage ────────────────────────────────────────────────────

    override suspend fun uploadListingPhoto(
        localUri: String,
        farmerId: String,
        listingId: String
    ): String = uploadPhoto(Uri.parse(localUri), "listing_photos/$farmerId/$listingId", CloudinaryConfig.UNSIGNED_PRESET)

    suspend fun uploadPhoto(uri: Uri, prefix: String, preset: String): String = withContext(Dispatchers.IO) {
        require(CloudinaryConfig.CLOUD_NAME.isNotBlank() && preset.isNotBlank()) {
            "Configure the Cloudinary cloud name and unsigned upload preset first."
        }


        // 1. Validate MIME type
        val mimeType = context.contentResolver.getType(uri)
            ?.lowercase()
            ?.takeIf { it in CloudinaryConfig.ALLOWED_MIME_TYPES }
            ?: throw ListingPhotoStorageException(
                ListingPhotoStorageException.Stage.UPLOAD,
                IOException(
                    "The selected file is not a supported image type. " +
                        "Please choose a JPEG, PNG, or WebP image."
                )
            )

        // 2. Read bytes — respect the size limit
        val imageBytes: ByteArray = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size().toLong() <= CloudinaryConfig.MAX_PHOTO_BYTES) {
                    val count = stream.read(buffer, 0, minOf(buffer.size, (CloudinaryConfig.MAX_PHOTO_BYTES + 1 - output.size()).toInt()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                if (bytes.size > CloudinaryConfig.MAX_PHOTO_BYTES) {
                    throw IOException(
                        "Photo is too large (${bytes.size / 1_024 / 1_024} MB). " +
                            "Please choose an image under 10 MB."
                    )
                }
                bytes
            } ?: throw IOException("Could not open the selected image.")
        } catch (io: IOException) {
            throw ListingPhotoStorageException(ListingPhotoStorageException.Stage.UPLOAD, io)
        }

        // 3. Build and execute the Cloudinary unsigned multipart upload request.
        //    The public_id scopes the file to the farmer+listing so it is easy to
        //    identify, but it is not used as a secret path.
        val publicId = "$prefix/${java.util.UUID.randomUUID()}"
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", preset)
            .addFormDataPart("public_id", publicId)
            .addFormDataPart(
                "file",
                "photo.${mimeType.substringAfter("/")}",
                imageBytes.toRequestBody(mimeType.toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(CloudinaryConfig.UPLOAD_URL)
            .post(requestBody)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (io: IOException) {
            throw ListingPhotoStorageException(
                ListingPhotoStorageException.Stage.UPLOAD,
                IOException("Network error during photo upload: ${io.message}", io)
            )
        }

        response.use { response ->
            // 4. Parse the JSON response and extract secure_url
            if (!response.isSuccessful) {
                val errorBody = try { response.body?.string() } catch (_: Exception) { null }
                val cloudinaryMessage = try {
                    errorBody?.let { JSONObject(it).optJSONObject("error")?.optString("message") }
                } catch (_: Exception) { null }
                throw ListingPhotoStorageException(
                    ListingPhotoStorageException.Stage.UPLOAD,
                    IOException(
                        cloudinaryMessage
                            ?: "Cloudinary upload failed (HTTP ${response.code}). Check your connection and try again."
                    )
                )
            }

            val responseBody = try {
                response.body?.string()
                    ?: throw IOException("Empty response from photo upload service.")
            } catch (io: IOException) {
                throw ListingPhotoStorageException(ListingPhotoStorageException.Stage.DOWNLOAD_URL, io)
            }

            val secureUrl = try {
                JSONObject(responseBody).optString("secure_url").takeIf { it.isNotBlank() }
                    ?: throw IOException("Photo upload succeeded but no URL was returned.")
            } catch (io: IOException) {
                throw ListingPhotoStorageException(ListingPhotoStorageException.Stage.DOWNLOAD_URL, io)
            }

            secureUrl
        }
    }
}
