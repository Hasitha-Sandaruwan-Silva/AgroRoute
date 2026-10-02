package com.agroroute.core.navigation

import com.agroroute.core.model.UserRole
import com.agroroute.core.model.UserProfile
import com.agroroute.core.repository.GoogleSignInOutcome
import com.agroroute.core.repository.googleSignInOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRoleRoutingTest {

    @Test
    fun successfulProfileSignInRoutesToSavedRoleHome() {
        assertEquals(Screen.FarmerHome.route, roleHomeDestination(UserRole.FARMER))
        assertEquals(Screen.SupplierHome.route, roleHomeDestination(UserRole.SUPPLIER))
    }

    @Test
    fun googleSignInUsesLoadedProfileAndRequiresRoleWhenProfileIsMissing() {
        val profile = UserProfile(userId = "firebase-uid", role = UserRole.SUPPLIER)

        assertEquals(GoogleSignInOutcome.ProfileReady(profile), googleSignInOutcome(profile))
        assertEquals(GoogleSignInOutcome.RoleRequired, googleSignInOutcome(null))
    }

    @Test
    fun unresolvedRoleDoesNotChooseAHomeAutomatically() {
        assertNull(roleHomeDestination(null))
    }
}