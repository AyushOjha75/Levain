package com.ayushojha.levain.ui.observation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.RiseRating
import com.ayushojha.levain.data.Smell
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ObservationFormState(
    val riseRating: RiseRating? = null,
    val timeToPeakMinutes: Int? = null,
    val smell: Smell? = null,
    val photoPath: String? = null,
    val note: String = "",
    val saved: Boolean = false,
) {
    /** Rise rating is the one required field; everything else is optional. */
    val canSave: Boolean get() = riseRating != null
}

class ObservationViewModel(
    private val repository: LevainRepository,
    private val clock: Clock,
    private val starterId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObservationFormState())
    val uiState: StateFlow<ObservationFormState> = _uiState.asStateFlow()

    fun setRiseRating(rating: RiseRating) = _uiState.update { it.copy(riseRating = rating) }
    fun setTimeToPeakMinutes(minutes: Int?) = _uiState.update { it.copy(timeToPeakMinutes = minutes) }
    fun setSmell(smell: Smell?) = _uiState.update { it.copy(smell = smell) }
    fun setPhotoPath(path: String?) = _uiState.update { it.copy(photoPath = path) }
    fun setNote(note: String) = _uiState.update { it.copy(note = note) }

    fun save() {
        val state = _uiState.value
        val riseRating = state.riseRating ?: return
        viewModelScope.launch {
            repository.logObservation(
                HealthObservation(
                    starterId = starterId,
                    timestampEpochMs = clock.instant().toEpochMilli(),
                    riseRating = riseRating,
                    timeToPeakMinutes = state.timeToPeakMinutes,
                    smell = state.smell,
                    photoPath = state.photoPath,
                    note = state.note.ifBlank { null },
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
