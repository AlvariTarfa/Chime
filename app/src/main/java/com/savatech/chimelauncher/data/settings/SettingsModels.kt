package com.savatech.chimelauncher.data.settings

enum class DrawerMode {
    TEXT,
    ICONS,
    GRID;

    companion object {
        fun fromStorage(value: String): DrawerMode =
            entries.firstOrNull { it.name == value } ?: TEXT
    }
}

enum class FrictionLevel(val defaultBaseDelaySeconds: Int) {
    GENTLE(5),
    BALANCED(8),
    STRICT(15);

    companion object {
        fun fromStorage(value: String): FrictionLevel =
            entries.firstOrNull { it.name == value } ?: BALANCED
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED;

    companion object {
        fun fromStorage(value: String): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
