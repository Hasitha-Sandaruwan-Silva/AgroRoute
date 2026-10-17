package com.agroroute.features.supplier.map

import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus

enum class SupplierMapFilter { All, Pickup, Delivery }

/** UI projection only. Ownership is checked again before exposing cached orders. */
internal fun supplierMapOrders(orders: List<Order>, supplierId: String, filter: SupplierMapFilter = SupplierMapFilter.All): List<Order> =
    orders.filter { order ->
        supplierId.isNotBlank() && order.supplierId == supplierId &&
            order.status in listOf(OrderStatus.AWAITING_PICKUP, OrderStatus.IN_TRANSIT) &&
            (filter == SupplierMapFilter.All ||
                (filter == SupplierMapFilter.Pickup && order.status == OrderStatus.AWAITING_PICKUP) ||
                (filter == SupplierMapFilter.Delivery && order.status == OrderStatus.IN_TRANSIT))
    }
