package com.ayushojha.levain.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.Starter
import com.ayushojha.levain.domain.DueCalculator
import com.ayushojha.levain.domain.Dueness
import com.ayushojha.levain.domain.Mood
import com.ayushojha.levain.domain.MoodCalculator
import com.ayushojha.levain.domain.StreakCalculator
import com.ayushojha.levain.domain.Vitals
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
    val mood: Mood,
    val vitals: Vitals,
)

data class DashboardUiState(
    val cards: List<StarterCard> = emptyList(),
    val factOfTheDay: String = "",
    /** False until the first database emission — distinguishes "loading" from "no starters". */
    val loaded: Boolean = false,
) {
    /** The first uncelebrated milestone of the day, surfaced as a banner. */
    val milestoneBanner: String? get() = cards.firstNotNullOfOrNull { it.vitals.milestoneToday }
}

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
        repository.observeAllFeedings(),
        repository.observeLastObservations(),
        minuteTicker,
    ) { starters, allFeedings, lastObservations, _ ->
        val now = clock.instant()
        val feedingsByStarter = allFeedings.groupBy { it.starterId }
        val observationByStarter = lastObservations.associateBy { it.starterId }
        DashboardUiState(
            loaded = true,
            // Local calendar day, refreshed by the ticker — rolls over at the
            // user's midnight, not UTC's.
            factOfTheDay = com.ayushojha.levain.domain.Tips.factOfTheDay(
                java.time.LocalDate.now(clock).toEpochDay()
            ),
            cards = starters.map { starter ->
                val feedings = feedingsByStarter[starter.id].orEmpty()
                val lastFeeding = feedings.maxByOrNull { it.timestampEpochMs }
                val lastObservation = observationByStarter[starter.id]
                val dueness = DueCalculator.dueness(starter, lastFeeding, now)
                StarterCard(
                    starter = starter,
                    lastFeeding = lastFeeding,
                    lastObservation = lastObservation,
                    dueness = dueness,
                    mood = MoodCalculator.mood(starter, dueness, lastObservation, now),
                    vitals = StreakCalculator.vitals(starter, feedings, now),
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
