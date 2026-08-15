package com.ayushojha.levain.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.ayushojha.levain.ui.theme.Palette
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ayushojha.levain.appContainer
import com.ayushojha.levain.domain.DueCalculator
import com.ayushojha.levain.domain.DueStatus

/**
 * Home-screen widget: which starters need feeding, ambient. Reads the same
 * repository as the app; launcher-scheduled periodic refresh keeps it fresh enough.
 */
class DueWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.appContainer
        val dao = container.database.levainDao()
        val now = container.clock.instant()

        val lines = dao.getStarters().mapNotNull { starter ->
            val dueness = DueCalculator.dueness(starter, dao.getLastFeeding(starter.id), now)
            when (dueness.status) {
                DueStatus.OVERDUE -> "${starter.name} — overdue!"
                DueStatus.DUE -> "${starter.name} — due now"
                else -> null
            }
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(Palette.Crumb)
                        .padding(12.dp),
                ) {
                    Text(
                        "Levain",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ColorProvider(Palette.CrustInk),
                        ),
                    )
                    if (lines.isEmpty()) {
                        Text(
                            "Everyone's fed",
                            style = TextStyle(fontSize = 13.sp, color = ColorProvider(Palette.Muted)),
                            modifier = GlanceModifier.padding(top = 4.dp),
                        )
                    } else {
                        lines.forEach { line ->
                            Text(
                                line,
                                style = TextStyle(fontSize = 13.sp, color = ColorProvider(Palette.Rust)),
                                modifier = GlanceModifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

class DueWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DueWidget()
}
