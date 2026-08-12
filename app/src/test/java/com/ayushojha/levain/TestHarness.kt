package com.ayushojha.levain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ayushojha.levain.data.LevainDatabase
import com.ayushojha.levain.data.LevainRepository
import com.ayushojha.levain.reminders.DueScheduler
import com.ayushojha.levain.reminders.NotificationPresenter
import com.ayushojha.levain.reminders.ReminderCoordinator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** A Clock tests can move forward. */
class MutableClock(private var now: Instant) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    fun advanceBy(duration: Duration) { now = now.plus(duration) }
    fun setTo(instant: Instant) { now = instant }
}

/** Records scheduled alarms instead of touching AlarmManager. */
class FakeScheduler : DueScheduler {
    val scheduled = mutableListOf<Instant>()
    var cancelled = 0
    val lastScheduledAt: Instant? get() = scheduled.lastOrNull()
    override fun scheduleExact(at: Instant) { scheduled += at }
    override fun cancel() { cancelled++ }
}

/** Records notification requests instead of touching NotificationManager. */
class FakePresenter : NotificationPresenter {
    val notifications = mutableListOf<List<String>>()
    override fun showDueNotification(starterNames: List<String>) { notifications += starterNames }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val dispatcher = StandardTestDispatcher()
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Awaits emissions until one matches — flows may emit intermediate states first. */
suspend fun <T> app.cash.turbine.ReceiveTurbine<T>.awaitMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}

/** Everything a ViewModel test needs, wired over an in-memory Room database. */
class TestApp(epoch: Instant = Instant.parse("2026-08-12T08:00:00Z")) {
    val clock = MutableClock(epoch)
    val scheduler = FakeScheduler()
    val presenter = FakePresenter()
    val db: LevainDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        LevainDatabase::class.java,
    )
        // Direct executors keep Room's suspend calls off real thread pools, so
        // advanceUntilIdle() is deterministic — no CI-only races.
        .setQueryExecutor(Runnable::run)
        .setTransactionExecutor(Runnable::run)
        .allowMainThreadQueries()
        .build()
    val coordinator = ReminderCoordinator(db.levainDao(), scheduler, presenter)
    val repository = LevainRepository(db.levainDao(), coordinator, clock)

    fun close() = db.close()
}
