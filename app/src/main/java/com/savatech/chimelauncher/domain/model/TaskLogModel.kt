package com.savatech.chimelauncher.domain.model

import java.time.LocalDate

data class TaskLogModel(
    val id: Long = 0,
    val taskId: String,
    val date: LocalDate,
    val value: Double?,
    val completed: Boolean,
)
