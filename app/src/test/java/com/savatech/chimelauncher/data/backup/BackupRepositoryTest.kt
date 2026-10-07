package com.savatech.chimelauncher.data.backup

import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.CheckIn
import com.savatech.chimelauncher.data.db.entities.DailyPriority
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.db.entities.FocusSession
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog
import com.savatech.chimelauncher.domain.backup.BackupCodec
import com.savatech.chimelauncher.domain.backup.BackupError
import com.savatech.chimelauncher.domain.backup.BackupIssueReason
import com.savatech.chimelauncher.domain.backup.BackupImportMode
import com.savatech.chimelauncher.domain.backup.BackupModel
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {
    @Test
    fun backupCodecRoundTripRestoresAllExportedRecords() {
        val original = records()
        val backup = model(original)
        val serialized = BackupCodec.encode(backup)
        assertEquals(backup, BackupCodec.decode(serialized))
        val store = InMemoryBackupStore(emptyRecords())
        store.import(serialized)

        assertEquals(original, store.rows)
    }

    @Test
    fun newerSchemaIsRejectedWithTypedError() {
        val error = assertThrows(BackupError.NewerSchemaVersion::class.java) {
            BackupCodec.decode("""{"schemaVersion":2,"exportedAt":"2026-10-07T00:00"}""")
        }

        assertEquals(2, error.found)
        assertEquals(BackupCodec.CURRENT_SCHEMA_VERSION, error.supported)
    }

    @Test
    fun orphanTaskIsSkippedAndCounted() {
        val incoming = emptyRecords().copy(
            tasks = listOf(Task("orphan", "missing", "Orphan", "DAILY", 0, null, 1)),
        )
        val plan = planImport(incoming, emptyRecords(), BackupImportMode.REPLACE)

        assertEquals(1, plan.report.skipped)
        assertEquals(BackupIssueReason.MISSING_GOAL, plan.report.errors.single().reason)
        assertTrue(plan.insertTasks.isEmpty())
    }

    @Test
    fun malformedJsonDoesNotChangeInMemoryStore() {
        val store = InMemoryBackupStore(records())
        val before = store.rows

        assertThrows(BackupError.MalformedJson::class.java) {
            store.import("{ not json")
        }

        assertEquals(before, store.rows)
    }

    @Test
    fun unknownFieldsAreIgnoredAndOptionalCollectionsDefaultEmpty() {
        val decoded = BackupCodec.decode(
            """{"schemaVersion":1,"exportedAt":"2026-10-07T00:00","futureField":{"value":1}}""",
        )

        assertTrue(decoded.goals.isEmpty())
        assertTrue(decoded.tasks.isEmpty())
        assertTrue(decoded.settings.isEmpty())
    }

    @Test
    fun mergeKeepsNewerExistingTimestampAndPrefersImportedWithoutTimestamp() {
        val existing = emptyRecords().copy(
            goals = listOf(Goal("g", "Existing", null, "HEALTH", null, null, null, "ACTIVE", 200)),
            appConfigs = listOf(AppConfig("pkg", "NEUTRAL", false, 0, false, null, null)),
        )
        val incoming = emptyRecords().copy(
            goals = listOf(Goal("g", "Older import", null, "HEALTH", null, null, null, "ACTIVE", 100)),
            appConfigs = listOf(AppConfig("pkg", "DISTRACTING", false, 0, false, null, null)),
        )
        val plan = planImport(incoming, existing, BackupImportMode.MERGE)

        assertTrue(plan.updateGoals.isEmpty())
        assertEquals(1, plan.report.skipped)
        assertEquals("DISTRACTING", plan.appConfigs.single().category)
        assertEquals(1, plan.report.updated)
    }

    @Test
    fun importWithoutTimestampUpdatesCollidingRows() {
        val old = emptyRecords().copy(
            appConfigs = listOf(AppConfig("pkg", "NEUTRAL", false, 0, false, null, null)),
        )
        val imported = emptyRecords().copy(
            appConfigs = listOf(AppConfig("pkg", "PRODUCTIVE", false, 0, false, null, null)),
        )
        val plan = planImport(imported, old, BackupImportMode.MERGE)

        assertEquals("PRODUCTIVE", plan.appConfigs.single().category)
        assertEquals(1, plan.report.updated)
    }

    private fun model(rows: BackupEntityLists) = BackupModel(
        schemaVersion = BackupCodec.CURRENT_SCHEMA_VERSION,
        exportedAt = "2026-10-07T00:00",
        goals = rows.goals.map(Goal::toBackup),
        tasks = rows.tasks.map(Task::toBackup),
        taskLogs = rows.taskLogs.map(TaskLog::toBackup),
        dailyPriorities = rows.dailyPriorities.map(DailyPriority::toBackup),
        appConfigs = rows.appConfigs.map(AppConfig::toBackup),
        focusModes = rows.focusModes.map(FocusMode::toBackup),
        focusSessions = rows.focusSessions.map(FocusSession::toBackup),
        checkIns = rows.checkIns.map(CheckIn::toBackup),
        interceptEvents = rows.interceptEvents.map(InterceptEvent::toBackup),
        settings = mapOf("focus_score_enabled" to JsonPrimitive(true)),
    )

    private fun records() = BackupEntityLists(
        goals = listOf(Goal("g", "Goal", "why", "HEALTH", "2026-12-01", "min", 30.0, "ACTIVE", 12)),
        tasks = listOf(Task("t", "g", "Task", "DAILY", 127, "09:30", 13)),
        taskLogs = listOf(TaskLog(1, "t", "2026-10-07", 3.5, true)),
        dailyPriorities = listOf(DailyPriority("2026-10-07", "g", 0)),
        appConfigs = listOf(AppConfig("pkg", "DISTRACTING", true, 2, false, 45, "g")),
        focusModes = listOf(FocusMode("mode", "Mode", "pkg", null, true, false)),
        focusSessions = listOf(FocusSession(2, "g", "t", 100, 25, 200, true)),
        checkIns = listOf(CheckIn(3, "EVENING", "2026-10-07", 4, "note", """{"x":1}""")),
        interceptEvents = listOf(InterceptEvent(4, "pkg", 300, "OPENED", 10, "reason")),
    )

    private fun emptyRecords() = BackupEntityLists(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(),
    )
}

private class InMemoryBackupStore(var rows: BackupEntityLists) {
    fun import(source: String) {
        val decoded = BackupCodec.decode(source).toEntityLists()
        val plan = planImport(decoded, rows, BackupImportMode.MERGE)
        rows = rows.copy(
            goals = rows.goals.filterNot { old -> plan.updateGoals.any { it.id == old.id } } +
                plan.insertGoals + plan.updateGoals,
            tasks = rows.tasks.filterNot { old -> plan.updateTasks.any { it.id == old.id } } +
                plan.insertTasks + plan.updateTasks,
            taskLogs = rows.taskLogs + plan.taskLogs,
            dailyPriorities = rows.dailyPriorities + plan.dailyPriorities,
            appConfigs = rows.appConfigs + plan.appConfigs,
            focusModes = rows.focusModes + plan.focusModes,
            focusSessions = rows.focusSessions + plan.focusSessions,
            checkIns = rows.checkIns + plan.checkIns,
            interceptEvents = rows.interceptEvents + plan.interceptEvents,
        )
    }
}
