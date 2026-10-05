package com.savatech.chimelauncher.domain.model

import java.time.LocalDate

data class GoalModel(
    val id: String,
    val title: String,
    val why: String?,
    val category: String,
    val targetDate: LocalDate?,
    val unit: String?,
    val targetValue: Double?,
    val status: GoalStatus,
    val createdAt: Long,
)
