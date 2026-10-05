package com.savatech.chimelauncher.domain.limits

sealed interface LimitState {
    data object NoLimit : LimitState
    data class Under(val remainingMillis: Long) : LimitState
    data object Reached : LimitState
}

fun evaluate(usedMillis: Long, limitMinutes: Int?): LimitState {
    if (limitMinutes == null || limitMinutes <= 0) return LimitState.NoLimit
    val limitMillis = limitMinutes.toLong() * MILLIS_PER_MINUTE
    val remainingMillis = limitMillis - usedMillis.coerceAtLeast(0L)
    return if (remainingMillis > 0L) LimitState.Under(remainingMillis) else LimitState.Reached
}

fun effectiveLimit(appLimit: Int?, categoryLimit: Int?): Int? = appLimit ?: categoryLimit

fun isValidLimitOverrideReason(reason: String): Boolean = reason.trim().length >= 10

private const val MILLIS_PER_MINUTE = 60_000L
