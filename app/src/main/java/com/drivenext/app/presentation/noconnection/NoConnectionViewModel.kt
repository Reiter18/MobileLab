package com.drivenext.app.presentation.noconnection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drivenext.app.AppContainer
import com.drivenext.app.domain.model.StartDestination
import com.drivenext.app.domain.usecase.ResolveStartDestinationUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class NoConnectionUiState(val isLoading: Boolean = false)

class NoConnectionViewModel(
    private val resolveStartDestination: ResolveStartDestinationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoConnectionUiState())
    val uiState: StateFlow<NoConnectionUiState> = _uiState.asStateFlow()

    private val _destination = Channel<StartDestination>(Channel.BUFFERED)
    val destination: Flow<StartDestination> = _destination.receiveAsFlow()

    fun onRetryClicked() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = NoConnectionUiState(isLoading = true)
            delay(PROGRESS_MIN_MS) // чтобы индикатор был заметен пользователю
            val result = resolveStartDestination()
            _uiState.value = NoConnectionUiState(isLoading = false)
            if (result != StartDestination.NO_CONNECTION) {
                _destination.send(result)
            }
        }
    }

    companion object {
        private const val PROGRESS_MIN_MS = 600L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { NoConnectionViewModel(container.resolveStartDestination) }
        }
    }
}
