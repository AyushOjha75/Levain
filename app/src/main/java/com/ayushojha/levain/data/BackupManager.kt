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
        /**
         * 1 — the v0.3 shape. 2 — adds bake status/scale/provenance and
         * snapshotted bake steps. Archives at 1 still import; Recipes are
         * never in a backup because they are bundled content, not user data.
         */
        const val FORMAT_VERSION = 2
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
                    put("id", b.id)
                    putOpt("starterId", b.starterId)
                    putOpt("recipeId", b.recipeId)
                    putOpt("recipeContentVersion", b.recipeContentVersion)
                    put("status", b.status.name)
                    put("scale", b.scale)
                    put("timestampEpochMs", b.timestampEpochMs)
                    putOpt("startedAtEpochMs", b.startedAtEpochMs)
                    putOpt("heldAtEpochMs", b.heldAtEpochMs)
                    putOpt("levainNotes", b.levainNotes)
                    putOpt("outcomeRating", b.outcomeRating)
                    putOpt("photoPath", b.photoPath)
                    putOpt("note", b.note)
                })
            }
        })
        put("bakeSteps", JSONArray().apply {
            dao.getAllBakeSteps().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("bakeId", s.bakeId); put("position", s.position)
                    put("title", s.title); put("instruction", s.instruction)
                    put("kind", s.kind.name)
                    putOpt("cue", s.cue)
                    putOpt("plannedDurationMinutes", s.plannedDurationMinutes)
                    putOpt("dueAtEpochMs", s.dueAtEpochMs)
                    putOpt("completedAtEpochMs", s.completedAtEpochMs)
                })
            }
        })
    }

    private suspend fun importJson(json: JSONObject) {
        dao.clearStarters() // feedings and observations cascade
        dao.clearBakes() // bakes no longer cascade from starters; steps cascade from bakes

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
                    // Format 1 always had a starter and always had a rating;
                    // format 2 may have neither (yeasted bread, bake in progress).
                    starterId = b.optLongOrNull("starterId"),
                    recipeId = b.optString("recipeId", "").ifEmpty { null },
                    recipeContentVersion = if (b.has("recipeContentVersion") && !b.isNull("recipeContentVersion")) {
                        b.getInt("recipeContentVersion")
                    } else {
                        null
                    },
                    status = BakeStatus.valueOf(b.optString("status", BakeStatus.FINISHED.name)),
                    scale = b.optDouble("scale", 1.0),
                    timestampEpochMs = b.getLong("timestampEpochMs"),
                    startedAtEpochMs = b.optLongOrNull("startedAtEpochMs"),
                    heldAtEpochMs = b.optLongOrNull("heldAtEpochMs"),
                    levainNotes = b.optString("levainNotes", "").ifEmpty { null },
                    outcomeRating = if (b.has("outcomeRating") && !b.isNull("outcomeRating")) {
                        b.getInt("outcomeRating")
                    } else {
                        null
                    },
                    photoPath = b.optString("photoPath", "").ifEmpty { null },
                    note = b.optString("note", "").ifEmpty { null },
                )
            )
        }

        // Absent in format 1 archives — nothing to restore, which is correct.
        val steps = json.optJSONArray("bakeSteps")
        if (steps != null) {
            val parsed = (0 until steps.length()).map { i ->
                val s = steps.getJSONObject(i)
                BakeStep(
                    id = s.getLong("id"),
                    bakeId = s.getLong("bakeId"),
                    position = s.getInt("position"),
                    title = s.getString("title"),
                    instruction = s.getString("instruction"),
                    kind = StepKind.valueOf(s.getString("kind")),
                    cue = s.optString("cue", "").ifEmpty { null },
                    plannedDurationMinutes = if (s.has("plannedDurationMinutes") && !s.isNull("plannedDurationMinutes")) {
                        s.getInt("plannedDurationMinutes")
                    } else {
                        null
                    },
                    dueAtEpochMs = s.optLongOrNull("dueAtEpochMs"),
                    completedAtEpochMs = s.optLongOrNull("completedAtEpochMs"),
                )
            }
            if (parsed.isNotEmpty()) dao.insertBakeSteps(parsed)
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null
}
