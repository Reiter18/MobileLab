package com.example.drivenext.data.preferences

import android.content.Context
import com.example.drivenext.domain.repository.OnboardingRepository

class PreferencesOnboardingRepository(context: Context) : OnboardingRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "drivenext_preferences", Context.MODE_PRIVATE
    )

    override fun isCompleted(): Boolean =
        preferences.getBoolean("onboarding_completed", false)

    override fun markCompleted() {
        preferences.edit().putBoolean("onboarding_completed", true).apply()
    }
}
