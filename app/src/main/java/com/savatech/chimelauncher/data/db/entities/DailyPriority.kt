package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "daily_priorities",
    primaryKeys = ["date", "goal_id"],
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
data class DailyPriority(
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "goal_id")
    val goalId: String,
    @ColumnInfo(name = "position")
    val position: Int,
)
