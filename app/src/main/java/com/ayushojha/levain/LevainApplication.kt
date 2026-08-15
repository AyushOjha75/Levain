package com.ayushojha.levain

import android.app.Application
import android.content.Context
import com.ayushojha.levain.data.LevainDatabase
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.data.PhotoStore
import com.ayushojha.levain.reminders.AlarmDueScheduler
import com.ayushojha.levain.reminders.DueNotificationPresenter
import com.ayushojha.levain.reminders.ReminderCoordinator
import java.time.Clock
import kotlinx.coroutines.launch

/** Manual DI: one graph, built lazily, no framework. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    val database: LevainDatabase = LevainDatabase.build(context)
    val photoStore = PhotoStore(context)
    private val scheduler = AlarmDueScheduler(context)
    private val presenter = DueNotificationPresenter(context)
    private val coordinator = ReminderCoordinator(database.levainDao(), scheduler, presenter)
    val repository = LevainRepository(database.levainDao(), coordinator, clock)
    val backupManager = com.ayushojha.levain.data.BackupManager(
        database,
        java.io.File(context.filesDir, "photos"),
    )
    val bakeSessions = com.ayushojha.levain.data.BakeSessions(
        database,
        AlarmDueScheduler(context, com.ayushojha.levain.reminders.AlarmPurpose.BAKE_STEP_DUE),
        clock,
    )
    val recipeCatalog = com.ayushojha.levain.data.RecipeCatalog(
        database,
        com.ayushojha.levain.data.AssetRecipeSource(context.assets),
    )
}

class LevainApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Bundled recipes are content: load them into Room on first run and
        // whenever a shipped contentVersion moves. Cheap and idempotent when
        // everything is already current.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { container.recipeCatalog.seed() }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as LevainApplication).container
