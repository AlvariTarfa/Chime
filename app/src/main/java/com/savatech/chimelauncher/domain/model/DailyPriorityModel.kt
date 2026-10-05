package com.savatech.chimelauncher.domain.model

import java.time.LocalDate

data class DailyPriorityModel(
    val date: LocalDate,
    val goalId: String,
    val position: Int,
)
