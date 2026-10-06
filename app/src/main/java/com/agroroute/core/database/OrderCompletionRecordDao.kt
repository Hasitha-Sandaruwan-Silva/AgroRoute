package com.agroroute.core.database

import androidx.room.Dao
import androidx.room.Query
import com.agroroute.core.model.OrderCompletionRecord
import kotlinx.coroutines.flow.Flow

/**
 * DAO for order completion records (ensures idempotent point awarding).
 */
@Dao
interface OrderCompletionRecordDao {

    @Query("SELECT * FROM order_completion_records WHERE orderId = :orderId")
    fun getRecordByOrderId(orderId: String): Flow<OrderCompletionRecord?>

    @Query("SELECT * FROM order_completion_records WHERE orderId = :orderId")
    suspend fun getRecordByOrderIdSync(orderId: String): OrderCompletionRecord?

    @Query("SELECT * FROM order_completion_records WHERE farmerId = :farmerId")
    fun getRecordsByFarmerId(farmerId: String): Flow<List<OrderCompletionRecord>>

    @Query("SELECT * FROM order_completion_records WHERE supplierId = :supplierId")
    fun getRecordsBySupplierId(supplierId: String): Flow<List<OrderCompletionRecord>>

    @androidx.room.Upsert
    suspend fun upsertRecord(record: OrderCompletionRecord)
}
