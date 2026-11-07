package com.example.drivenext.domain.repository

interface OnboardingRepository {
    fun isCompleted(): Boolean
    fun markCompleted()
}
