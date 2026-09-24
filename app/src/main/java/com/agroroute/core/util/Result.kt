package com.agroroute.core.util

/**
 * A sealed wrapper representing the three states of any async operation:
 * Loading, Success, or Error — used consistently across all repository and use-case returns.
 */
sealed class Result<out T> {

    /** Indicates an operation is in progress. */
    data object Loading : Result<Nothing>()

    /** Carries the successful result payload. */
    data class Success<T>(val data: T) : Result<T>()

    /** Carries the error that occurred, optionally wrapping the original [Throwable]. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : Result<Nothing>()

    // ── Convenience helpers ────────────────────────────────────────────────────

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError   get() = this is Error

    /** Returns the data if this is [Success], otherwise null. */
    fun getOrNull(): T? = if (this is Success) data else null

    /** Returns the data if this is [Success], otherwise throws. */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error   -> throw cause ?: RuntimeException(message)
        is Loading -> throw IllegalStateException("Result is still Loading")
    }
}

/** Wraps a suspending [block] in a try/catch, returning [Result.Success] or [Result.Error]. */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e.message ?: "An unexpected error occurred", e)
}
