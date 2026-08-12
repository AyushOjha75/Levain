package com.ayushojha.levain.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.Starter
import com.ayushojha.levain.domain.DueCalculator
import com.ayushojha.levain.domain.Dueness
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StarterCard(
    val starter: Starter,
    val lastFeeding: Feeding?,
    val lastObservation: HealthObservation?,
    val dueness: Dueness,
)

data class DashboardUiState(
    val cards: List<StarterCard> = emptyList(),
)

class DashboardViewModel(
    repository: LevainRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeStarters(),
        repository.observeLastFeedings(),
        repository.observeLastObservations(),
    ) { starters, lastFeedings, lastObservations ->
        val feedingByStarter = lastFeedings.associateBy { it.starterId }
        val observationByStarter = lastObservations.associateBy { it.starterId }
        DashboardUiState(
            cards = starters.map { starter ->
                val lastFeeding = feedingByStarter[starter.id]
                StarterCard(
                    starter = starter,
                    lastFeeding = lastFeeding,
                    lastObservation = observationByStarter[starter.id],
                    dueness = DueCalculator.dueness(starter, lastFeeding, clock.instant()),
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
