package com.example.finance_management_system.tests.auth

import com.example.finance_management_system.data.session.AuthPostLoginCoordinator
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.repository.AuthRepository
import com.example.finance_management_system.repository.AuthUser
import com.example.finance_management_system.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendPasswordReset_withRegisteredEmail_sendsResetEmailAndShowsSuccessMessage() = runTest {
        val fakeRepository = PasswordResetAuthRepository(
            resetResult = Result.success(Unit),
        )
        val viewModel = AuthViewModel(
            repository = fakeRepository,
            authPostLoginCoordinator = PasswordResetAuthPostLoginCoordinator(),
        )

        viewModel.updateEmail("testuser01@example.com")

        viewModel.sendPasswordReset()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("testuser01@example.com", fakeRepository.lastResetEmail)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(AppDefaults.SUCCESS_RESET_PASSWORD, state.successMessage)
    }
}

private class PasswordResetAuthRepository(
    private val resetResult: Result<Unit>,
) : AuthRepository {
    var lastResetEmail: String? = null

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        lastResetEmail = email
        return resetResult
    }
}

private class PasswordResetAuthPostLoginCoordinator : AuthPostLoginCoordinator {
    override suspend fun onAuthenticated(uid: String) {
        error("Not used in this test")
    }
}