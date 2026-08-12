package com.ayushojha.levain.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.appContainer
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.domain.DueStatus
import com.ayushojha.levain.ui.containerViewModel
import com.ayushojha.levain.ui.formatAgo
import com.ayushojha.levain.ui.formatDue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddStarter: () -> Unit,
    onOpenStarter: (Long) -> Unit,
) {
    val viewModel = containerViewModel { DashboardViewModel(it.repository, it.clock) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Levain", fontWeight = FontWeight.SemiBold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStarter) {
                Icon(Icons.Filled.Add, contentDescription = "Add starter")
            }
        },
    ) { padding ->
        if (state.cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No starters yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Add your first culture to start tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.cards, key = { it.starter.id }) { card ->
                    StarterCardItem(card = card, onClick = { onOpenStarter(card.starter.id) })
                }
            }
        }
    }
}

@Composable
private fun StarterCardItem(card: StarterCard, onClick: () -> Unit) {
    val clock = LocalContext.current.appContainer.clock
    Card(onClick = onClick, colors = CardDefaults.cardColors()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.starter.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                DueBadge(card)
            }
            Spacer(Modifier.width(0.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StateChip(card.starter.state)
                card.lastFeeding?.let {
                    Text(
                        "fed ${formatAgo(it.timestampEpochMs, clock)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                card.lastObservation?.let {
                    Text(
                        it.riseRating.name.lowercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
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
private fun StateChip(state: LifecycleState) {
    Text(
        when (state) {
            LifecycleState.ACTIVE -> "counter"
            LifecycleState.DORMANT -> "fridge"
            LifecycleState.ARCHIVED -> "archived"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
