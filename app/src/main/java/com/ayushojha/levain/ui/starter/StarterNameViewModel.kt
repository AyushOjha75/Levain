package com.ayushojha.levain.ui.starter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayushojha.levain.data.LevainRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StarterNameViewModel(repository: LevainRepository, starterId: Long) : ViewModel() {
    val name: StateFlow<String> = repository.observeStarter(starterId)
        .map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
}
