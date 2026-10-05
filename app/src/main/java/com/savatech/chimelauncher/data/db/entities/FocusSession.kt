package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "goal_id")
    val goalId: String?,
    @ColumnInfo(name = "task_id")
    val taskId: String?,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "planned_minutes")
    val plannedMinutes: Int,
    @ColumnInfo(name = "ended_at")
    val endedAt: Long?,
    @ColumnInfo(name = "completed")
    val completed: Boolean,
)
