package com.ayushojha.levain.ui.observation

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.appContainer
import com.ayushojha.levain.data.RiseRating
import com.ayushojha.levain.data.Smell
import com.ayushojha.levain.ui.containerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationScreen(starterId: Long, onDone: () -> Unit) {
    val viewModel = containerViewModel(key = "observation-$starterId") {
        ObservationViewModel(it.repository, it.clock, starterId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val photoStore = LocalContext.current.appContainer.photoStore

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.setPhotoPath(photoStore.importPhoto(it)) }
    }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How does it look?") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rise", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RiseRating.entries.forEach { rating ->
                    FilterChip(
                        selected = state.riseRating == rating,
                        onClick = { viewModel.setRiseRating(rating) },
                        label = { Text(rating.name.lowercase()) },
                    )
                }
            }

            state.riseRating?.let { rating ->
                com.ayushojha.levain.domain.Tips.riseTips[rating]?.let { tip ->
                    TipCard(tip)
                }
            }

            OutlinedTextField(
                value = state.timeToPeakMinutes?.let { (it / 60.0).let { h -> if (h % 1.0 == 0.0) h.roundToInt().toString() else "%.1f".format(h) } } ?: "",
                onValueChange = { text ->
                    viewModel.setTimeToPeakMinutes(text.toDoubleOrNull()?.let { (it * 60).roundToInt() })
                },
                label = { Text("Time to peak (hours, optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Smell", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Smell.entries.take(4).forEach { smell ->
                    FilterChip(
                        selected = state.smell == smell,
                        onClick = { viewModel.setSmell(if (state.smell == smell) null else smell) },
                        label = { Text(smell.name.lowercase()) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Smell.entries.drop(4).forEach { smell ->
                    FilterChip(
                        selected = state.smell == smell,
                        onClick = { viewModel.setSmell(if (state.smell == smell) null else smell) },
                        label = { Text(smell.name.lowercase()) },
                    )
                }
            }

            state.smell?.let { smell ->
                com.ayushojha.levain.domain.Tips.smellTips[smell]?.let { tip ->
                    TipCard(tip)
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
                    modifier = Modifier.padding(start = 4.dp),
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
                Text("Save observation")
            }
        }
    }
}

@Composable
internal fun TipCard(tip: String) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "💡 $tip",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}
