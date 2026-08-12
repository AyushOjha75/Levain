package com.ayushojha.levain.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ayushojha.levain.AppContainer
import com.ayushojha.levain.appContainer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Builds a ViewModel from the app container without a DI framework. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalContext.current.appContainer
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

private val eventFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")

fun formatTimestamp(epochMs: Long): String =
    eventFormatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

/** "3h ago", "2d ago" — dashboard-card shorthand. */
fun formatAgo(epochMs: Long, clock: Clock): String {
    val elapsed = Duration.between(Instant.ofEpochMilli(epochMs), clock.instant())
    return when {
        elapsed.toMinutes() < 1 -> "just now"
        elapsed.toHours() < 1 -> "${elapsed.toMinutes()}m ago"
        elapsed.toDays() < 1 -> "${elapsed.toHours()}h ago"
        else -> "${elapsed.toDays()}d ago"
    }
}

/** "in 5h", "in 2d", "3h overdue" — due-status shorthand. */
fun formatDue(dueAt: Instant, clock: Clock): String {
    val until = Duration.between(clock.instant(), dueAt)
    val abs = until.abs()
    val span = when {
        abs.toHours() < 1 -> "${abs.toMinutes()}m"
        abs.toDays() < 1 -> "${abs.toHours()}h"
        else -> "${abs.toDays()}d"
    }
    return if (until.isNegative) "$span overdue" else "due in $span"
}
