package com.ayushojha.levain.ui.wizard

import androidx.lifecycle.ViewModel
import com.ayushojha.levain.domain.TroubleshootingNode
import com.ayushojha.levain.domain.TroubleshootingTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TroubleshootingState(
    val node: TroubleshootingNode = TroubleshootingTree.root,
    val path: List<TroubleshootingNode> = emptyList(),
) {
    val canGoBack: Boolean get() = path.isNotEmpty()
}

class TroubleshootingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TroubleshootingState())
    val uiState: StateFlow<TroubleshootingState> = _uiState.asStateFlow()

    fun select(next: TroubleshootingNode) = _uiState.update {
        it.copy(node = next, path = it.path + it.node)
    }

    fun back() = _uiState.update {
        if (it.path.isEmpty()) it
        else it.copy(node = it.path.last(), path = it.path.dropLast(1))
    }

    fun restart() = _uiState.update { TroubleshootingState() }
}
