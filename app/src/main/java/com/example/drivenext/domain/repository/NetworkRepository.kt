package com.example.drivenext.domain.repository

interface NetworkRepository {
    fun hasInternetConnection(): Boolean
}
