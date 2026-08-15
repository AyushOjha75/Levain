package com.ayushojha.levain.ui.starter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ayushojha.levain.appContainer
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.domain.Insights
import com.ayushojha.levain.domain.RiseTrend
import com.ayushojha.levain.domain.StarterProgram
import com.ayushojha.levain.ui.components.LevainCard
import com.ayushojha.levain.ui.components.MetricRow
import com.ayushojha.levain.ui.components.PrimaryAction
import com.ayushojha.levain.ui.components.SectionHeader
import com.ayushojha.levain.ui.containerViewModel
import com.ayushojha.levain.ui.formatTimestamp
import com.ayushojha.levain.ui.theme.LevainType
import com.ayushojha.levain.ui.theme.Spacing
import com.ayushojha.levain.ui.timeline.TimelineEvent
import com.ayushojha.levain.ui.timeline.TimelineViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarterDetailScreen(
    starterId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onLogFeeding: () -> Unit,
    onEditFeeding: (Long) -> Unit,
    onLogObservation: () -> Unit,
    onLogBake: () -> Unit,
) {
    val timelineViewModel = containerViewModel(key = "timeline-$starterId") {
        TimelineViewModel(it.repository, starterId)
    }
    val headerViewModel = containerViewModel(key = "starter-header-$starterId") {
        StarterHeaderViewModel(it.repository, starterId)
    }
    val clock = LocalContext.current.appContainer.clock
    val timeline by timelineViewModel.uiState.collectAsStateWithLifecycle()
    val starter by headerViewModel.starter.collectAsStateWithLifecycle()
    val insights by headerViewModel.insights.collectAsStateWithLifecycle()
    var viewedPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(starter?.name ?: "", style = MaterialTheme.typography.headlineSmall) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.section,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            starter?.programStartedAtEpochMs?.let { startedAt ->
                item(key = "program") {
                    ProgramCard(
                        programStartedAt = Instant.ofEpochMilli(startedAt),
                        now = clock.instant(),
                        onGraduate = headerViewModel::graduate,
                    )
                }
            }

            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    QuickAction("Feed", Icons.Filled.RestaurantMenu, Modifier.weight(1f), onLogFeeding)
                    QuickAction("Observe", Icons.Filled.Visibility, Modifier.weight(1f), onLogObservation)
                    QuickAction("Bake", Icons.Filled.LocalFireDepartment, Modifier.weight(1f), onLogBake)
                }
            }

            insights?.let { data ->
                item(key = "insights") {
                    Column(Modifier.padding(top = Spacing.s)) {
                        SectionHeader("Insights")
                        InsightsCard(data)
                    }
                }
            }

            item(key = "history-header") {
                SectionHeader("History", Modifier.padding(top = Spacing.s))
            }

            if (timeline.events.isEmpty()) {
                item(key = "history-empty") {
                    LevainCard(tone = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Nothing logged yet. Feed it, or note how it looks — this is where its story builds up.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(
                timeline.events,
                key = { "${it::class.simpleName}-${it.timestampEpochMs}-${it.hashCode()}" },
            ) { event ->
                TimelineEventCard(
                    event = event,
                    onDelete = { timelineViewModel.delete(event) },
                    onEditFeeding = onEditFeeding,
                    onViewPhoto = { viewedPhoto = it },
                )
            }
        }
    }

    viewedPhoto?.let { path ->
        Dialog(onDismissRequest = { viewedPhoto = null }) {
            val photoStore = LocalContext.current.appContainer.photoStore
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                AsyncImage(
                    model = photoStore.fileFor(path),
                    contentDescription = "Photo",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(vertical = Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun InsightsCard(insights: Insights) {
    LevainCard(modifier = Modifier.fillMaxWidth()) {
        val hasAny = insights.avgGapHours != null || insights.onTimePercent != null ||
            insights.riseTrend != null || insights.bakeCount > 0
        if (!hasAny) {
            Text(
                "Log a few feedings and observations and this starter's patterns show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@LevainCard
        }
        insights.avgGapHours?.let { MetricRow("Feeding rhythm", "every ~${it}h") }
        insights.onTimePercent?.let { MetricRow("On time", "$it%") }
        insights.riseTrend?.let {
            MetricRow(
                "Rise trend",
                when (it) {
                    RiseTrend.IMPROVING -> "improving"
                    RiseTrend.STEADY -> "steady"
                    RiseTrend.DECLINING -> "declining"
                },
            )
        }
        if (insights.bakeCount > 0) {
            MetricRow("Bakes", "${insights.bakeCount} · avg ${"%.1f".format(insights.avgBakeRating)}★")
        }
        if (insights.riseTrend == RiseTrend.DECLINING) {
            Text(
                "Rise has been falling off. The starter doctor has a plan for that.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
    }
}

@Composable
private fun ProgramCard(programStartedAt: Instant, now: Instant, onGraduate: () -> Unit) {
    val day = StarterProgram.currentDay(programStartedAt, now)
    val complete = StarterProgram.isComplete(programStartedAt, now)
    val content = StarterProgram.DAYS[day - 1]

    LevainCard(tone = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            (if (complete) "Program complete" else "Day $day of 7").uppercase(),
            style = LevainType.eyebrow,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            if (complete) "It's alive" else content.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Text(
            if (complete) {
                "Seven days done. If it doubles reliably and smells tangy-sweet, it has graduated."
            } else {
                content.instruction
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(top = Spacing.s),
        )
        if (complete) {
            PrimaryAction(
                text = "Graduate to a regular starter",
                onClick = onGraduate,
                modifier = Modifier.padding(top = Spacing.m),
            )
        }
    }
}

@Composable
private fun TimelineEventCard(
    event: TimelineEvent,
    onDelete: () -> Unit,
    onEditFeeding: (Long) -> Unit,
    onViewPhoto: (String) -> Unit,
) {
    val photoPath = when (event) {
        is TimelineEvent.ObservationEvent -> event.observation.photoPath
        is TimelineEvent.BakeEvent -> event.bake.photoPath
        else -> null
    }
    LevainCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            photoPath?.let { path ->
                val photoStore = LocalContext.current.appContainer.photoStore
                AsyncImage(
                    model = photoStore.fileFor(path),
                    contentDescription = "Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(end = Spacing.m)
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onViewPhoto(path) },
                )
            }
            Column(Modifier.weight(1f)) {
                val (title, detail) = when (event) {
                    is TimelineEvent.FeedingEvent ->
                        "Fed ${event.feeding.ratio}" to event.feeding.flourType
                    is TimelineEvent.ObservationEvent ->
                        "Observed: ${event.observation.riseRating.name.lowercase()}" to observationDetail(event.observation)
                    is TimelineEvent.BakeEvent ->
                        "Baked ${"★".repeat(event.bake.outcomeRating)}" to (event.bake.levainNotes ?: event.bake.note ?: "")
                }
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (detail.isNotEmpty()) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.hair),
                    )
                }
                Text(
                    formatTimestamp(event.timestampEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            if (event is TimelineEvent.FeedingEvent) {
                IconButton(onClick = { onEditFeeding(event.feeding.id) }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit feeding",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
