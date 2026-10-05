package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_modes")
data class FocusMode(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "allowed_packages")
    val allowedPackages: String,
    @ColumnInfo(name = "schedule_json")
    val scheduleJson: String?,
    @ColumnInfo(name = "suppress_notifications")
    val suppressNotifications: Boolean,
    @ColumnInfo(name = "is_built_in")
    val isBuiltIn: Boolean,
)
