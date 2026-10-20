package com.drivenext.app.data.repository

import com.drivenext.app.data.local.AppPreferences
import com.drivenext.app.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(
    private val preferences: AppPreferences
) : OnboardingRepository {

    override fun isOnboardingCompleted(): Boolean = preferences.onboardingCompleted

    override fun setOnboardingCompleted() {
        preferences.onboardingCompleted = true
    }
}
