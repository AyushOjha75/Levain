package com.ayushojha.levain.ui.bake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Bake
import com.ayushojha.levain.data.BakeSessions
import com.ayushojha.levain.data.BakeStatus
import com.ayushojha.levain.data.BakeStep
import com.ayushojha.levain.domain.BakePlanner
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveBakeUiState(
    val bake: Bake? = null,
    val steps: List<BakeStep> = emptyList(),
    val now: Instant = Instant.EPOCH,
) {
    /** The step the baker is on: the first one not yet ticked off. */
    val current: BakeStep? get() = steps.firstOrNull { it.completedAtEpochMs == null }
    val done: Int get() = steps.count { it.completedAtEpochMs != null }
    val held: Boolean get() = bake?.status == BakeStatus.HELD

    /** Time left on the current step. Negative once it's overdue. */
    val remaining: Duration?
        get() = current?.dueAtEpochMs?.let { Duration.between(now, Instant.ofEpochMilli(it)) }

    val overdue: Boolean get() = remaining?.isNegative == true
    val projectedEnd: Instant? get() = BakePlanner.projectedEnd(steps)
    val finished: Boolean
        get() = bake != null && steps.isNotEmpty() && steps.all { it.completedAtEpochMs != null }
}

@Suppress("OPT_IN_USAGE")
class LiveBakeViewModel(
    private val sessions: BakeSessions,
    private val clock: Clock,
    private val bakeId: Long,
) : ViewModel() {

    // A countdown has to move, so this one ticks every second — unlike the
    // dashboard, which only needs to notice a starter crossing its due time.
    private val ticker = flow {
        while (true) {
            emit(clock.instant())
            delay(1_000)
        }
    }

    val uiState: StateFlow<LiveBakeUiState> = combine(
        flowOf(bakeId).flatMapLatest { sessions.observeSteps(it) },
        sessions.observeActive(),
        ticker,
    ) { steps, active, now ->
        LiveBakeUiState(
            bake = active?.takeIf { it.id == bakeId } ?: sessions.getBake(bakeId),
            steps = steps,
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveBakeUiState())

    fun complete(step: BakeStep) = viewModelScope.launch { sessions.complete(step.id) }
    fun uncomplete(step: BakeStep) = viewModelScope.launch { sessions.uncomplete(step.id) }
    fun extend(step: BakeStep, minutes: Long) =
        viewModelScope.launch { sessions.extend(step.id, Duration.ofMinutes(minutes)) }

    fun hold() = viewModelScope.launch { sessions.hold(bakeId) }
    fun resume() = viewModelScope.launch { sessions.resume(bakeId) }
    fun abandon() = viewModelScope.launch { sessions.abandon(bakeId) }
    fun finish(rating: Int) = viewModelScope.launch { sessions.finish(bakeId, rating) }
}
