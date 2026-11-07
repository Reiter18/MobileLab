package com.example.drivenext.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.example.drivenext.domain.usecase.CompleteOnboardingUseCase

class OnboardingViewModel(private val completeOnboarding: CompleteOnboardingUseCase) : ViewModel() {
    fun finishOnboarding() = completeOnboarding()
}
