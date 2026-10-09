package com.agroroute.core.remote

import android.net.Uri
import com.agroroute.core.util.CloudinaryConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePhotoStorage @Inject constructor(
    private val cloudinary: CloudinaryPhotoStorage,
    private val firebase: StorageService
) {
    suspend fun upload(uri: Uri, userId: String): String =
        if (CloudinaryConfig.PROFILE_UNSIGNED_PRESET.isNotBlank()) {
            cloudinary.uploadPhoto(uri, "profile_photos/$userId", CloudinaryConfig.PROFILE_UNSIGNED_PRESET)
        } else {
            firebase.uploadImage(uri, "profile_images/$userId/avatar")
        }
}
