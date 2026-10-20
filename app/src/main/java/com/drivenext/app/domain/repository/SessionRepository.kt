package com.drivenext.app.domain.repository

interface SessionRepository {
    suspend fun hasValidSession(): Boolean
}
