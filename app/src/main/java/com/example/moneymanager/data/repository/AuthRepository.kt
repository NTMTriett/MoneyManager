package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isUserAuthenticated: Flow<Boolean>
    
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): Result<User>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    
    // Profile management methods
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit>
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun updateProfilePhoto(photoUrl: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}