package com.tetrova_flowledger.di

import com.tetrova_flowledger.data.session.AuthPostLoginCoordinator
import com.tetrova_flowledger.data.session.DefaultAuthPostLoginCoordinator
import com.tetrova_flowledger.repository.AuthRepository
import com.tetrova_flowledger.repository.FinanceRepository
import com.tetrova_flowledger.repository.GoalRepository
import com.tetrova_flowledger.repository.firebase.FirebaseAuthRepository
import com.tetrova_flowledger.repository.local.LocalFinanceRepository
import com.tetrova_flowledger.repository.local.LocalGoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthPostLoginCoordinator(
        coordinator: DefaultAuthPostLoginCoordinator,
    ): AuthPostLoginCoordinator

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        repository: FirebaseAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(
        repository: LocalFinanceRepository,
    ): FinanceRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        repository: LocalGoalRepository,
    ): GoalRepository
}
