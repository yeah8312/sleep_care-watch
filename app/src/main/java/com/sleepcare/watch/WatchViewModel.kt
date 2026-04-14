package com.sleepcare.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepcare.watch.domain.model.WatchUiState
import com.sleepcare.watch.domain.session.WatchSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchViewModel(
    private val repository: WatchSessionRepository,
) : ViewModel() {
    val uiState: StateFlow<WatchUiState> = repository.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.uiState.value,
    )

    fun dismissAlert() {
        repository.dismissAlert()
    }

    fun openSettings() {
        repository.showSettings()
    }

    fun closeSettings() {
        repository.hideSettings()
    }

    fun refreshCapabilities() {
        viewModelScope.launch {
            repository.refreshCapabilities()
        }
    }

    fun openOnPhone() {
        viewModelScope.launch {
            repository.openOnPhone()
        }
    }

    fun retrySync() {
        repository.retrySync()
    }

    fun stopSession() {
        repository.stopFromWatch()
    }

    companion object {
        fun factory(repository: WatchSessionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WatchViewModel(repository) as T
                }
            }
    }
}
