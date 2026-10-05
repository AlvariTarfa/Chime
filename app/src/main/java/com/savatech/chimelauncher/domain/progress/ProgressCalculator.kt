package com.savatech.chimelauncher.domain.progress

fun dailyProgress(scheduledTaskCount: Int, completedCount: Int): Float? {
    if (scheduledTaskCount <= 0) return null
    return (completedCount.coerceIn(0, scheduledTaskCount).toFloat() / scheduledTaskCount)
        .coerceIn(0f, 1f)
}

fun overallProgress(sumOfLoggedValues: Double, targetValue: Double?): Float? {
    if (targetValue == null || targetValue <= 0.0 || targetValue.isNaN()) return null
    val progress = sumOfLoggedValues / targetValue
    if (progress.isNaN()) return 0f
    return progress.coerceIn(0.0, 1.0).toFloat()
}
