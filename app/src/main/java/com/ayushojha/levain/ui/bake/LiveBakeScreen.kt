package com.ayushojha.levain.ui.bake

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.data.BakeStep
import com.ayushojha.levain.data.StepKind
import com.ayushojha.levain.ui.components.LevainCard
import com.ayushojha.levain.ui.components.PrimaryAction
import com.ayushojha.levain.ui.components.SecondaryAction
import com.ayushojha.levain.ui.components.SectionHeader
import com.ayushojha.levain.ui.components.StatePill
import com.ayushojha.levain.ui.components.Tone
import com.ayushojha.levain.ui.containerViewModel
import com.ayushojha.levain.ui.formatTimestamp
import com.ayushojha.levain.ui.theme.BakeRegister
import com.ayushojha.levain.ui.theme.LevainType
import com.ayushojha.levain.ui.theme.Spacing
import java.time.Duration

/**
 * The one screen that switches into the Ember register: dark ground, huge
 * tabular numerals, ember means needs-you. This is read at arm's length on a
 * floury counter, which is a different job from every other screen in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveBakeScreen(bakeId: Long, onBack: () -> Unit, onFinished: () -> Unit) {
    val viewModel = containerViewModel(key = "live-bake-$bakeId") {
        LiveBakeViewModel(it.bakeSessions, it.clock, bakeId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmAbandon by rememberSaveable { mutableStateOf(false) }
    var rating by rememberSaveable { mutableStateOf(0) }

    BakeRegister {
        Scaffold(
            topBar = {
                com.ayushojha.levain.ui.components.LevainTopBar(
                    title = { Text("Step ${state.done + 1} of ${state.steps.size}") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { confirmAbandon = true }) { Text("Abandon") }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item("current") {
                    val current = state.current
                    if (current == null) {
                        FinishCard(rating, { rating = it }) { viewModel.finish(rating); onFinished() }
                    } else {
                        CurrentStepCard(
                            step = current,
                            remaining = state.remaining,
                            overdue = state.overdue,
                            held = state.held,
                            onDone = { viewModel.complete(current) },
                            onExtend = { viewModel.extend(current, 15) },
                        )
                    }
                }

                item("hold") {
                    if (state.held) {
                        PrimaryAction("Take it out — resume the bake", { viewModel.resume() })
                    } else if (state.current != null) {
                        SecondaryAction("Hold — it's going in the fridge", { viewModel.hold() })
                    }
                }

                state.projectedEnd?.let { end ->
                    item("projection") {
                        Text(
                            if (state.held) {
                                "Held. The clock stops until you resume."
                            } else {
                                "Out of the oven around ${formatTimestamp(end.toEpochMilli())}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.s),
                        )
                    }
                }

                item("all-steps") { SectionHeader("The whole bake", Modifier.padding(top = Spacing.l)) }

                items(state.steps, key = { it.id }) { step ->
                    StepRow(
                        step = step,
                        isCurrent = step.id == state.current?.id,
                        onToggle = {
                            if (step.completedAtEpochMs == null) viewModel.complete(step)
                            else viewModel.uncomplete(step)
                        },
                    )
                }
            }
        }

        if (confirmAbandon) {
            AlertDialog(
                onDismissRequest = { confirmAbandon = false },
                title = { Text("Abandon this bake?") },
                text = { Text("It stays in your history as abandoned — a failed bake is still worth knowing about.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.abandon(); confirmAbandon = false; onBack() }) {
                        Text("Abandon")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmAbandon = false }) { Text("Keep baking") }
                },
            )
        }
    }
}

@Composable
private fun CurrentStepCard(
    step: BakeStep,
    remaining: Duration?,
    overdue: Boolean,
    held: Boolean,
    onDone: () -> Unit,
    onExtend: () -> Unit,
) {
    LevainCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (step.kind) {
                    StepKind.TIMED -> "On the clock"
                    StepKind.JUDGED -> "Your call"
                    StepKind.ACTION -> "Do this now"
                }.uppercase(),
                style = LevainType.eyebrow,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (overdue && !held) StatePill("overdue", Tone.Urgent)
        }

        if (step.kind != StepKind.ACTION && remaining != null) {
            Text(
                formatCountdown(remaining),
                style = LevainType.numeral,
                color = if (overdue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
                textAlign = TextAlign.Center,
            )
        }

        Text(step.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            step.instruction,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )

        step.cue?.let { cue ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
            ) {
                Column(Modifier.padding(Spacing.m)) {
                    Text("Look for".uppercase(), style = LevainType.eyebrow)
                    Text(cue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.xs))
                }
            }
        }

        PrimaryAction(
            text = if (step.kind == StepKind.JUDGED) "It's ready" else "Done",
            onClick = onDone,
            modifier = Modifier.padding(top = Spacing.m),
        )
        if (step.kind != StepKind.ACTION) {
            SecondaryAction("Give it 15 more minutes", onExtend, Modifier.padding(top = Spacing.s))
        }
    }
}

@Composable
private fun FinishCard(rating: Int, onRate: (Int) -> Unit, onFinish: () -> Unit) {
    LevainCard(modifier = Modifier.fillMaxWidth()) {
        Text("That's the bake", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "How did it turn out?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.s),
        )
        Row {
            (1..5).forEach { star ->
                IconButton(onClick = { onRate(star) }) {
                    Icon(
                        if (rating >= star) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "$star star",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        PrimaryAction("Save the bake", onFinish, Modifier.padding(top = Spacing.s), enabled = rating > 0)
    }
}

@Composable
private fun StepRow(step: BakeStep, isCurrent: Boolean, onToggle: () -> Unit) {
    val done = step.completedAtEpochMs != null
    LevainCard(
        onClick = onToggle,
        tone = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(Spacing.m),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (done) "✓" else "○",
                style = MaterialTheme.typography.titleMedium,
                color = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Spacing.m),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                step.completedAtEpochMs?.let {
                    Text(
                        formatTimestamp(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatCountdown(remaining: Duration): String {
    val abs = remaining.abs()
    val hours = abs.toHours()
    val minutes = abs.toMinutesPart()
    val seconds = abs.toSecondsPart()
    val body = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
    return if (remaining.isNegative) "+$body" else body
}
