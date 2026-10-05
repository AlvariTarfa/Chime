package com.savatech.chimelauncher.domain.model

enum class CheckInType {
    MORNING,
    EVENING,
    WEEKLY;

    companion object {
        fun fromStorage(value: String): CheckInType =
            entries.firstOrNull { it.name == value } ?: MORNING
    }
}
