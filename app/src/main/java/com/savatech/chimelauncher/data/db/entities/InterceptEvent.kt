package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intercept_events",
    indices = [Index(value = ["timestamp"])],
)
data class InterceptEvent(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "outcome")
    val outcome: String,
    @ColumnInfo(name = "granted_minutes")
    val grantedMinutes: Int?,
    @ColumnInfo(name = "reason")
    val reason: String?,
)
