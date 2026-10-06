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

enum class IconShape {
    CIRCLE,
    SQUIRCLE,
    ROUNDED_SQUARE,
    NONE;

    companion object {
        fun fromStorage(value: String): IconShape =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CIRCLE
    }
}

enum class FontPreset {
    DEFAULT,
    SANS_SERIF,
    SERIF,
    MONOSPACE,
    CURSIVE;

    companion object {
        fun fromStorage(value: String): FontPreset = when (value.lowercase()) {
            "sans_serif", "sansserif" -> SANS_SERIF
            "serif" -> SERIF
            "monospace" -> MONOSPACE
            "cursive" -> CURSIVE
            else -> DEFAULT
        }
    }
}

enum class LayoutDensityPreset {
    COMPACT,
    COMFORTABLE,
    SPACIOUS;

    companion object {
        fun fromStorage(value: String): LayoutDensityPreset =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COMFORTABLE
    }
}

data class SwipeAppTarget(
    val packageName: String,
    val className: String,
    val userSerial: Long,
)
