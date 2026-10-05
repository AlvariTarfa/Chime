package com.savatech.chimelauncher.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digest_items")
data class DigestItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "title")
    val title: String?,
    @ColumnInfo(name = "text")
    val text: String?,
    @ColumnInfo(name = "posted_at")
    val postedAt: Long,
    @ColumnInfo(name = "delivered")
    val delivered: Boolean,
)
