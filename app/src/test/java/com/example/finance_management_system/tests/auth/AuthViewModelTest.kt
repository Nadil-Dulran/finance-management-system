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
class AuthViewModelTest {
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
    fun register_withValidDetails_registersUserAndCompletesPostAuthFlow() = runTest {
        val fakeRepository = FakeAuthRepository(
            registerResult = Result.success(
                AuthUser(
                    uid = "uid-123",
                    displayName = "Test User",
                    email = "testuser01@example.com",
                ),
            ),
        )
        val fakeCoordinator = FakeAuthPostLoginCoordinator()
        val viewModel = AuthViewModel(
            repository = fakeRepository,
            authPostLoginCoordinator = fakeCoordinator,
        )
        var onSuccessCalled = false

        viewModel.updateName("Test User")
        viewModel.updateEmail("testuser01@example.com")
        viewModel.updatePassword("Test@12345")

        viewModel.register {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Test User", fakeRepository.registeredName)
        assertEquals("testuser01@example.com", fakeRepository.registeredEmail)
        assertEquals("Test@12345", fakeRepository.registeredPassword)
        assertEquals("uid-123", fakeCoordinator.lastAuthenticatedUid)
        assertTrue(onSuccessCalled)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }
}

private class FakeAuthRepository(
    private val registerResult: Result<AuthUser>,
) : AuthRepository {
    var registeredName: String? = null
    var registeredEmail: String? = null
    var registeredPassword: String? = null

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
        registeredName = name
        registeredEmail = email
        registeredPassword = password
        return registerResult
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        error("Not used in this test")
    }
}

private class FakeAuthPostLoginCoordinator : AuthPostLoginCoordinator {
    var lastAuthenticatedUid: String? = null

    override suspend fun onAuthenticated(uid: String) {
        lastAuthenticatedUid = uid
    }
}
