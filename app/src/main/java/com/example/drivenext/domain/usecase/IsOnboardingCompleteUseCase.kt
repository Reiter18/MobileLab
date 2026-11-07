package com.example.drivenext.domain.usecase

import com.example.drivenext.domain.repository.OnboardingRepository

class IsOnboardingCompleteUseCase(private val repository: OnboardingRepository) {
    operator fun invoke(): Boolean = repository.isCompleted()
}
