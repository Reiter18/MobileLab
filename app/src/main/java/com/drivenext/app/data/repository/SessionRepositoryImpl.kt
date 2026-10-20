package com.drivenext.app.data.repository

import com.drivenext.app.data.local.AppPreferences
import com.drivenext.app.domain.repository.SessionRepository

class SessionRepositoryImpl(
    private val preferences: AppPreferences
) : SessionRepository {

    override suspend fun hasValidSession(): Boolean = !preferences.accessToken.isNullOrBlank()
}
