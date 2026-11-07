package com.example.drivenext.presentation.common

import androidx.lifecycle.ViewModel
import com.example.drivenext.domain.usecase.CheckInternetUseCase

class NetworkViewModel(private val checkInternet: CheckInternetUseCase) : ViewModel() {
    fun isInternetAvailable(): Boolean = checkInternet()
}
