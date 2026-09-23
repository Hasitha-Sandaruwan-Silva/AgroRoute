package com.agroroute.data.repository

import com.agroroute.data.model.UserProfile
import com.agroroute.data.model.UserRole
import com.agroroute.domain.repository.AuthRepository
import com.agroroute.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthRepository @Inject constructor() : AuthRepository {

    private val _currentUserState = MutableStateFlow<UserProfile?>(null)

    override val isLoggedIn: Boolean
        get() = _currentUserState.value != null

    override val currentUserId: String?
        get() = _currentUserState.value?.userId

    override fun getCurrentUserProfile(): Flow<Result<UserProfile?>> {
        return _currentUserState.asStateFlow().map { user ->
            Result.Success(user)
        }
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()

        val mockUser = when {
            // 1. FARMER LOGIN
            cleanEmail.contains("fam") -> {
                UserProfile(
                    userId = "farmer_123",
                    displayName = "Agro Farmer",
                    email = "fam@gmail.com",
                    phone = "0771112233",
                    role = UserRole.FARMER,
                    profileImageUrl = null,
                    location = "Dambulla, Sri Lanka",
                    createdAt = System.currentTimeMillis()
                )
            }
            // 2. SUPPLIER LOGIN
            cleanEmail.contains("sup") -> {
                UserProfile(
                    userId = "supplier_456",
                    displayName = "Agro Supplier",
                    email = "sup@gmail.com",
                    phone = "0774445566",
                    role = UserRole.SUPPLIER,
                    profileImageUrl = null,
                    location = "Colombo, Sri Lanka",
                    createdAt = System.currentTimeMillis()
                )
            }
            // 3. ANY OTHER EMAIL (Default to FARMER)
            else -> {
                UserProfile(
                    userId = "mock_${System.currentTimeMillis()}",
                    displayName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = cleanEmail,
                    phone = "0771234567",
                    role = UserRole.FARMER,
                    profileImageUrl = null,
                    location = "Sri Lanka",
                    createdAt = System.currentTimeMillis()
                )
            }
        }

        _currentUserState.value = mockUser
        return Result.Success(mockUser)
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<UserProfile> {
        val newUser = UserProfile(
            userId = "mock_${System.currentTimeMillis()}",
            displayName = displayName.ifBlank { "New User" },
            email = email.ifBlank { "newuser@agroroute.com" },
            phone = "0771234567",
            role = role,
            profileImageUrl = null,
            location = "Sri Lanka",
            createdAt = System.currentTimeMillis()
        )

        _currentUserState.value = newUser
        return Result.Success(newUser)
    }

    override suspend fun logout(): Result<Unit> {
        _currentUserState.value = null
        return Result.Success(Unit)
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        _currentUserState.value = profile
        return Result.Success(Unit)
    }
}