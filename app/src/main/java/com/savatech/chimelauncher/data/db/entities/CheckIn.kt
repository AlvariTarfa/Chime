package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_ins",
    indices = [Index(value = ["type", "date"])],
)
data class CheckIn(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "mood")
    val mood: Int?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String?,
)
