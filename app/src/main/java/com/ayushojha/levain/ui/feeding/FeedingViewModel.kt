package com.ayushojha.levain.ui.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.LevainRepository
import java.time.Clock
import java.time.Duration
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
    /** Which when-chip is selected: 0 = now, otherwise hours ago. Null = custom timestamp. */
    val hoursAgo: Int? = 0,
    val editing: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Logs a new Feeding, or edits an existing one when [feedingId] is non-null.
 * Back-filling is first-class: the when-chips set the fed timestamp, and the
 * streak judges the fed time, never the log time.
 */
class FeedingViewModel(
    private val repository: LevainRepository,
    private val clock: Clock,
    private val starterId: Long,
    private val feedingId: Long? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedingFormState())
    val uiState: StateFlow<FeedingFormState> = _uiState.asStateFlow()

    private var existing: Feeding? = null

    init {
        viewModelScope.launch {
            if (feedingId != null) {
                repository.getFeeding(feedingId)?.let { feeding ->
                    existing = feeding
                    _uiState.update {
                        it.copy(
                            ratio = feeding.ratio,
                            flourType = feeding.flourType,
                            timestampEpochMs = feeding.timestampEpochMs,
                            hoursAgo = null,
                            editing = true,
                        )
                    }
                }
            } else {
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
    }

    fun setRatio(ratio: String) = _uiState.update { it.copy(ratio = ratio) }
    fun setFlourType(flourType: String) = _uiState.update { it.copy(flourType = flourType) }

    fun setHoursAgo(hours: Int) = _uiState.update {
        it.copy(
            hoursAgo = hours,
            timestampEpochMs = if (hours == 0) null else clock.instant().minus(Duration.ofHours(hours.toLong())).toEpochMilli(),
        )
    }

    fun setTimestamp(epochMs: Long) = _uiState.update { it.copy(timestampEpochMs = epochMs, hoursAgo = null) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val current = existing
            if (current != null) {
                repository.updateFeeding(
                    current.copy(
                        ratio = state.ratio,
                        flourType = state.flourType,
                        timestampEpochMs = state.timestampEpochMs ?: current.timestampEpochMs,
                    )
                )
            } else {
                repository.logFeeding(
                    Feeding(
                        starterId = starterId,
                        timestampEpochMs = state.timestampEpochMs ?: clock.instant().toEpochMilli(),
                        ratio = state.ratio,
                        flourType = state.flourType,
                    )
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
