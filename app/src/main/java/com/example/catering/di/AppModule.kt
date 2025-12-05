package com.example.catering.di

import com.example.catering.data.repo.AuthRepository
import com.example.catering.data.repo.CateringRepository
import com.example.catering.data.repo.firebase.FirebaseAuthRepository
import com.example.catering.data.repo.firebase.FirebaseCateringRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(impl: FirebaseAuthRepository): AuthRepository = impl

    @Provides
    @Singleton
    fun provideCateringRepository(impl: FirebaseCateringRepository): CateringRepository = impl
}
