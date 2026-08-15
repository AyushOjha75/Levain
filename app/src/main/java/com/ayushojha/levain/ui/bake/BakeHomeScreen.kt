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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.data.Recipe
import com.ayushojha.levain.ui.components.LevainCard
import com.ayushojha.levain.ui.components.PrimaryAction
import com.ayushojha.levain.ui.components.SectionHeader
import com.ayushojha.levain.ui.components.StatePill
import com.ayushojha.levain.ui.components.Tone
import com.ayushojha.levain.ui.containerViewModel
import com.ayushojha.levain.ui.theme.LevainType
import com.ayushojha.levain.ui.theme.Spacing

/**
 * Home. If a bake is running, that is the only thing on this screen that
 * matters — mid-bake, nothing else you could be doing competes with it.
 */
@Composable
fun BakeHomeScreen(
    onOpenBake: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = containerViewModel { BakeHomeViewModel(it.bakeSessions, it.repository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        state.active?.let { active ->
            item("active") {
                LevainCard(
                    onClick = { onOpenBake(active.id) },
                    tone = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Baking now".uppercase(),
                            style = LevainType.eyebrow,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        if (active.status == com.ayushojha.levain.data.BakeStatus.HELD) {
                            StatePill("held", Tone.Neutral)
                        }
                    }
                    Text(
                        state.recipes.firstOrNull { it.id == active.recipeId }?.name ?: "A bake",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    Text(
                        "Tap to pick up where you left off.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        item("header") {
            SectionHeader(
                if (state.active == null) "Start a bake" else "Or start another",
                Modifier.padding(top = Spacing.s),
            )
        }

        items(state.recipes, key = { it.id }) { recipe ->
            RecipeCard(
                recipe = recipe,
                blocked = recipe.requiresStarter && !state.hasStarter,
                onStart = { viewModel.start(recipe, onOpenBake) },
            )
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, blocked: Boolean, onStart: () -> Unit) {
    LevainCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                recipe.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (!recipe.requiresStarter) StatePill("no starter", Tone.Calm)
        }
        Text(
            recipe.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Text(
            "Makes ${recipe.referenceBatch}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s),
        )
        if (blocked) {
            Text(
                "Needs a starter — add one under Starters first, or bake the yeasted focaccia instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
        PrimaryAction(
            text = "Start this bake",
            onClick = onStart,
            enabled = !blocked,
            modifier = Modifier.padding(top = Spacing.m),
        )
    }
}
