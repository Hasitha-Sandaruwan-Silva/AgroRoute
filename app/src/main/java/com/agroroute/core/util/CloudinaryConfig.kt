package com.agroroute.core.util

/**
 * Public Cloudinary configuration for listing-photo uploads.
 *
 * These are **upload-only** settings, not secrets:
 *  - [CLOUD_NAME] is visible in every Cloudinary URL.
 *  - [UNSIGNED_PRESET] is an unsigned preset that grants write-only access to
 *    a restricted folder; it does NOT allow reading, transforming, or deleting
 *    files from a normal app client.
 *
 * ⚠️  Never add a Cloudinary API Secret or API Key to this file or anywhere
 *     in the Android source tree.  Unsigned presets are the correct approach
 *     for mobile clients.
 *
 * To rotate the preset: update [UNSIGNED_PRESET] here and redeploy.
 * To change the cloud:  update [CLOUD_NAME] here and update the Cloudinary
 *                       console accordingly.
 */
object CloudinaryConfig {
    /** Cloudinary cloud name — visible in every public URL, not a secret. */
    const val CLOUD_NAME = "dyjmxsmkv"

    /**
     * Unsigned upload preset created in the Cloudinary console.
     * It must be set to "Unsigned" and scoped to the listing-photos folder.
     */
    const val UNSIGNED_PRESET = "agroroute_listing_photos"

    /** Cloudinary unsigned upload endpoint. */
    val UPLOAD_URL get() = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    // ── Photo limits (keep in sync with Firebase Storage rules) ───────────────
    /** Maximum allowed image file size in bytes (10 MB). */
    const val MAX_PHOTO_BYTES = 10L * 1_024 * 1_024

    /** Allowed MIME types for listing photos. */
    val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
}

