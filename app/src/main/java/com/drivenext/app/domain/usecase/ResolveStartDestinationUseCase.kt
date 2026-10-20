package com.drivenext.app.domain.usecase

import com.drivenext.app.domain.model.StartDestination
import com.drivenext.app.domain.repository.NetworkRepository
import com.drivenext.app.domain.repository.OnboardingRepository
import com.drivenext.app.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException

class ResolveStartDestinationUseCase(
    private val networkRepository: NetworkRepository,
    private val onboardingRepository: OnboardingRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): StartDestination = try {
        when {
            !networkRepository.isInternetAvailable() -> StartDestination.NO_CONNECTION
            !onboardingRepository.isOnboardingCompleted() -> StartDestination.ONBOARDING
            !sessionRepository.hasValidSession() -> StartDestination.LOGIN
            else -> StartDestination.MAIN
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        StartDestination.LOGIN
    }
}
