package com.drivenext.app.domain.repository

interface OnboardingRepository {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted()
}
