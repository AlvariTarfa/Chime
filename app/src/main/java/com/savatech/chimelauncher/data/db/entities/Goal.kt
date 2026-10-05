package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "why")
    val why: String?,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "target_date")
    val targetDate: String?,
    @ColumnInfo(name = "unit")
    val unit: String?,
    @ColumnInfo(name = "target_value")
    val targetValue: Double?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
