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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
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

    // Dueness is a function of the clock, so the dashboard must recompute even
    // when nothing is written — a starter crosses its due time just by sitting there.
    private val minuteTicker = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeStarters(),
        repository.observeLastFeedings(),
        repository.observeLastObservations(),
        minuteTicker,
    ) { starters, lastFeedings, lastObservations, _ ->
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
