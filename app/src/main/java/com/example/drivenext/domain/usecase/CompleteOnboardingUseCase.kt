package com.example.drivenext.domain.usecase

import com.example.drivenext.domain.repository.OnboardingRepository

class CompleteOnboardingUseCase(private val repository: OnboardingRepository) {
    operator fun invoke() = repository.markCompleted()
}
