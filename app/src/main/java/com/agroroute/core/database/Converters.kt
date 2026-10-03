package com.agroroute.core.database

import androidx.room.TypeConverter
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.ListingCategory
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.OrderRequestStatus
import com.agroroute.core.model.QuantityUnit
import com.agroroute.core.model.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters that translate between Kotlin types (enums, lists) and SQLite-compatible primitives.
 */
class Converters {

    private val gson = Gson()

    // ── Enum: UserRole ─────────────────────────────────────────────────────────
    @TypeConverter fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter fun toUserRole(value: String): UserRole   = UserRole.valueOf(value)

    // ── Enum: ListingStatus ────────────────────────────────────────────────────
    @TypeConverter fun fromListingStatus(value: ListingStatus): String = value.name
    @TypeConverter fun toListingStatus(value: String): ListingStatus   = ListingStatus.valueOf(value)

    // ── Enum: ListingCategory ─────────────────────────────────────────────────
    @TypeConverter fun fromListingCategory(value: ListingCategory?): String? = value?.name
    @TypeConverter fun toListingCategory(value: String?): ListingCategory? =
        value?.let(ListingCategory::valueOf)

    // ── Enum: BidStatus ────────────────────────────────────────────────────────
    @TypeConverter fun fromBidStatus(value: BidStatus): String = value.name
    @TypeConverter fun toBidStatus(value: String): BidStatus   = BidStatus.valueOf(value)

    // ── Enum: OrderStatus ──────────────────────────────────────────────────────
    @TypeConverter fun fromOrderStatus(value: OrderStatus): String = value.name
    @TypeConverter fun toOrderStatus(value: String): OrderStatus   = OrderStatus.valueOf(value)

    // ── Enum: OrderRequestStatus ────────────────────────────────────────────────
    @TypeConverter fun fromOrderRequestStatus(value: OrderRequestStatus): String = value.name
    @TypeConverter fun toOrderRequestStatus(value: String): OrderRequestStatus   = OrderRequestStatus.valueOf(value)

    // ── Enum: QuantityUnit ─────────────────────────────────────────────────────
    @TypeConverter fun fromQuantityUnit(value: QuantityUnit): String = value.name
    @TypeConverter fun toQuantityUnit(value: String): QuantityUnit   = QuantityUnit.valueOf(value)

    // ── List<String> (e.g. imageUrls) ─────────────────────────────────────────
    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
