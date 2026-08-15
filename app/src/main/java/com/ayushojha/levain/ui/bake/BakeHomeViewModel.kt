package com.ayushojha.levain.ui.bake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.Bake
import com.ayushojha.levain.data.BakeSessions
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.Recipe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BakeHomeUiState(
    val recipes: List<Recipe> = emptyList(),
    val active: Bake? = null,
    val hasStarter: Boolean = false,
)

class BakeHomeViewModel(
    private val sessions: BakeSessions,
    private val repository: LevainRepository,
) : ViewModel() {

    val uiState: StateFlow<BakeHomeUiState> = combine(
        sessions.observeRecipes(),
        sessions.observeActive(),
        repository.observeStarters(),
    ) { recipes, active, starters ->
        BakeHomeUiState(
            recipes = recipes,
            active = active,
            hasStarter = starters.any { it.state != LifecycleState.ARCHIVED },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BakeHomeUiState())

    /**
     * A sourdough bake is tied to the starter that leavened it, so its history
     * can be traced back. A yeasted one has no starter to tie it to, and that
     * is not a missing value — it's the point.
     */
    fun start(recipe: Recipe, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val starterId = if (recipe.requiresStarter) firstUsableStarterId() else null
            onStarted(sessions.start(recipe.id, starterId = starterId))
        }
    }

    private suspend fun firstUsableStarterId(): Long? =
        repository.getStarters().firstOrNull { it.state == LifecycleState.ACTIVE }?.id
}
