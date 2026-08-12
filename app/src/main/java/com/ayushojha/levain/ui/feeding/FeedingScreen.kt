package com.ayushojha.levain.ui.feeding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.ui.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingScreen(starterId: Long, feedingId: Long? = null, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "feeding-$starterId-$feedingId") {
        FeedingViewModel(it.repository, it.clock, starterId, feedingId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing) "Edit feeding" else "Log feeding") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.editing) {
                Text(
                    "Pre-filled from last time — usually you just hit Save.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("When?", style = MaterialTheme.typography.titleSmall)
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(0 to "Just now", 1 to "1h ago", 3 to "3h ago").forEach { (hours, label) ->
                    androidx.compose.material3.FilterChip(
                        selected = state.hoursAgo == hours,
                        onClick = { viewModel.setHoursAgo(hours) },
                        label = { Text(label) },
                    )
                }
            }
            OutlinedTextField(
                value = when {
                    state.hoursAgo != null && state.hoursAgo !in listOf(0, 1, 3) -> state.hoursAgo.toString()
                    else -> ""
                },
                onValueChange = { it.toIntOrNull()?.let(viewModel::setHoursAgo) },
                label = { Text("Custom (hours ago)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.ratio,
                onValueChange = viewModel::setRatio,
                label = { Text("Ratio (starter : flour : water)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.flourType,
                onValueChange = viewModel::setFlourType,
                label = { Text("Flour") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.editing) "Save changes" else "Save feeding")
            }
        }
    }
}
