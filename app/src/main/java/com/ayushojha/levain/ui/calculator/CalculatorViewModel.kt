package com.ayushojha.levain.ui.calculator

import androidx.lifecycle.ViewModel
import com.ayushojha.levain.domain.BakersPercentages
import com.ayushojha.levain.domain.DoughMath
import com.ayushojha.levain.domain.LevainBuild
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculatorState(
    // Levain build inputs
    val targetGrams: Int = 200,
    val hydrationPct: Int = 100,
    val inoculationPct: Int = 20,
    // Baker's % inputs
    val doughFlour: Int = 500,
    val doughWater: Int = 350,
    val doughSalt: Double = 10.0,
    val doughLevain: Int = 100,
) {
    val levainBuild: LevainBuild get() = DoughMath.levainBuild(targetGrams, hydrationPct, inoculationPct)
    val percentages: BakersPercentages get() = DoughMath.bakersPercentages(doughFlour, doughWater, doughSalt, doughLevain)
}

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorState())
    val uiState: StateFlow<CalculatorState> = _uiState.asStateFlow()

    fun setTargetGrams(v: Int) = _uiState.update { it.copy(targetGrams = v.coerceIn(1, 5000)) }
    fun setHydrationPct(v: Int) = _uiState.update { it.copy(hydrationPct = v.coerceIn(50, 200)) }
    fun setInoculationPct(v: Int) = _uiState.update { it.copy(inoculationPct = v.coerceIn(1, 100)) }
    fun setDoughFlour(v: Int) = _uiState.update { it.copy(doughFlour = v.coerceIn(0, 100_000)) }
    fun setDoughWater(v: Int) = _uiState.update { it.copy(doughWater = v.coerceIn(0, 100_000)) }
    fun setDoughSalt(v: Double) = _uiState.update { it.copy(doughSalt = v.coerceIn(0.0, 1000.0)) }
    fun setDoughLevain(v: Int) = _uiState.update { it.copy(doughLevain = v.coerceIn(0, 100_000)) }
}
