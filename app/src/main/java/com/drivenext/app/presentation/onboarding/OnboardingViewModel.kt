package com.drivenext.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drivenext.app.AppContainer
import com.drivenext.app.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentPage: Int = 0,
    val pageCount: Int
) {
    val isLastPage: Boolean get() = currentPage == pageCount - 1
}

sealed interface OnboardingEvent {
    data class ScrollToPage(val page: Int) : OnboardingEvent
    data object NavigateToLogin : OnboardingEvent
}

class OnboardingViewModel(
    pageCount: Int,
    private val completeOnboarding: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(pageCount = pageCount))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    fun onPageSelected(position: Int) {
        _uiState.update { it.copy(currentPage = position) }
    }

    fun onNextClicked() {
        val state = _uiState.value
        if (state.isLastPage) finish() else emit(OnboardingEvent.ScrollToPage(state.currentPage + 1))
    }

    fun onSkipClicked() = finish()

    fun onBackClicked() {
        val page = _uiState.value.currentPage
        if (page > 0) emit(OnboardingEvent.ScrollToPage(page - 1))
    }

    private fun finish() {
        completeOnboarding()
        emit(OnboardingEvent.NavigateToLogin)
    }

    private fun emit(event: OnboardingEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    companion object {
        fun factory(container: AppContainer, pageCount: Int): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { OnboardingViewModel(pageCount, container.completeOnboarding) }
            }
    }
}
