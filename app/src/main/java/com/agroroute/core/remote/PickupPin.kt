package com.agroroute.core.remote

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Six-digit demo verifier. A modified participant client can bypass local comparison. */
object PickupPin {
    const val ITERATIONS = 120_000
    private val random = SecureRandom()
    data class Generated(val code: String, val salt: String, val verifier: String)
    fun generate(): Generated {
        val code = random.nextInt(1_000_000).toString().padStart(6, '0')
        val salt = ByteArray(16).also(random::nextBytes).hex()
        return Generated(code, salt, derive(code, salt))
    }
    fun matches(code: String, salt: String, verifier: String): Boolean =
        code.matches(Regex("[0-9]{6}")) && MessageDigest.isEqual(
            derive(code, salt).toByteArray(Charsets.US_ASCII), verifier.toByteArray(Charsets.US_ASCII))
    private fun derive(code: String, salt: String): String {
        require(salt.matches(Regex("[0-9a-f]{32}"))) { "Pickup verification is unavailable. Ask the Supplier to generate a new code." }
        val bytes = salt.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(code.toCharArray(), bytes, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.hex() }
        finally { spec.clearPassword() }
    }
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 255) }
}
