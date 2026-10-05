package com.savatech.chimelauncher.core.launch

import com.savatech.chimelauncher.domain.intercept.PauseReason

sealed interface LaunchResult {
    data object Started : LaunchResult
    data class NeedsPause(val args: PauseLaunchArgs) : LaunchResult
    data class Failed(val error: Throwable) : LaunchResult
}

data class PauseLaunchArgs(
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val appLabel: String,
    val delaySeconds: Int,
    val reason: PauseReason,
    val usedMillis: Long = 0L,
    val limitMinutes: Int? = null,
)
