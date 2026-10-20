package com.drivenext.app.domain.usecase

import com.drivenext.app.domain.model.StartDestination
import com.drivenext.app.domain.repository.NetworkRepository
import com.drivenext.app.domain.repository.OnboardingRepository
import com.drivenext.app.domain.repository.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveStartDestinationUseCaseTest {

    private fun useCase(
        internet: Boolean,
        onboardingDone: Boolean,
        session: Boolean
    ) = ResolveStartDestinationUseCase(
        object : NetworkRepository {
            override fun isInternetAvailable() = internet
        },
        object : OnboardingRepository {
            override fun isOnboardingCompleted() = onboardingDone
            override fun setOnboardingCompleted() = Unit
        },
        object : SessionRepository {
            override suspend fun hasValidSession() = session
        }
    )

    @Test
    fun `no internet leads to no connection screen`() = runBlocking {
        assertEquals(StartDestination.NO_CONNECTION, useCase(false, false, false)())
    }

    @Test
    fun `first launch leads to onboarding`() = runBlocking {
        assertEquals(StartDestination.ONBOARDING, useCase(true, false, false)())
    }

    @Test
    fun `onboarding passed but no valid token leads to login`() = runBlocking {
        assertEquals(StartDestination.LOGIN, useCase(true, true, false)())
    }

    @Test
    fun `valid token leads to main`() = runBlocking {
        assertEquals(StartDestination.MAIN, useCase(true, true, true)())
    }

    @Test
    fun `unexpected error falls back to login`() = runBlocking {
        val failing = ResolveStartDestinationUseCase(
            object : NetworkRepository {
                override fun isInternetAvailable(): Boolean = throw IllegalStateException("boom")
            },
            object : OnboardingRepository {
                override fun isOnboardingCompleted() = true
                override fun setOnboardingCompleted() = Unit
            },
            object : SessionRepository {
                override suspend fun hasValidSession() = true
            }
        )
        assertEquals(StartDestination.LOGIN, failing())
    }
}
