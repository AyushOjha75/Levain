package com.ayushojha.levain.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.Starter
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WizardStep { CHOICE, NAME, HOME, FIRST_FEED, PROGRAM_NAME, DONE }

data class WizardState(
    val step: WizardStep = WizardStep.CHOICE,
    /** true = user has a living starter; false = grow one from scratch. */
    val hasStarter: Boolean? = null,
    val name: String = "",
    val home: LifecycleState = LifecycleState.ACTIVE,
    val activeIntervalHours: Int = 24,
    val dormantIntervalHours: Int = 168,
    val ratio: String = "1:5:5",
    val flourType: String = "White",
    val createdStarterId: Long? = null,
) {
    val canAdvanceName: Boolean get() = name.isNotBlank()
}

/**
 * One wizard, two paths: first-feed onboarding for an existing starter, or
 * the create-a-starter 7-day program for growing one from scratch.
 */
class StarterWizardViewModel(
    private val repository: LevainRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardState())
    val uiState: StateFlow<WizardState> = _uiState.asStateFlow()

    fun chooseHasStarter(has: Boolean) = _uiState.update {
        it.copy(hasStarter = has, step = if (has) WizardStep.NAME else WizardStep.PROGRAM_NAME)
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }
    fun setHome(state: LifecycleState) = _uiState.update { it.copy(home = state) }
    fun setActiveInterval(hours: Int) = _uiState.update { it.copy(activeIntervalHours = hours.coerceAtLeast(1)) }
    fun setDormantInterval(hours: Int) = _uiState.update { it.copy(dormantIntervalHours = hours.coerceAtLeast(1)) }
    fun setRatio(ratio: String) = _uiState.update { it.copy(ratio = ratio) }
    fun setFlourType(flour: String) = _uiState.update { it.copy(flourType = flour) }

    fun advanceFromName() = _uiState.update { it.copy(step = WizardStep.HOME) }
    fun advanceFromHome() = _uiState.update { it.copy(step = WizardStep.FIRST_FEED) }

    fun back() = _uiState.update {
        val previous = when (it.step) {
            WizardStep.NAME, WizardStep.PROGRAM_NAME -> WizardStep.CHOICE
            WizardStep.HOME -> WizardStep.NAME
            WizardStep.FIRST_FEED -> WizardStep.HOME
            else -> it.step
        }
        it.copy(step = previous)
    }

    /** Onboarding finish: create the starter and log the feeding just given. */
    fun finishOnboarding(fedJustNow: Boolean) {
        val s = _uiState.value
        if (!s.canAdvanceName) return
        viewModelScope.launch {
            val id = repository.createStarter(
                Starter(
                    name = s.name.trim(),
                    state = s.home,
                    activeIntervalHours = s.activeIntervalHours,
                    dormantIntervalHours = s.dormantIntervalHours,
                    createdAtEpochMs = clock.instant().toEpochMilli(),
                )
            )
            if (fedJustNow) {
                repository.logFeeding(
                    Feeding(
                        starterId = id,
                        timestampEpochMs = clock.instant().toEpochMilli(),
                        ratio = s.ratio,
                        flourType = s.flourType,
                    )
                )
            }
            _uiState.update { it.copy(step = WizardStep.DONE, createdStarterId = id) }
        }
    }

    /** Program finish: a from-scratch starter on the 7-day program, day 1 today. */
    fun startProgram() {
        val s = _uiState.value
        if (!s.canAdvanceName) return
        viewModelScope.launch {
            val now = clock.instant().toEpochMilli()
            val id = repository.createStarter(
                Starter(
                    name = s.name.trim(),
                    state = LifecycleState.ACTIVE,
                    activeIntervalHours = 24, // the program rhythm is daily
                    createdAtEpochMs = now,
                    programStartedAtEpochMs = now,
                )
            )
            _uiState.update { it.copy(step = WizardStep.DONE, createdStarterId = id) }
        }
    }
}
