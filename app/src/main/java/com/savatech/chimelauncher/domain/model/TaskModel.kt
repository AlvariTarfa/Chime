package com.savatech.chimelauncher.domain.model

data class TaskModel(
    val id: String,
    val goalId: String,
    val title: String,
    val recurrence: Recurrence,
    val daysMask: Int,
    val reminderTime: String?,
    val createdAt: Long,
)
