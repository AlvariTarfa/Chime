package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["goal_id"])],
)
data class Task(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "goal_id")
    val goalId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "recurrence")
    val recurrence: String,
    @ColumnInfo(name = "days_mask")
    val daysMask: Int,
    @ColumnInfo(name = "reminder_time")
    val reminderTime: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
