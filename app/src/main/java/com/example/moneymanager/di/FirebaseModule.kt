package com.example.moneymanager.di

import android.content.Context
import com.example.moneymanager.data.repository.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val gso = try {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.example.moneymanager.R.string.default_web_client_id))
                .requestEmail()
                .build()
        } catch (e: Exception) {
            // Fallback configuration when google-services.json is not available
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        }
        return GoogleSignIn.getClient(context, gso)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository {
        return FirebaseAuthRepository(auth)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): TransactionRepository {
        return FirebaseTransactionRepository(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): CategoryRepository {
        return FirebaseCategoryRepository(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): BudgetRepository {
        return FirebaseBudgetRepository(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideSavingsGoalRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): SavingsGoalRepository {
        return FirebaseSavingsGoalRepository(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideRecurringTransactionRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): RecurringTransactionRepository {
        return FirebaseRecurringTransactionRepository(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideDebtLoanRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): DebtLoanRepository {
        return FirebaseDebtLoanRepository(firestore, auth)
    }
}
