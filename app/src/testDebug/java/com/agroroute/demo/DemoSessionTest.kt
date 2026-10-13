package com.agroroute.demo
import com.agroroute.core.model.UserRole
import org.junit.Assert.*
import org.junit.Test
class DemoSessionTest {
    @Test fun startsDisabledWithNoIdentity() { assertEquals(DemoSession(),DemoSessionManager().state.value) }
    @Test fun entryRequiresRoleSelection() { val s=DemoSessionManager(); s.enter(); assertTrue(s.state.value.enabled); assertNull(s.state.value.selectedDemoUserId) }
    @Test fun farmerDefaultIdentity() { val s=DemoSessionManager(); s.select(UserRole.FARMER); assertEquals("demo_farmer_001",s.state.value.selectedDemoUserId); assertEquals(UserRole.FARMER,s.state.value.selectedRole) }
    @Test fun supplierDefaultIdentity() { val s=DemoSessionManager(); s.select(UserRole.SUPPLIER); assertEquals("demo_supplier_001",s.state.value.selectedDemoUserId); assertEquals(UserRole.SUPPLIER,s.state.value.selectedRole) }
    @Test fun bothRoleSwitchesReplaceIdentity() { val s=DemoSessionManager(); s.select(UserRole.FARMER); s.select(UserRole.SUPPLIER); assertEquals("demo_supplier_001",s.state.value.selectedDemoUserId); s.select(UserRole.FARMER); assertEquals("demo_farmer_001",s.state.value.selectedDemoUserId) }
    @Test fun exitClearsOnlyLocalSessionState() { val s=DemoSessionManager(); s.select(UserRole.SUPPLIER); s.exit(); assertEquals(DemoSession(),s.state.value) }
    @Test(expected=IllegalArgumentException::class) fun rejectsRealIdentity() { DemoSessionManager().select(UserRole.FARMER,"real-user") }
    @Test(expected=IllegalArgumentException::class) fun rejectsRoleMismatch() { DemoSessionManager().select(UserRole.FARMER,"demo_supplier_001") }
}
