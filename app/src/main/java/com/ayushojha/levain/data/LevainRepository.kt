package com.ayushojha.levain.data

import com.ayushojha.levain.reminders.ReminderCoordinator
import java.time.Clock
import kotlinx.coroutines.flow.Flow

/**
 * Single write surface over the database. Every mutation that can change a
 * Starter's dueness reschedules reminders through the one choke point.
 */
class LevainRepository(
    private val dao: LevainDao,
    private val reminders: ReminderCoordinator,
    private val clock: Clock,
) {

    fun observeStarters(): Flow<List<Starter>> = dao.observeStarters()
    fun observeStarter(id: Long): Flow<Starter?> = dao.observeStarter(id)
    fun observeLastFeedings(): Flow<List<Feeding>> = dao.observeLastFeedings()
    fun observeLastObservations(): Flow<List<HealthObservation>> = dao.observeLastObservations()
    fun observeFeedings(starterId: Long): Flow<List<Feeding>> = dao.observeFeedings(starterId)
    fun observeObservations(starterId: Long): Flow<List<HealthObservation>> = dao.observeObservations(starterId)
    fun observeBakes(starterId: Long): Flow<List<Bake>> = dao.observeBakes(starterId)

    suspend fun getStarter(id: Long): Starter? = dao.getStarter(id)
    suspend fun getLastFeeding(starterId: Long): Feeding? = dao.getLastFeeding(starterId)

    suspend fun createStarter(starter: Starter): Long {
        val id = dao.insertStarter(starter)
        reminders.reschedule(clock.instant())
        return id
    }

    suspend fun updateStarter(starter: Starter) {
        dao.updateStarter(starter)
        reminders.reschedule(clock.instant())
    }

    suspend fun deleteStarter(id: Long) {
        dao.deleteStarter(id)
        reminders.reschedule(clock.instant())
    }

    suspend fun logFeeding(feeding: Feeding): Long {
        val id = dao.insertFeeding(feeding)
        reminders.reschedule(clock.instant())
        return id
    }

    suspend fun deleteFeeding(feeding: Feeding) {
        dao.deleteFeeding(feeding)
        reminders.reschedule(clock.instant())
    }

    suspend fun logObservation(observation: HealthObservation): Long = dao.insertObservation(observation)
    suspend fun deleteObservation(observation: HealthObservation) = dao.deleteObservation(observation)

    suspend fun logBake(bake: Bake): Long = dao.insertBake(bake)
    suspend fun deleteBake(bake: Bake) = dao.deleteBake(bake)

    suspend fun onAlarmFired() = reminders.onAlarmFired(clock.instant())
}
