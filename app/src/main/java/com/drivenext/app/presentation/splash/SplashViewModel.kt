package com.drivenext.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drivenext.app.AppContainer
import com.drivenext.app.domain.model.StartDestination
import com.drivenext.app.domain.usecase.ResolveStartDestinationUseCase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val resolveStartDestination: ResolveStartDestinationUseCase
) : ViewModel() {

    private val _destination = Channel<StartDestination>(Channel.BUFFERED)

    val destination: Flow<StartDestination> = _destination.receiveAsFlow()

    init {
        viewModelScope.launch {
            val result = coroutineScope {
                val init: Deferred<StartDestination> = async { resolveStartDestination() }
                delay(MIN_DISPLAY_MS)
                init.await()
            }
            _destination.send(result)
        }
    }

    companion object {
        const val MIN_DISPLAY_MS = 2_500L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SplashViewModel(container.resolveStartDestination) }
        }
    }
}
