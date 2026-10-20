package com.drivenext.app.domain.repository

interface NetworkRepository {
    fun isInternetAvailable(): Boolean
}
