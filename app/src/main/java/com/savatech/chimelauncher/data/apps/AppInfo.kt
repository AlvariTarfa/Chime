package com.savatech.chimelauncher.data.apps

data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val isWorkProfile: Boolean,
    val isSystemApp: Boolean = false,
) {
    val key: String
        get() = "$packageName/$className/$userSerial"
}
