package com.ayushojha.levain.ui.starter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.Starter
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StarterFormState(
    val name: String = "",
    val state: LifecycleState = LifecycleState.ACTIVE,
    val activeIntervalHours: Int = 24,
    val dormantIntervalHours: Int = 168,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/** Creates a Starter, or edits an existing one when [starterId] is non-null. */
class StarterEditorViewModel(
    private val repository: LevainRepository,
    private val clock: Clock,
    private val starterId: Long? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarterFormState())
    val uiState: StateFlow<StarterFormState> = _uiState.asStateFlow()

    private var existing: Starter? = null

    init {
        starterId?.let { id ->
            viewModelScope.launch {
                repository.getStarter(id)?.let { starter ->
                    existing = starter
                    _uiState.update {
                        it.copy(
                            name = starter.name,
                            state = starter.state,
                            activeIntervalHours = starter.activeIntervalHours,
                            dormantIntervalHours = starter.dormantIntervalHours,
                        )
                    }
                }
            }
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }
    fun setState(state: LifecycleState) = _uiState.update { it.copy(state = state) }
    fun setActiveIntervalHours(hours: Int) = _uiState.update { it.copy(activeIntervalHours = hours.coerceAtLeast(1)) }
    fun setDormantIntervalHours(hours: Int) = _uiState.update { it.copy(dormantIntervalHours = hours.coerceAtLeast(1)) }

    fun save() {
        val form = _uiState.value
        if (!form.canSave) return
        viewModelScope.launch {
            val current = existing
            if (current == null) {
                repository.createStarter(
                    Starter(
                        name = form.name.trim(),
                        state = form.state,
                        activeIntervalHours = form.activeIntervalHours,
                        dormantIntervalHours = form.dormantIntervalHours,
                        createdAtEpochMs = clock.instant().toEpochMilli(),
                    )
                )
            } else {
                repository.updateStarter(
                    current.copy(
                        name = form.name.trim(),
                        state = form.state,
                        activeIntervalHours = form.activeIntervalHours,
                        dormantIntervalHours = form.dormantIntervalHours,
                    )
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
