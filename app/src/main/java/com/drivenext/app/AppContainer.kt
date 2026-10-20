package com.drivenext.app

import android.content.Context
import com.drivenext.app.data.local.AppPreferences
import com.drivenext.app.data.network.ConnectivityNetworkRepository
import com.drivenext.app.data.repository.OnboardingRepositoryImpl
import com.drivenext.app.data.repository.SessionRepositoryImpl
import com.drivenext.app.domain.repository.NetworkRepository
import com.drivenext.app.domain.repository.OnboardingRepository
import com.drivenext.app.domain.repository.SessionRepository
import com.drivenext.app.domain.usecase.CompleteOnboardingUseCase
import com.drivenext.app.domain.usecase.ResolveStartDestinationUseCase

class AppContainer(context: Context) {

    private val preferences = AppPreferences(context)

    private val networkRepository: NetworkRepository = ConnectivityNetworkRepository(context)
    private val onboardingRepository: OnboardingRepository = OnboardingRepositoryImpl(preferences)
    private val sessionRepository: SessionRepository = SessionRepositoryImpl(preferences)

    val resolveStartDestination = ResolveStartDestinationUseCase(
        networkRepository, onboardingRepository, sessionRepository
    )
    val completeOnboarding = CompleteOnboardingUseCase(onboardingRepository)
}
