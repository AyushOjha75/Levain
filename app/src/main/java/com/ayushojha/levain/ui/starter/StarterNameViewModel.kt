package com.ayushojha.levain.ui.starter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.Starter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The detail screen's header: the Starter itself, insights, and program graduation. */
class StarterHeaderViewModel(
    private val repository: LevainRepository,
    starterId: Long,
) : ViewModel() {

    val starter: StateFlow<Starter?> = repository.observeStarter(starterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val insights: StateFlow<com.ayushojha.levain.domain.Insights?> = kotlinx.coroutines.flow.combine(
        repository.observeFeedings(starterId),
        repository.observeObservations(starterId),
        repository.observeBakes(starterId),
    ) { feedings, observations, bakes ->
        com.ayushojha.levain.domain.InsightsCalculator.insights(feedings, observations, bakes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Day 7 is done — the program flag comes off, the starter lives on. */
    fun graduate() {
        val current = starter.value ?: return
        viewModelScope.launch {
            repository.updateStarter(current.copy(programStartedAtEpochMs = null))
        }
    }
}
