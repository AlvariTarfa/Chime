package com.savatech.chimelauncher.domain.intercept

sealed interface InterceptDecision {
    data object Allow : InterceptDecision
    data class Pause(val delaySeconds: Int, val reason: PauseReason) : InterceptDecision
}

enum class PauseReason {
    DISTRACTING,
    LIMIT_REACHED,
}
