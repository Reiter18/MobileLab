package com.example.drivenext.presentation.startup

import androidx.lifecycle.ViewModel
import com.example.drivenext.domain.usecase.CheckInternetUseCase
import com.example.drivenext.domain.usecase.IsOnboardingCompleteUseCase

class StartupViewModel(
    private val checkInternet: CheckInternetUseCase,
    private val isOnboardingComplete: IsOnboardingCompleteUseCase
) : ViewModel() {
    fun hasInternet(): Boolean = checkInternet()
    fun hasCompletedOnboarding(): Boolean = isOnboardingComplete()
}
