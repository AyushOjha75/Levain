package com.ayushojha.levain.ui.starter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.appContainer
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.ui.containerViewModel
import com.ayushojha.levain.ui.formatTimestamp
import com.ayushojha.levain.ui.timeline.TimelineEvent
import com.ayushojha.levain.ui.timeline.TimelineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarterDetailScreen(
    starterId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onLogFeeding: () -> Unit,
    onLogObservation: () -> Unit,
    onLogBake: () -> Unit,
) {
    val timelineViewModel = containerViewModel(key = "timeline-$starterId") {
        TimelineViewModel(it.repository, starterId)
    }
    val headerViewModel = containerViewModel(key = "starter-header-$starterId") {
        StarterHeaderViewModel(it.repository, starterId)
    }
    val clock = androidx.compose.ui.platform.LocalContext.current.appContainer.clock
    val timeline by timelineViewModel.uiState.collectAsStateWithLifecycle()
    val starter by headerViewModel.starter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(starter?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit starter")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            starter?.programStartedAtEpochMs?.let { startedAt ->
                ProgramCard(
                    programStartedAt = java.time.Instant.ofEpochMilli(startedAt),
                    now = clock.instant(),
                    onGraduate = headerViewModel::graduate,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onLogFeeding, modifier = Modifier.weight(1f)) {
                    Text("Feed")
                }
                FilledTonalButton(onClick = onLogObservation, modifier = Modifier.weight(1f)) {
                    Text("Observe")
                }
                FilledTonalButton(onClick = onLogBake, modifier = Modifier.weight(1f)) {
                    Text("Bake")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(timeline.events, key = { "${it::class.simpleName}-${it.timestampEpochMs}-${it.hashCode()}" }) { event ->
                    TimelineEventCard(event = event, onDelete = { timelineViewModel.delete(event) })
                }
            }
        }
    }
}

@Composable
private fun TimelineEventCard(event: TimelineEvent, onDelete: () -> Unit) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                val (title, detail) = when (event) {
                    is TimelineEvent.FeedingEvent ->
                        "Fed ${event.feeding.ratio}" to event.feeding.flourType
                    is TimelineEvent.ObservationEvent ->
                        "Observed: ${event.observation.riseRating.name.lowercase()}" to observationDetail(event.observation)
                    is TimelineEvent.BakeEvent ->
                        "Baked — ${"★".repeat(event.bake.outcomeRating)}" to (event.bake.levainNotes ?: event.bake.note ?: "")
                }
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (detail.isNotEmpty()) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    formatTimestamp(event.timestampEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun observationDetail(observation: HealthObservation): String = buildList {
    observation.timeToPeakMinutes?.let { add("peaked in ${it / 60}h${it % 60}m") }
    observation.smell?.let { add("smell: ${it.name.lowercase()}") }
    observation.note?.let { add(it) }
}.joinToString(" · ")

@Composable
private fun ProgramCard(
    programStartedAt: java.time.Instant,
    now: java.time.Instant,
    onGraduate: () -> Unit,
) {
    val day = com.ayushojha.levain.domain.StarterProgram.currentDay(programStartedAt, now)
    val complete = com.ayushojha.levain.domain.StarterProgram.isComplete(programStartedAt, now)
    val content = com.ayushojha.levain.domain.StarterProgram.DAYS[day - 1]

    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (complete) "Program complete 🎓" else "Day $day of 7 — ${content.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (complete) {
                    "Seven days done! If it's doubling reliably and smells tangy-sweet, it's officially alive."
                } else {
                    content.instruction
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (complete) {
                FilledTonalButton(onClick = onGraduate, modifier = Modifier.padding(top = 10.dp)) {
                    Text("Graduate to a regular starter")
                }
            }
        }
    }
}
