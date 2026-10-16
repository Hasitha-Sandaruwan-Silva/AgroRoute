package com.agroroute.features.participants

import com.agroroute.core.model.UserRole

data class ParticipantIdentity(
    val id: String, val name: String, val role: UserRole,
    val imageUrl: String? = null, val memberSince: Long? = null, val points: Long? = null
)
data class ParticipantReview(val id: String, val rating: Int, val text: String,
    val date: Long?, val reviewerName: String)
data class ParticipantProfile(val identity: ParticipantIdentity, val reviews: List<ParticipantReview>,
    val activeHarvests: List<String>?, val phone: String? = null) {
    val averageRating: Double? get() = reviews.takeIf { it.isNotEmpty() }?.map { it.rating }?.average()
    fun ratingCount(stars: Int) = reviews.count { it.rating == stars }
}
data class Conversation(val id: String, val other: ParticipantIdentity, val lastMessage: String, val updatedAt: Long?)
data class ChatMessage(val id: String, val senderId: String, val text: String, val sentAt: Long?)

/** Role ordering and length prefixes make the pair unambiguous, including UIDs containing underscores. */
fun conversationIdentity(farmerId: String, supplierId: String): String {
    require(farmerId.isNotBlank() && supplierId.isNotBlank() && farmerId != supplierId)
    require(listOf(farmerId, supplierId).all { '/' !in it && it.length <= 128 })
    return "${farmerId.length}:$farmerId${supplierId.length}:$supplierId"
}
fun messageText(input: String): String = input.trim().also { require(it.isNotBlank() && it.length <= 4000) }
