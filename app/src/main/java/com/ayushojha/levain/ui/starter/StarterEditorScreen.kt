package com.ayushojha.levain.ui.starter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.ayushojha.levain.ui.theme.Spacing
import com.ayushojha.levain.ui.components.LevainTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.ui.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarterEditorScreen(starterId: Long?, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "starter-editor-$starterId") {
        StarterEditorViewModel(it.repository, it.clock, starterId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            LevainTopBar(
                title = { Text(if (starterId == null) "New starter" else "Edit starter") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(Spacing.l)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Where does it live?", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FilterChip(
                    selected = state.state == LifecycleState.ACTIVE,
                    onClick = { viewModel.setState(LifecycleState.ACTIVE) },
                    label = { Text("Counter (active)") },
                )
                FilterChip(
                    selected = state.state == LifecycleState.DORMANT,
                    onClick = { viewModel.setState(LifecycleState.DORMANT) },
                    label = { Text("Fridge (dormant)") },
                )
                FilterChip(
                    selected = state.state == LifecycleState.ARCHIVED,
                    onClick = { viewModel.setState(LifecycleState.ARCHIVED) },
                    label = { Text("Archived") },
                )
            }

            if (state.state != LifecycleState.ARCHIVED) {
                OutlinedTextField(
                    value = state.activeIntervalHours.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setActiveIntervalHours) },
                    label = { Text("Feed every (hours, while active)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.dormantIntervalHours.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setDormantIntervalHours) },
                    label = { Text("Feed every (hours, while dormant)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
