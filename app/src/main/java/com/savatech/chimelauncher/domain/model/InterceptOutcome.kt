package com.savatech.chimelauncher.domain.model

enum class InterceptOutcome {
    CANCELLED,
    OPENED,
    LIMIT_OVERRIDE;

    companion object {
        fun fromStorage(value: String): InterceptOutcome =
            entries.firstOrNull { it.name == value } ?: CANCELLED
    }
}
