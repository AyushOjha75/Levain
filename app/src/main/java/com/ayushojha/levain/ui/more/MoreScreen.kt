package com.ayushojha.levain.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ayushojha.levain.ui.components.LevainCard
import com.ayushojha.levain.ui.components.SectionHeader
import com.ayushojha.levain.ui.theme.Spacing

/**
 * The reference shelf. These used to be unlabelled icons in a top bar, which
 * made every one of them invisible unless you already knew it was there.
 */
@Composable
fun MoreScreen(
    onOpenTools: () -> Unit,
    onOpenDoctor: () -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        item { SectionHeader("Tools") }
        item {
            MoreRow(
                title = "Baker's tools",
                body = "Levain build and baker's percentage calculators.",
                onClick = onOpenTools,
            )
        }
        item {
            MoreRow(
                title = "Starter doctor",
                body = "Something's wrong with your starter — work out what, and what to do.",
                onClick = onOpenDoctor,
            )
        }
        item { SectionHeader("Your data", Modifier.padding(top = Spacing.l)) }
        item {
            MoreRow(
                title = "Backup",
                body = "Export everything — starters, history and photos — to a file you keep.",
                onClick = onOpenBackup,
            )
        }
    }
}

@Composable
private fun MoreRow(title: String, body: String, onClick: () -> Unit) {
    LevainCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
