package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_logs",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["task_id", "date"], unique = true),
    ],
)
data class TaskLog(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "value")
    val value: Double?,
    @ColumnInfo(name = "completed")
    val completed: Boolean,
)
