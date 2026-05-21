package com.example.finance_management_system.di

import com.example.finance_management_system.repository.AuthRepository
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
import com.example.finance_management_system.repository.firebase.FirebaseAuthRepository
import com.example.finance_management_system.repository.local.LocalFinanceRepository
import com.example.finance_management_system.repository.local.LocalGoalRepository
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
