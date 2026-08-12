package com.ayushojha.levain.data

import androidx.room.withTransaction
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * The manual "export everything" escape hatch: one zip holding data.json
 * (all four tables) plus every photo. Import wipes and restores — a backup
 * is only a backup if it's provably restorable, so this round-trips in tests.
 */
class BackupManager(
    private val db: LevainDatabase,
    private val photosDir: File,
) {

    private val dao: LevainDao get() = db.levainDao()

    companion object {
        const val FORMAT_VERSION = 1
    }

    suspend fun export(out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(exportJson().toString().toByteArray())
            zip.closeEntry()

            photosDir.listFiles()?.forEach { photo ->
                zip.putNextEntry(ZipEntry("photos/${photo.name}"))
                photo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Replaces ALL current data with the archive's contents. */
    suspend fun import(input: InputStream) {
        var data: JSONObject? = null
        val photos = mutableMapOf<String, ByteArray>()

        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "data.json" -> data = JSONObject(zip.readBytes().decodeToString())
                    entry.name.startsWith("photos/") ->
                        photos[entry.name.removePrefix("photos/")] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }

        val json = requireNotNull(data) { "Not a Levain backup: data.json missing" }
        require(json.optInt("version", Int.MAX_VALUE) <= FORMAT_VERSION) {
            "Backup was made by a newer Levain — update the app first"
        }
        // Atomic: a bad row rolls the whole import back, current data intact.
        db.withTransaction { importJson(json) }

        photosDir.mkdirs()
        photosDir.listFiles()?.forEach { it.delete() }
        photos.forEach { (name, bytes) ->
            // Zip entry names are attacker-controlled: keep writes inside photosDir.
            val target = File(photosDir, File(name).name)
            target.writeBytes(bytes)
        }
    }

    private suspend fun exportJson(): JSONObject = JSONObject().apply {
        put("version", FORMAT_VERSION)
        put("starters", JSONArray().apply {
            dao.getStarters().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("state", s.state.name)
                    put("activeIntervalHours", s.activeIntervalHours)
                    put("dormantIntervalHours", s.dormantIntervalHours)
                    put("createdAtEpochMs", s.createdAtEpochMs)
                    putOpt("lastNotifiedDueAtEpochMs", s.lastNotifiedDueAtEpochMs)
                    putOpt("programStartedAtEpochMs", s.programStartedAtEpochMs)
                })
            }
        })
        put("feedings", JSONArray().apply {
            dao.getAllFeedings().forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id); put("starterId", f.starterId)
                    put("timestampEpochMs", f.timestampEpochMs)
                    put("ratio", f.ratio); put("flourType", f.flourType)
                    putOpt("intervalHoursAtFeeding", f.intervalHoursAtFeeding)
                })
            }
        })
        put("observations", JSONArray().apply {
            dao.getAllObservations().forEach { o ->
                put(JSONObject().apply {
                    put("id", o.id); put("starterId", o.starterId)
                    put("timestampEpochMs", o.timestampEpochMs)
                    put("riseRating", o.riseRating.name)
                    putOpt("timeToPeakMinutes", o.timeToPeakMinutes)
                    putOpt("smell", o.smell?.name)
                    putOpt("photoPath", o.photoPath)
                    putOpt("note", o.note)
                })
            }
        })
        put("bakes", JSONArray().apply {
            dao.getAllBakes().forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id); put("starterId", b.starterId)
                    put("timestampEpochMs", b.timestampEpochMs)
                    putOpt("levainNotes", b.levainNotes)
                    put("outcomeRating", b.outcomeRating)
                    putOpt("photoPath", b.photoPath)
                    putOpt("note", b.note)
                })
            }
        })
    }

    private suspend fun importJson(json: JSONObject) {
        dao.clearStarters() // children cascade

        val starters = json.getJSONArray("starters")
        for (i in 0 until starters.length()) {
            val s = starters.getJSONObject(i)
            dao.insertStarter(
                Starter(
                    id = s.getLong("id"),
                    name = s.getString("name"),
                    state = LifecycleState.valueOf(s.getString("state")),
                    activeIntervalHours = s.getInt("activeIntervalHours"),
                    dormantIntervalHours = s.getInt("dormantIntervalHours"),
                    createdAtEpochMs = s.getLong("createdAtEpochMs"),
                    lastNotifiedDueAtEpochMs = s.optLongOrNull("lastNotifiedDueAtEpochMs"),
                    programStartedAtEpochMs = s.optLongOrNull("programStartedAtEpochMs"),
                )
            )
        }
        val feedings = json.getJSONArray("feedings")
        for (i in 0 until feedings.length()) {
            val f = feedings.getJSONObject(i)
            dao.insertFeeding(
                Feeding(
                    id = f.getLong("id"),
                    starterId = f.getLong("starterId"),
                    timestampEpochMs = f.getLong("timestampEpochMs"),
                    ratio = f.getString("ratio"),
                    flourType = f.getString("flourType"),
                    intervalHoursAtFeeding = if (f.has("intervalHoursAtFeeding")) f.getInt("intervalHoursAtFeeding") else null,
                )
            )
        }
        val observations = json.getJSONArray("observations")
        for (i in 0 until observations.length()) {
            val o = observations.getJSONObject(i)
            dao.insertObservation(
                HealthObservation(
                    id = o.getLong("id"),
                    starterId = o.getLong("starterId"),
                    timestampEpochMs = o.getLong("timestampEpochMs"),
                    riseRating = RiseRating.valueOf(o.getString("riseRating")),
                    timeToPeakMinutes = if (o.has("timeToPeakMinutes")) o.getInt("timeToPeakMinutes") else null,
                    smell = o.optString("smell", "").ifEmpty { null }?.let { Smell.valueOf(it) },
                    photoPath = o.optString("photoPath", "").ifEmpty { null },
                    note = o.optString("note", "").ifEmpty { null },
                )
            )
        }
        val bakes = json.getJSONArray("bakes")
        for (i in 0 until bakes.length()) {
            val b = bakes.getJSONObject(i)
            dao.insertBake(
                Bake(
                    id = b.getLong("id"),
                    starterId = b.getLong("starterId"),
                    timestampEpochMs = b.getLong("timestampEpochMs"),
                    levainNotes = b.optString("levainNotes", "").ifEmpty { null },
                    outcomeRating = b.getInt("outcomeRating"),
                    photoPath = b.optString("photoPath", "").ifEmpty { null },
                    note = b.optString("note", "").ifEmpty { null },
                )
            )
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null
}
