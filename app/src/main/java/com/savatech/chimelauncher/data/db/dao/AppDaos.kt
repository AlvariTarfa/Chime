package com.savatech.chimelauncher.data.db.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.AppGrant
import com.savatech.chimelauncher.data.db.entities.CheckIn
import com.savatech.chimelauncher.data.db.entities.DailyPriority
import com.savatech.chimelauncher.data.db.entities.DigestItem
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.db.entities.FocusSession
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal): Int

    @Delete
    suspend fun delete(goal: Goal): Int

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): Goal?

    @Query("SELECT * FROM goals WHERE status = :status")
    fun observeByStatus(status: String): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<Goal>>
}

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task): Int

    @Delete
    suspend fun delete(task: Task): Int

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): Task?

    @Query("SELECT * FROM tasks WHERE goal_id = :goalId")
    fun observeByGoal(goalId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    fun observeAll(): Flow<List<Task>>
}

@Dao
interface TaskLogDao {
    @Insert
    suspend fun insert(taskLog: TaskLog): Long

    @Update
    suspend fun update(taskLog: TaskLog): Int

    @Delete
    suspend fun delete(taskLog: TaskLog): Int

    @Query("SELECT * FROM task_logs WHERE id = :id")
    suspend fun getById(id: Long): TaskLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(taskLog: TaskLog): Long

    @Query("SELECT * FROM task_logs WHERE task_id = :taskId AND date = :date")
    suspend fun getForTaskAndDate(taskId: String, date: String): TaskLog?

    @Query("SELECT * FROM task_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date")
    fun observeBetweenDates(startDate: String, endDate: String): Flow<List<TaskLog>>

    @Query("SELECT * FROM task_logs")
    fun observeAll(): Flow<List<TaskLog>>
}

@Dao
interface DailyPriorityDao {
    @Insert
    suspend fun insert(dailyPriority: DailyPriority): Long

    @Update
    suspend fun update(dailyPriority: DailyPriority): Int

    @Delete
    suspend fun delete(dailyPriority: DailyPriority): Int

    @Query("SELECT * FROM daily_priorities WHERE date = :date AND goal_id = :goalId")
    suspend fun getById(date: String, goalId: String): DailyPriority?

    @Query("SELECT * FROM daily_priorities WHERE date = :date ORDER BY position")
    fun observeForDate(date: String): Flow<List<DailyPriority>>

    @Query("DELETE FROM daily_priorities WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Insert
    suspend fun insertAll(dailyPriorities: List<DailyPriority>)

    @Transaction
    suspend fun replaceForDate(date: String, dailyPriorities: List<DailyPriority>) {
        deleteForDate(date)
        if (dailyPriorities.isNotEmpty()) insertAll(dailyPriorities)
    }

    @Query("SELECT * FROM daily_priorities")
    fun observeAll(): Flow<List<DailyPriority>>
}

@Dao
interface AppConfigDao {
    @Insert
    suspend fun insert(appConfig: AppConfig): Long

    @Update
    suspend fun update(appConfig: AppConfig): Int

    @Delete
    suspend fun delete(appConfig: AppConfig): Int

    @Query("SELECT * FROM app_configs WHERE package_name = :packageName")
    suspend fun getById(packageName: String): AppConfig?

    @Query("SELECT * FROM app_configs")
    suspend fun getAll(): List<AppConfig>

    @Query("SELECT * FROM app_configs")
    fun observeAll(): Flow<List<AppConfig>>
}

@Dao
interface AppGrantDao {
    @Insert
    suspend fun insert(appGrant: AppGrant): Long

    @Update
    suspend fun update(appGrant: AppGrant): Int

    @Delete
    suspend fun delete(appGrant: AppGrant): Int

    @Query("SELECT * FROM app_grants WHERE package_name = :packageName")
    suspend fun getById(packageName: String): AppGrant?

    @Query("SELECT * FROM app_grants WHERE package_name = :packageName AND expires_at > :now")
    suspend fun getActiveByPackage(packageName: String, now: Long): AppGrant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appGrant: AppGrant): Long

    @Query("DELETE FROM app_grants WHERE expires_at <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("SELECT * FROM app_grants")
    fun observeAll(): Flow<List<AppGrant>>
}

@Dao
interface InterceptEventDao {
    @Insert
    suspend fun insert(interceptEvent: InterceptEvent): Long

    @Update
    suspend fun update(interceptEvent: InterceptEvent): Int

    @Delete
    suspend fun delete(interceptEvent: InterceptEvent): Int

    @Query("SELECT * FROM intercept_events WHERE id = :id")
    suspend fun getById(id: Long): InterceptEvent?

    @Query(
        "SELECT * FROM intercept_events WHERE timestamp >= :startInclusive " +
            "AND timestamp < :endExclusive",
    )
    suspend fun getBetween(startInclusive: Long, endExclusive: Long): List<InterceptEvent>

    @Query(
        "SELECT COUNT(*) FROM intercept_events WHERE package_name = :packageName " +
            "AND outcome = 'OPENED' AND timestamp >= :startInclusive AND timestamp < :endExclusive",
    )
    suspend fun countOpenedBetween(packageName: String, startInclusive: Long, endExclusive: Long): Int

    @Query("SELECT * FROM intercept_events")
    fun observeAll(): Flow<List<InterceptEvent>>
}

@Dao
interface FocusModeDao {
    @Insert
    suspend fun insert(focusMode: FocusMode): Long

    @Update
    suspend fun update(focusMode: FocusMode): Int

    @Delete
    suspend fun delete(focusMode: FocusMode): Int

    @Query("SELECT * FROM focus_modes WHERE id = :id")
    suspend fun getById(id: String): FocusMode?

    @Query("SELECT * FROM focus_modes")
    fun observeAll(): Flow<List<FocusMode>>
}

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(focusSession: FocusSession): Long

    @Update
    suspend fun update(focusSession: FocusSession): Int

    @Delete
    suspend fun delete(focusSession: FocusSession): Int

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getById(id: Long): FocusSession?

    @Query("SELECT * FROM focus_sessions WHERE ended_at IS NULL")
    suspend fun getUnfinished(): List<FocusSession>

    @Query("SELECT * FROM focus_sessions")
    fun observeAll(): Flow<List<FocusSession>>
}

@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckIn): Long

    @Update
    suspend fun update(checkIn: CheckIn): Int

    @Delete
    suspend fun delete(checkIn: CheckIn): Int

    @Query("SELECT * FROM check_ins WHERE id = :id")
    suspend fun getById(id: Long): CheckIn?

    @Query("SELECT * FROM check_ins")
    fun observeAll(): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins ORDER BY date DESC, id DESC")
    fun observeHistory(): Flow<List<CheckIn>>
}

@Dao
interface DigestItemDao {
    @Insert
    suspend fun insert(digestItem: DigestItem): Long

    @Update
    suspend fun update(digestItem: DigestItem): Int

    @Delete
    suspend fun delete(digestItem: DigestItem): Int

    @Query("SELECT * FROM digest_items WHERE id = :id")
    suspend fun getById(id: Long): DigestItem?

    @Query("SELECT * FROM digest_items")
    fun observeAll(): Flow<List<DigestItem>>

    @Query("SELECT * FROM digest_items WHERE delivered = 0 ORDER BY posted_at")
    suspend fun getUndelivered(): List<DigestItem>

    @Query("UPDATE digest_items SET delivered = 1 WHERE id IN (:ids)")
    suspend fun markDelivered(ids: List<Long>)

    @Query("DELETE FROM digest_items WHERE delivered = 1 AND posted_at < :cutoff")
    suspend fun deleteDeliveredBefore(cutoff: Long)

    @Query("DELETE FROM digest_items")
    suspend fun deleteAll()
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM goals")
    suspend fun goals(): List<Goal>

    @Query("SELECT * FROM tasks")
    suspend fun tasks(): List<Task>

    @Query("SELECT * FROM task_logs")
    suspend fun taskLogs(): List<TaskLog>

    @Query("SELECT * FROM daily_priorities")
    suspend fun dailyPriorities(): List<DailyPriority>

    @Query("SELECT * FROM app_configs")
    suspend fun appConfigs(): List<AppConfig>

    @Query("SELECT * FROM focus_modes")
    suspend fun focusModes(): List<FocusMode>

    @Query("SELECT * FROM focus_sessions")
    suspend fun focusSessions(): List<FocusSession>

    @Query("SELECT * FROM check_ins")
    suspend fun checkIns(): List<CheckIn>

    @Query("SELECT * FROM intercept_events")
    suspend fun interceptEvents(): List<InterceptEvent>

    @Query("SELECT * FROM app_grants")
    suspend fun appGrants(): List<AppGrant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putGoals(rows: List<Goal>)

    @Update
    suspend fun updateGoals(rows: List<Goal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTasks(rows: List<Task>)

    @Update
    suspend fun updateTasks(rows: List<Task>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTaskLogs(rows: List<TaskLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDailyPriorities(rows: List<DailyPriority>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAppConfigs(rows: List<AppConfig>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFocusModes(rows: List<FocusMode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFocusSessions(rows: List<FocusSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCheckIns(rows: List<CheckIn>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putInterceptEvents(rows: List<InterceptEvent>)

    @Query("DELETE FROM task_logs")
    suspend fun clearTaskLogs()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM daily_priorities")
    suspend fun clearDailyPriorities()

    @Query("DELETE FROM app_configs")
    suspend fun clearAppConfigs()

    @Query("DELETE FROM app_grants")
    suspend fun clearAppGrants()

    @Query("DELETE FROM intercept_events")
    suspend fun clearInterceptEvents()

    @Query("DELETE FROM focus_sessions")
    suspend fun clearFocusSessions()

    @Query("DELETE FROM check_ins")
    suspend fun clearCheckIns()

    @Query("DELETE FROM digest_items")
    suspend fun clearDigestItems()

    @Query("DELETE FROM focus_modes")
    suspend fun clearFocusModes()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()
}
