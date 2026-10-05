package com.savatech.chimelauncher.domain.model

enum class Recurrence {
    DAILY,
    DAYS_OF_WEEK;

    companion object {
        fun fromStorage(value: String): Recurrence =
            entries.firstOrNull { it.name == value } ?: DAILY
    }
}
