package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_configs")
data class AppConfig(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "pinned")
    val pinned: Boolean,
    @ColumnInfo(name = "pin_order")
    val pinOrder: Int,
    @ColumnInfo(name = "hidden")
    val hidden: Boolean,
    @ColumnInfo(name = "daily_limit_min")
    val dailyLimitMin: Int?,
    @ColumnInfo(name = "linked_goal_id")
    val linkedGoalId: String?,
)
