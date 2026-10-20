package com.drivenext.app.domain.usecase

import com.drivenext.app.domain.repository.OnboardingRepository

class CompleteOnboardingUseCase(
    private val onboardingRepository: OnboardingRepository
) {
    operator fun invoke() = onboardingRepository.setOnboardingCompleted()
}
