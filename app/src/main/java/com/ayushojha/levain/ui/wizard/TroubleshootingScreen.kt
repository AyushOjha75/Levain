package com.ayushojha.levain.ui.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayushojha.levain.domain.Mood
import com.ayushojha.levain.domain.TroubleshootingNode
import com.ayushojha.levain.ui.avatar.StarterAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(onBack: () -> Unit) {
    val viewModel: TroubleshootingViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Starter doctor") },
                navigationIcon = {
                    IconButton(onClick = { if (state.canGoBack) viewModel.back() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(targetState = state.node, label = "troubleshoot") { node ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (node) {
                    is TroubleshootingNode.Question -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarterAvatar(Mood.SLEEPY, Modifier.size(80.dp), isSystemInDarkTheme())
                            Text(
                                node.text,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                        node.options.forEach { (label, next) ->
                            OutlinedButton(
                                onClick = { viewModel.select(next) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(label)
                            }
                        }
                    }
                    is TroubleshootingNode.Diagnosis -> {
                        Text(node.title, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            node.explanation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Card {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("The plan", style = MaterialTheme.typography.titleMedium)
                                node.plan.forEachIndexed { i, step ->
                                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Button(onClick = { viewModel.restart() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Check another symptom")
                        }
                    }
                }
            }
        }
    }
}
