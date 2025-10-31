package com.example.toda.di

import com.example.toda.repository.TODARepository
import com.example.toda.service.FirebaseRealtimeDatabaseService
import com.example.toda.service.FirebaseAuthService
import com.example.toda.service.FirebaseSyncService
import com.example.toda.service.ChatService
import com.example.toda.service.RoutingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseRealtimeDatabaseService(): FirebaseRealtimeDatabaseService {
        return FirebaseRealtimeDatabaseService()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuthService(): FirebaseAuthService {
        return FirebaseAuthService()
    }

    @Provides
    @Singleton
    fun provideFirebaseSyncService(): FirebaseSyncService {
        return FirebaseSyncService()
    }

    @Provides
    @Singleton
    fun provideRoutingService(): RoutingService {
        return RoutingService()
    }

    @Provides
    @Singleton
    fun provideChatService(): ChatService {
        return ChatService()
    }

    @Provides
    @Singleton
    fun provideTODARepository(
        firebaseService: FirebaseRealtimeDatabaseService,
        authService: FirebaseAuthService
    ): TODARepository {
        return TODARepository(firebaseService, authService)
    }
}
