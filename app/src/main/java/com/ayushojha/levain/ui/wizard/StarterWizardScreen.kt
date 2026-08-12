package com.ayushojha.levain.ui.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.domain.Mood
import com.ayushojha.levain.ui.avatar.StarterAvatar
import com.ayushojha.levain.ui.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarterWizardScreen(onDone: (Long?) -> Unit) {
    val viewModel = containerViewModel { StarterWizardViewModel(it.repository, it.clock) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.step) {
        if (state.step == WizardStep.DONE) onDone(state.createdStarterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { if (state.step == WizardStep.CHOICE) onDone(null) else viewModel.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(targetState = state.step, label = "wizard-step") { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (step) {
                    WizardStep.CHOICE -> ChoiceStep(viewModel)
                    WizardStep.NAME -> NameStep(state, viewModel, forProgram = false)
                    WizardStep.HOME -> HomeStep(state, viewModel)
                    WizardStep.FIRST_FEED -> FirstFeedStep(state, viewModel)
                    WizardStep.PROGRAM_NAME -> NameStep(state, viewModel, forProgram = true)
                    WizardStep.DONE -> {}
                }
            }
        }
    }
}

@Composable
private fun ChoiceStep(viewModel: StarterWizardViewModel) {
    StarterAvatar(Mood.CONTENT, Modifier.size(140.dp), isSystemInDarkTheme())
    Text("Welcome to Levain", style = MaterialTheme.typography.displaySmall)
    Text(
        "Every great loaf starts with a living jar of flour and water. Do you already have one?",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = { viewModel.chooseHasStarter(true) }, modifier = Modifier.fillMaxWidth()) {
        Text("Yes — my starter is alive and bubbling")
    }
    OutlinedButton(onClick = { viewModel.chooseHasStarter(false) }, modifier = Modifier.fillMaxWidth()) {
        Text("No — grow one with me (7 days)")
    }
}

@Composable
private fun NameStep(state: WizardState, viewModel: StarterWizardViewModel, forProgram: Boolean) {
    StarterAvatar(if (forProgram) Mood.SLEEPY else Mood.BEAMING, Modifier.size(120.dp), isSystemInDarkTheme())
    Text(
        if (forProgram) "Name your future starter" else "What's its name?",
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        if (forProgram) {
            "In seven days of flour, water and patience, this jar will be alive. Naming it first is tradition."
        } else {
            "Every starter deserves a name — it's going to outlive your houseplants."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = state.name,
        onValueChange = viewModel::setName,
        label = { Text("Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { if (forProgram) viewModel.startProgram() else viewModel.advanceFromName() },
        enabled = state.canAdvanceName,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (forProgram) "Start day 1 🌱" else "Next")
    }
}

@Composable
private fun HomeStep(state: WizardState, viewModel: StarterWizardViewModel) {
    Text("Where does ${state.name.trim()} live?", style = MaterialTheme.typography.headlineMedium)
    Text(
        "This sets the feeding rhythm — counter starters eat daily, fridge starters weekly.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.home == LifecycleState.ACTIVE,
            onClick = { viewModel.setHome(LifecycleState.ACTIVE) },
            label = { Text("On the counter") },
        )
        FilterChip(
            selected = state.home == LifecycleState.DORMANT,
            onClick = { viewModel.setHome(LifecycleState.DORMANT) },
            label = { Text("In the fridge") },
        )
    }
    if (state.home == LifecycleState.ACTIVE) {
        OutlinedTextField(
            value = state.activeIntervalHours.toString(),
            onValueChange = { it.toIntOrNull()?.let(viewModel::setActiveInterval) },
            label = { Text("Feed every (hours)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        OutlinedTextField(
            value = state.dormantIntervalHours.toString(),
            onValueChange = { it.toIntOrNull()?.let(viewModel::setDormantInterval) },
            label = { Text("Feed every (hours)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Button(onClick = viewModel::advanceFromHome, modifier = Modifier.fillMaxWidth()) {
        Text("Next")
    }
}

@Composable
private fun FirstFeedStep(state: WizardState, viewModel: StarterWizardViewModel) {
    StarterAvatar(Mood.HUNGRY, Modifier.size(120.dp), isSystemInDarkTheme())
    Text("When did ${state.name.trim()} last eat?", style = MaterialTheme.typography.headlineMedium)
    Text(
        "If you feed it now, the app starts the clock from this moment. You can also skip and log the next feeding when it happens.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = state.ratio,
        onValueChange = viewModel::setRatio,
        label = { Text("Ratio (starter : flour : water)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.flourType,
        onValueChange = viewModel::setFlourType,
        label = { Text("Flour") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { viewModel.finishOnboarding(fedJustNow = true) }, modifier = Modifier.fillMaxWidth()) {
        Text("I'm feeding it now 🥄")
    }
    OutlinedButton(onClick = { viewModel.finishOnboarding(fedJustNow = false) }, modifier = Modifier.fillMaxWidth()) {
        Text("Skip — I'll log the next one")
    }
}
