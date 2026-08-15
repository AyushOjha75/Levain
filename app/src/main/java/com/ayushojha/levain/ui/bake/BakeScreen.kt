package com.ayushojha.levain.ui.bake

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.appContainer
import com.ayushojha.levain.ui.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakeScreen(starterId: Long, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "bake-$starterId") {
        BakeViewModel(it.repository, it.clock, starterId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val photoStore = LocalContext.current.appContainer.photoStore

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.setPhotoPath(photoStore.importPhoto(it)) }
    }

    // A 5-star bake earns its confetti before leaving the screen.
    LaunchedEffect(state.saved) {
        if (state.saved && state.outcomeRating != 5) onDone()
    }

    if (state.saved && state.outcomeRating == 5) {
        com.ayushojha.levain.ui.celebration.ConfettiOverlay(onFinished = onDone)
    }

    Scaffold(
        topBar = {
            LevainTopBar(
                title = { Text("Log a bake") },
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
                value = state.levainNotes,
                onValueChange = viewModel::setLevainNotes,
                label = { Text("Levain build (ratio, ripeness…)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("When was it baked?", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.s)) {
                listOf(0 to "Today", 1 to "Yesterday", 2 to "2 days ago").forEach { (days, label) ->
                    androidx.compose.material3.FilterChip(
                        selected = state.daysAgo == days,
                        onClick = { viewModel.setDaysAgo(days) },
                        label = { Text(label) },
                    )
                }
            }

            Text("How did the bread turn out?", style = MaterialTheme.typography.titleSmall)
            Row {
                (1..5).forEach { star ->
                    IconButton(onClick = { viewModel.setOutcomeRating(star) }) {
                        Icon(
                            imageVector = if ((state.outcomeRating ?: 0) >= star) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "$star star",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Text(
                    if (state.photoPath == null) "  Add photo" else "  Photo attached ✓",
                    modifier = Modifier.padding(start = Spacing.xs),
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save bake")
            }
        }
    }
}
