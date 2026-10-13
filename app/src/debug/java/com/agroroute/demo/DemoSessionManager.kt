package com.agroroute.demo

import com.agroroute.core.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DemoSession(val enabled: Boolean = false, val selectedRole: UserRole? = null,
    val selectedDemoUserId: String? = null)
class DemoSessionManager {
    private val mutable = MutableStateFlow(DemoSession())
    val state = mutable.asStateFlow()
    fun enter() { mutable.value = DemoSession(enabled = true) }
    fun select(role: UserRole, userId: String = if (role == UserRole.FARMER) "demo_farmer_001" else "demo_supplier_001") {
        require(userId in if (role == UserRole.FARMER) listOf("demo_farmer_001","demo_farmer_002","demo_farmer_003")
            else listOf("demo_supplier_001","demo_supplier_002","demo_supplier_003"))
        mutable.value = DemoSession(true,role,userId)
    }
    fun exit() { mutable.value = DemoSession() }
}
