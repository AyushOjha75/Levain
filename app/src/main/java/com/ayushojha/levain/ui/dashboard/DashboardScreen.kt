package com.ayushojha.levain.ui.dashboard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.appContainer
import com.ayushojha.levain.domain.DueStatus
import com.ayushojha.levain.domain.Mood
import com.ayushojha.levain.ui.avatar.StarterAvatar
import com.ayushojha.levain.ui.celebration.ConfettiOverlay
import com.ayushojha.levain.ui.components.EmptyState
import com.ayushojha.levain.ui.components.LevainCard
import com.ayushojha.levain.ui.components.SectionHeader
import com.ayushojha.levain.ui.components.StatePill
import com.ayushojha.levain.ui.components.Tone
import com.ayushojha.levain.ui.containerViewModel
import com.ayushojha.levain.ui.formatAgo
import com.ayushojha.levain.ui.formatDue
import com.ayushojha.levain.ui.moodLine
import com.ayushojha.levain.ui.theme.LevainType
import com.ayushojha.levain.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddStarter: () -> Unit,
    onOpenStarter: (Long) -> Unit,
    onOpenTools: () -> Unit,
    onOpenTroubleshoot: () -> Unit,
    onOpenSettings: () -> Unit,
    onFirstRun: () -> Unit,
) {
    val viewModel = containerViewModel { DashboardViewModel(it.repository, it.clock) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var celebratedMilestone by rememberSaveable { mutableStateOf<String?>(null) }

    // Fresh install: no starters and the wizard never shown → route straight there.
    val context = LocalContext.current
    LaunchedEffect(state.loaded) {
        if (state.loaded && state.cards.isEmpty()) {
            val prefs = context.getSharedPreferences("levain", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("wizard_shown", false)) {
                prefs.edit().putBoolean("wizard_shown", true).apply()
                onFirstRun()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Levain", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onOpenTools) {
                        Icon(Icons.Filled.Calculate, contentDescription = "Baker's tools")
                    }
                    IconButton(onClick = onOpenTroubleshoot) {
                        Icon(Icons.Filled.HealthAndSafety, contentDescription = "Starter doctor")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Backup")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        floatingActionButton = {
            if (state.cards.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddStarter,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("New starter", modifier = Modifier.padding(start = Spacing.s))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.cards.isEmpty()) {
                EmptyState(
                    title = "Nothing rising yet",
                    body = "Levain keeps your cultures alive — when they were fed, how they're doing, and what they baked.",
                    illustration = {
                        StarterAvatar(Mood.CONTENT, Modifier.size(132.dp), isSystemInDarkTheme())
                    },
                    action = {
                        com.ayushojha.levain.ui.components.PrimaryAction(
                            text = "Add your first starter",
                            onClick = onAddStarter,
                            modifier = Modifier.padding(top = Spacing.m),
                        )
                    },
                    modifier = Modifier.fillMaxSize().padding(bottom = Spacing.section),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.l, end = Spacing.l,
                        top = Spacing.s, bottom = Spacing.section + Spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    state.milestoneBanner?.let { banner ->
                        item(key = "milestone") { MilestoneBanner(banner) }
                    }
                    item(key = "starters-header") {
                        SectionHeader("Your starters", Modifier.padding(top = Spacing.s))
                    }
                    items(state.cards, key = { it.starter.id }) { card ->
                        StarterCardItem(card = card, onClick = { onOpenStarter(card.starter.id) })
                    }
                    item(key = "fact") {
                        Column(Modifier.padding(top = Spacing.l)) {
                            SectionHeader("Did you know")
                            FactCard(state.factOfTheDay)
                        }
                    }
                }
            }

            val banner = state.milestoneBanner
            if (banner != null && celebratedMilestone != banner) {
                ConfettiOverlay(onFinished = { celebratedMilestone = banner })
            }
        }
    }
}

@Composable
private fun MilestoneBanner(text: String) {
    LevainCard(tone = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            "Milestone".uppercase(),
            style = LevainType.eyebrow,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun StarterCardItem(card: StarterCard, onClick: () -> Unit) {
    val clock = LocalContext.current.appContainer.clock
    LevainCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarterAvatar(
                mood = card.mood,
                modifier = Modifier.size(76.dp),
                darkTheme = isSystemInDarkTheme(),
            )
            Column(Modifier.weight(1f).padding(start = Spacing.m)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.starter.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    DueBadge(card)
                }
                Text(
                    moodLine(card.starter.name, card.mood),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.hair),
                )
                Row(
                    Modifier.padding(top = Spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    if (card.vitals.feedingStreak >= 3) {
                        Text(
                            "${card.vitals.feedingStreak} on time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        "${card.vitals.ageDays}d old",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    card.lastFeeding?.let {
                        Text(
                            "fed ${formatAgo(it.timestampEpochMs, clock)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DueBadge(card: StarterCard) {
    val clock = LocalContext.current.appContainer.clock
    val (text, tone) = when (card.dueness.status) {
        DueStatus.OVERDUE -> (card.dueness.dueAt?.let { formatDue(it, clock) } ?: "overdue") to Tone.Urgent
        DueStatus.DUE -> "due now" to Tone.Attention
        DueStatus.OK -> (card.dueness.dueAt?.let { formatDue(it, clock) } ?: "") to Tone.Calm
        DueStatus.NEVER_DUE -> "archived" to Tone.Neutral
    }
    if (text.isNotEmpty()) StatePill(text, tone)
}

@Composable
private fun FactCard(fact: String) {
    LevainCard(
        tone = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            fact,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
