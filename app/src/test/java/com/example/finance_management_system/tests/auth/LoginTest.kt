package com.example.finance_management_system.tests.auth

import com.example.finance_management_system.data.session.AuthPostLoginCoordinator
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginTest {
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
    fun login_withCorrectCredentials_authenticatesUserAndLoadsDashboard() = runTest {
        val fakeRepository = LoginAuthRepository(
            loginResult = Result.success(
                AuthUser(
                    uid = "uid-200",
                    displayName = "Test User",
                    email = "testuser01@example.com",
                ),
            ),
        )
        val fakeCoordinator = LoginAuthPostLoginCoordinator()
        val viewModel = AuthViewModel(
            repository = fakeRepository,
            authPostLoginCoordinator = fakeCoordinator,
        )
        var onSuccessCalled = false

        viewModel.updateEmail("testuser01@example.com")
        viewModel.updatePassword("Test@12345")

        viewModel.login {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("testuser01@example.com", fakeRepository.lastEmail)
        assertEquals("Test@12345", fakeRepository.lastPassword)
        assertEquals("uid-200", fakeCoordinator.lastAuthenticatedUid)
        assertTrue(onSuccessCalled)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }
}

private class LoginAuthRepository(
    private val loginResult: Result<AuthUser>,
) : AuthRepository {
    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        lastEmail = email
        lastPassword = password
        return loginResult
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        error("Not used in this test")
    }
}

private class LoginAuthPostLoginCoordinator : AuthPostLoginCoordinator {
    var lastAuthenticatedUid: String? = null

    override suspend fun onAuthenticated(uid: String) {
        lastAuthenticatedUid = uid
    }
}