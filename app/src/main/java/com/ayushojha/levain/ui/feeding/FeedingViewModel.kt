package com.ayushojha.levain.ui.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.LevainRepository
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedingFormState(
    val ratio: String = "",
    val flourType: String = "",
    /** Null means "use now at save time". */
    val timestampEpochMs: Long? = null,
    val saved: Boolean = false,
)

class FeedingViewModel(
    private val repository: LevainRepository,
    private val clock: Clock,
    private val starterId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedingFormState())
    val uiState: StateFlow<FeedingFormState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // The common case is two taps: everything pre-filled from last time.
            val last = repository.getLastFeeding(starterId)
            _uiState.update {
                it.copy(
                    ratio = last?.ratio ?: "1:5:5",
                    flourType = last?.flourType ?: "White",
                )
            }
        }
    }

    fun setRatio(ratio: String) = _uiState.update { it.copy(ratio = ratio) }
    fun setFlourType(flourType: String) = _uiState.update { it.copy(flourType = flourType) }
    fun setTimestamp(epochMs: Long) = _uiState.update { it.copy(timestampEpochMs = epochMs) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.logFeeding(
                Feeding(
                    starterId = starterId,
                    timestampEpochMs = state.timestampEpochMs ?: clock.instant().toEpochMilli(),
                    ratio = state.ratio,
                    flourType = state.flourType,
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
