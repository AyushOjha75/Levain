package com.ayushojha.levain.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.BackupManager
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val busy: Boolean = false,
    val lastResult: String? = null,
)

class SettingsViewModel(
    private val backup: BackupManager,
    private val repository: com.ayushojha.levain.data.LevainRepository,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun export(open: () -> OutputStream?) {
        run("Backup exported ✓", "Export failed") { open()?.use { backup.export(it) } ?: error("no stream") }
    }

    fun import(open: () -> InputStream?) {
        run("Backup restored ✓", "Import failed — is that a Levain backup?") {
            open()?.use { backup.import(it) } ?: error("no stream")
            // Import bypasses the repository choke point: re-arm reminders for
            // the restored starters ourselves.
            repository.rescheduleReminders()
        }
    }

    private fun run(okMessage: String, failMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, lastResult = null) }
            val result = runCatching {
                kotlinx.coroutines.withContext(ioDispatcher) { block() }
            }
            _uiState.update {
                it.copy(busy = false, lastResult = if (result.isSuccess) okMessage else failMessage)
            }
        }
    }
}
