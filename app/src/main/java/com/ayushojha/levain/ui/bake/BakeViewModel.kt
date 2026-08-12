package com.ayushojha.levain.ui.bake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Bake
import com.ayushojha.levain.data.LevainRepository
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BakeFormState(
    val levainNotes: String = "",
    val outcomeRating: Int? = null,
    val photoPath: String? = null,
    val note: String = "",
    /** Bakes are often logged after the fact — "the loaf from Tuesday". */
    val daysAgo: Int = 0,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = outcomeRating != null
}

class BakeViewModel(
    private val repository: LevainRepository,
    private val clock: Clock,
    private val starterId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BakeFormState())
    val uiState: StateFlow<BakeFormState> = _uiState.asStateFlow()

    fun setLevainNotes(notes: String) = _uiState.update { it.copy(levainNotes = notes) }
    fun setOutcomeRating(rating: Int) = _uiState.update { it.copy(outcomeRating = rating.coerceIn(1, 5)) }
    fun setPhotoPath(path: String?) = _uiState.update { it.copy(photoPath = path) }
    fun setNote(note: String) = _uiState.update { it.copy(note = note) }
    fun setDaysAgo(days: Int) = _uiState.update { it.copy(daysAgo = days.coerceIn(0, 365)) }

    fun save() {
        val state = _uiState.value
        val outcomeRating = state.outcomeRating ?: return
        viewModelScope.launch {
            repository.logBake(
                Bake(
                    starterId = starterId,
                    timestampEpochMs = clock.instant()
                        .minus(java.time.Duration.ofDays(state.daysAgo.toLong()))
                        .toEpochMilli(),
                    levainNotes = state.levainNotes.ifBlank { null },
                    outcomeRating = outcomeRating,
                    photoPath = state.photoPath,
                    note = state.note.ifBlank { null },
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
