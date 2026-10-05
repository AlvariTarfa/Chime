package com.savatech.chimelauncher.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun goalAndTaskDaosReadObserveAndCascadeDelete() = runBlocking {
        val goal = Goal(
            id = "goal-1",
            title = "Read",
            why = null,
            category = "PRODUCTIVE",
            targetDate = null,
            unit = null,
            targetValue = null,
            status = "ACTIVE",
            createdAt = 1L,
        )
        val task = Task(
            id = "task-1",
            goalId = goal.id,
            title = "Read a chapter",
            recurrence = "DAILY",
            daysMask = 0,
            reminderTime = null,
            createdAt = 1L,
        )

        database.goalDao().insert(goal)
        database.taskDao().insert(task)

        assertEquals(goal, database.goalDao().getById(goal.id))
        assertEquals(task, database.taskDao().observeAll().first().single())

        database.goalDao().delete(goal)

        assertNull(database.goalDao().getById(goal.id))
        assertEquals(emptyList<Task>(), database.taskDao().observeAll().first())
    }

    @Test
    fun taskLogsEnforceUniqueTaskAndDate() = runBlocking {
        val goal = Goal(
            id = "goal-1",
            title = "Read",
            why = null,
            category = "PRODUCTIVE",
            targetDate = null,
            unit = null,
            targetValue = null,
            status = "ACTIVE",
            createdAt = 1L,
        )
        val task = Task("task-1", goal.id, "Read", "DAILY", 0, null, 1L)
        database.goalDao().insert(goal)
        database.taskDao().insert(task)
        database.taskLogDao().insert(
            TaskLog(taskId = task.id, date = "2026-10-05", value = null, completed = true),
        )

        try {
            database.taskLogDao().insert(
                TaskLog(taskId = task.id, date = "2026-10-05", value = null, completed = false),
            )
        } catch (_: SQLiteConstraintException) {
            return@runBlocking
        }
        throw AssertionError("A task can only have one log per date.")
    }
}
