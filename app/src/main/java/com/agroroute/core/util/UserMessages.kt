package com.agroroute.core.util

/** Display wording only; original exceptions stay attached to Result.Error and in existing logs. */
internal const val GENERIC_ACTION_ERROR = "Couldn't complete this action. Please try again."
internal const val PHOTO_UPLOAD_ERROR = "Couldn't upload the photo. Check your connection and try again."

private val technicalDetails = Regex(
    """(?i)cloudinary|firebase|firestore|\broom\b|repository|exception|\bhttp\b|\bSDK\b|\bAPI\b|\bdatabase\b|\bdocument\b|\bcollection (?:path|reference|name)\b|\btransaction\b|\b(?:java|javax|android|kotlin|okhttp|io\.grpc)\.|https?://|content://|file://|\bcode\s*[:=]|\bstatus\s*\{|\bgrpc\b|\b(?:SQL|JSON|UID|null)\b|(?-i:\b[A-Z]+(?:_[A-Z]+)+\b)|\bat \S+\([^)]*:\d+\)"""
)

internal fun userFacingError(message: String?, fallback: String = GENERIC_ACTION_ERROR): String = when {
    message.isNullOrBlank() -> fallback
    message.contains("NOT_FOUND", true) -> "This item is unavailable. Refresh and try again."
    message.contains("PERMISSION_DENIED", true) || message.contains("insufficient permissions", true) ->
        "This action is not available for your account or the current item state. Refresh and try again."
    message.contains("UNAVAILABLE:", true) || message.contains("code=UNAVAILABLE", true) ||
        message.contains("network error", true) || message.contains("timed out", true) ||
        message.contains("timeout", true) || message.contains("Unable to resolve host", true) ->
        "Could not connect. Check your connection and try again."
    message == "Check failed." || message == "Failed requirement." -> fallback
    message.length > 400 || technicalDetails.containsMatchIn(message) -> fallback
    else -> message
}

internal fun userFacingThrowable(error: Throwable, fallback: String = GENERIC_ACTION_ERROR): String = when (error) {
    is java.net.UnknownHostException, is java.net.SocketException, is java.net.SocketTimeoutException -> "Could not connect. Check your connection and try again."
    is IllegalArgumentException, is IllegalStateException -> {
        // Only app-authored validation is suitable for display. SDK diagnostics stay private.
        if (error.stackTrace.firstOrNull()?.className?.startsWith("com.agroroute.") == true)
            userFacingError(error.message, fallback) else fallback
    }
    else -> fallback
}

internal fun photoUploadMessage(cause: Throwable): String {
    // Keep useful photo validation without exposing arbitrary provider response bodies.
    val detail = cause.message.orEmpty()
    return when {
        detail.startsWith("The selected file is not a supported image type.") ->
            "Choose a JPEG, PNG, or WebP photo."
        detail.startsWith("Photo is too large") -> "Photo is too large. Please choose a smaller photo."
        detail == "Could not open the selected image." -> "Couldn't open the photo. Please select it again."
        else -> PHOTO_UPLOAD_ERROR
    }
}
