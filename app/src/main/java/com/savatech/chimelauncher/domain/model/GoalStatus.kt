package com.savatech.chimelauncher.domain.model

enum class GoalStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED;

    companion object {
        fun fromStorage(value: String): GoalStatus =
            entries.firstOrNull { it.name == value } ?: ACTIVE
    }
}
