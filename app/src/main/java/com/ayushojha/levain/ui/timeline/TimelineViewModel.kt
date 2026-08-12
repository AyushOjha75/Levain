package com.ayushojha.levain.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Bake
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.LevainRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One entry in a Starter's chronological story. */
sealed interface TimelineEvent {
    val timestampEpochMs: Long

    data class FeedingEvent(val feeding: Feeding) : TimelineEvent {
        override val timestampEpochMs get() = feeding.timestampEpochMs
    }

    data class ObservationEvent(val observation: HealthObservation) : TimelineEvent {
        override val timestampEpochMs get() = observation.timestampEpochMs
    }

    data class BakeEvent(val bake: Bake) : TimelineEvent {
        override val timestampEpochMs get() = bake.timestampEpochMs
    }
}

data class TimelineUiState(
    val events: List<TimelineEvent> = emptyList(),
)

class TimelineViewModel(
    private val repository: LevainRepository,
    starterId: Long,
) : ViewModel() {

    val uiState: StateFlow<TimelineUiState> = combine(
        repository.observeFeedings(starterId),
        repository.observeObservations(starterId),
        repository.observeBakes(starterId),
    ) { feedings, observations, bakes ->
        TimelineUiState(
            events = buildList {
                feedings.forEach { add(TimelineEvent.FeedingEvent(it)) }
                observations.forEach { add(TimelineEvent.ObservationEvent(it)) }
                bakes.forEach { add(TimelineEvent.BakeEvent(it)) }
            }.sortedByDescending { it.timestampEpochMs }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())

    fun delete(event: TimelineEvent) {
        viewModelScope.launch {
            when (event) {
                is TimelineEvent.FeedingEvent -> repository.deleteFeeding(event.feeding)
                is TimelineEvent.ObservationEvent -> repository.deleteObservation(event.observation)
                is TimelineEvent.BakeEvent -> repository.deleteBake(event.bake)
            }
        }
    }
}
