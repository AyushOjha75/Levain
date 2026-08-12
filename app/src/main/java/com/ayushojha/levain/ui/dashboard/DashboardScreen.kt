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
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ayushojha.levain.ui.containerViewModel
import com.ayushojha.levain.ui.formatAgo
import com.ayushojha.levain.ui.formatDue
import com.ayushojha.levain.ui.moodLine

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

    // Fresh install: no starters and the wizard never shown → route straight there.
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(state.loaded) {
        if (state.loaded && state.cards.isEmpty()) {
            val prefs = context.getSharedPreferences("levain", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("wizard_shown", false)) {
                prefs.edit().putBoolean("wizard_shown", true).apply()
                onFirstRun()
            }
        }
    }
    var celebratedMilestone by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf<String?>(null)
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
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddStarter,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add starter")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.cards.isEmpty()) {
                EmptyDashboard()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.milestoneBanner?.let { banner ->
                        item(key = "milestone") { MilestoneBanner(banner) }
                    }
                    items(state.cards, key = { it.starter.id }) { card ->
                        StarterCardItem(card = card, onClick = { onOpenStarter(card.starter.id) })
                    }
                    item(key = "fact") {
                        FactCard(state.factOfTheDay)
                    }
                }
            }

            // One confetti burst per milestone per dashboard visit.
            val banner = state.milestoneBanner
            if (banner != null && celebratedMilestone != banner) {
                ConfettiOverlay(onFinished = { celebratedMilestone = banner })
            }
        }
    }
}

@Composable
private fun EmptyDashboard() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StarterAvatar(
                mood = Mood.CONTENT,
                modifier = Modifier.size(120.dp),
                darkTheme = isSystemInDarkTheme(),
            )
            Text(
                "No starters yet",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Add your first starter — it'll live right here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun MilestoneBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun StarterCardItem(card: StarterCard, onClick: () -> Unit) {
    val clock = LocalContext.current.appContainer.clock
    Card(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StarterAvatar(
                mood = card.mood,
                modifier = Modifier.size(72.dp),
                darkTheme = isSystemInDarkTheme(),
            )
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.starter.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    DueBadge(card)
                }
                Text(
                    moodLine(card.starter.name, card.mood),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (card.vitals.feedingStreak >= 3) {
                        Text(
                            "🔥 ${card.vitals.feedingStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
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
    val (text, container, content) = when (card.dueness.status) {
        DueStatus.OVERDUE -> Triple(
            card.dueness.dueAt?.let { formatDue(it, clock) } ?: "overdue",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
        )
        DueStatus.DUE -> Triple(
            "due now",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        DueStatus.OK -> Triple(
            card.dueness.dueAt?.let { formatDue(it, clock) } ?: "",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        DueStatus.NEVER_DUE -> Triple(
            "archived",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (text.isNotEmpty()) {
        Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FactCard(fact: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Did you know?", style = MaterialTheme.typography.titleSmall)
            Text(fact, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
