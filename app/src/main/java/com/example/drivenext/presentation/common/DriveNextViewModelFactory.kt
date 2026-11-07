package com.example.drivenext.presentation.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.drivenext.data.network.AndroidNetworkRepository
import com.example.drivenext.data.preferences.PreferencesOnboardingRepository
import com.example.drivenext.domain.usecase.CheckInternetUseCase
import com.example.drivenext.domain.usecase.CompleteOnboardingUseCase
import com.example.drivenext.domain.usecase.IsOnboardingCompleteUseCase
import com.example.drivenext.presentation.onboarding.OnboardingViewModel
import com.example.drivenext.presentation.startup.StartupViewModel

class DriveNextViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val networkUseCase = CheckInternetUseCase(AndroidNetworkRepository(appContext))
        val onboardingRepository = PreferencesOnboardingRepository(appContext)

        return when {
            modelClass.isAssignableFrom(StartupViewModel::class.java) ->
                StartupViewModel(
                    networkUseCase,
                    IsOnboardingCompleteUseCase(onboardingRepository)
                ) as T
            modelClass.isAssignableFrom(NetworkViewModel::class.java) ->
                NetworkViewModel(networkUseCase) as T
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                OnboardingViewModel(CompleteOnboardingUseCase(onboardingRepository)) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
