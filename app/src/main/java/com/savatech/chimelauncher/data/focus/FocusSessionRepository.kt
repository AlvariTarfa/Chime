package com.savatech.chimelauncher.data.focus

import androidx.room.withTransaction
import com.savatech.chimelauncher.data.db.AppDatabase
import com.savatech.chimelauncher.data.db.dao.FocusSessionDao
import com.savatech.chimelauncher.data.db.dao.GoalDao
import com.savatech.chimelauncher.data.db.dao.TaskLogDao
import com.savatech.chimelauncher.data.db.dao.TaskDao
import com.savatech.chimelauncher.data.db.entities.FocusSession
import com.savatech.chimelauncher.domain.focus.creditSessionToTaskLog
import com.savatech.chimelauncher.domain.focus.remainingSessionMillis
import com.savatech.chimelauncher.service.SessionCompletionScheduler
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FocusSessionRepository @Inject constructor(
    private val database: AppDatabase,
    private val sessionDao: FocusSessionDao,
    private val goalDao: GoalDao,
    private val taskLogDao: TaskLogDao,
    private val taskDao: TaskDao,
    private val focusRepository: FocusRepository,
    private val alarmScheduler: SessionCompletionScheduler,
    private val clock: Clock,
) {
    fun observeActiveSession(): Flow<FocusSession?> =
        sessionDao.observeAll().map { sessions -> sessions.firstOrNull { it.endedAt == null } }

    fun observeLatestSession(): Flow<FocusSession?> =
        sessionDao.observeAll().map { sessions -> sessions.maxByOrNull(FocusSession::startedAt) }

    suspend fun start(goalId: String?, taskId: String?, plannedMinutes: Int): Long {
        require(plannedMinutes in MIN_SESSION_MINUTES..MAX_SESSION_MINUTES) {
            "Session length must be between $MIN_SESSION_MINUTES and $MAX_SESSION_MINUTES minutes."
        }
        require(observeUnfinishedNow().isEmpty()) { "A focus session is already active." }
        if (taskId != null) {
            require(goalId != null && goalDao.getById(goalId) != null) { "Linked goal does not exist." }
            require(taskDao.getById(taskId)?.goalId == goalId) { "Linked task does not belong to the goal." }
        }

        val startedAt = clock.millis()
        val sessionId = sessionDao.insert(
            FocusSession(
                goalId = goalId,
                taskId = taskId,
                startedAt = startedAt,
                plannedMinutes = plannedMinutes,
                endedAt = null,
                completed = false,
            ),
        )
        alarmScheduler.schedule(sessionId, startedAt + plannedMinutes * 60_000L)
        focusRepository.seedBuiltInModesIfNeeded()
        focusRepository.setManualMode(
            DEEP_WORK_MODE_ID,
            LocalDateTime.now(clock).plusMinutes(plannedMinutes.toLong()),
        )
        return sessionId
    }

    suspend fun endEarly(sessionId: Long) {
        val session = sessionDao.getById(sessionId) ?: return
        if (session.endedAt != null) return
        sessionDao.update(session.copy(endedAt = clock.millis(), completed = false))
        alarmScheduler.cancel(sessionId)
        focusRepository.setManualMode(null, LocalDateTime.now(clock).plusMinutes(1))
    }

    suspend fun reconcileCompletedSessions(now: Long = clock.millis()) {
        database.withTransaction {
            sessionDao.getUnfinished().forEach { session ->
                if (remainingSessionMillis(session.startedAt, session.plannedMinutes, now) == 0L) {
                    val taskId = session.taskId
                    if (taskId != null) {
                        val today = LocalDate.now(clock).toString()
                        val existing = taskLogDao.getForTaskAndDate(taskId, today)
                        val goal = if (session.goalId != null) goalDao.getById(session.goalId) else null
                        val updated = creditSessionToTaskLog(
                            existing = existing,
                            taskId = taskId,
                            date = today,
                            goalUnit = goal?.unit,
                            plannedMinutes = session.plannedMinutes,
                        )
                        if (updated != null) taskLogDao.upsert(updated)
                    }
                    val deadline = session.startedAt + session.plannedMinutes * 60_000L
                    sessionDao.update(session.copy(endedAt = deadline, completed = true))
                }
            }
        }
    }

    private suspend fun observeUnfinishedNow(): List<FocusSession> = sessionDao.getUnfinished()

    private companion object {
        const val MIN_SESSION_MINUTES = 1
        const val MAX_SESSION_MINUTES = 240
        const val DEEP_WORK_MODE_ID = "builtin-deep-work"
    }
}
