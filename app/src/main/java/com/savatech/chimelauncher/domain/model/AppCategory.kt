package com.savatech.chimelauncher.domain.model

enum class AppCategory {
    PRODUCTIVE,
    NEUTRAL,
    DISTRACTING;

    companion object {
        fun fromStorage(value: String): AppCategory =
            entries.firstOrNull { it.name == value } ?: NEUTRAL
    }
}
