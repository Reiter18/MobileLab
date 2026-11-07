package com.example.drivenext.domain.usecase

import com.example.drivenext.domain.repository.NetworkRepository

class CheckInternetUseCase(private val repository: NetworkRepository) {
    operator fun invoke(): Boolean = repository.hasInternetConnection()
}
