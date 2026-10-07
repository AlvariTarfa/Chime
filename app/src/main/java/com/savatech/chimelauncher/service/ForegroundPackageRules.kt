package com.savatech.chimelauncher.service

class ForegroundPackageRules(
    private val appPackageName: String,
    private val systemUiPackageName: String,
    private val homePackages: Set<String>,
    private val inputMethodPackages: Set<String>,
) {
    fun shouldIgnore(packageName: String?): Boolean =
        packageName == null ||
            packageName == appPackageName ||
            packageName == systemUiPackageName ||
            packageName in homePackages ||
            packageName in inputMethodPackages
}

class ForegroundPackageDebouncer(
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
) {
    private val lastSeenByPackage = mutableMapOf<String, Long>()

    fun shouldProcess(packageName: String, elapsedRealtimeMillis: Long): Boolean {
        val lastSeen = lastSeenByPackage[packageName]
        if (lastSeen != null && elapsedRealtimeMillis - lastSeen < debounceMillis) return false
        lastSeenByPackage[packageName] = elapsedRealtimeMillis
        return true
    }

    private companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 2_000L
    }
}
