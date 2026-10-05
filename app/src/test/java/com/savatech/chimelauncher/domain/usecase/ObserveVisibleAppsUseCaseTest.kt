package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.FocusMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveVisibleAppsUseCaseTest {
    @Test
    fun filtersHiddenAppsAndUsesDefaultForMissingConfiguration() {
        val visible = app("visible")
        val hidden = app("hidden")

        assertEquals(
            listOf(visible),
            combineVisibleApps(
                listOf(visible, hidden),
                mapOf("hidden" to config("hidden", isHidden = true)),
            ),
        )
    }

    @Test
    fun ignoresConfigurationForAnUninstalledApp() {
        val installed = app("installed")

        assertEquals(
            listOf(installed),
            combineVisibleApps(
                listOf(installed),
                mapOf("uninstalled" to config("uninstalled", isHidden = true)),
            ),
        )
    }

    @Test
    fun activeModeKeepsAllowedAndAlwaysAllowedAppsButNeverOverridesHiddenApps() {
        val allowed = app("allowed")
        val phone = app("phone")
        val hidden = app("hidden")
        val denied = app("denied")
        val mode = FocusMode(
            id = "work",
            name = "Work",
            allowedPackages = "[\"allowed\",\"hidden\"]",
            scheduleJson = null,
            suppressNotifications = false,
            isBuiltIn = true,
        )

        assertEquals(
            listOf(allowed, phone),
            combineVisibleApps(
                listOf(allowed, phone, hidden, denied),
                mapOf("hidden" to config("hidden", isHidden = true)),
                mode,
                setOf("phone"),
            ),
        )
    }

    private fun app(packageName: String) =
        AppInfo(packageName, packageName, "Main", 0, false)

    private fun config(packageName: String, isHidden: Boolean) =
        AppConfig(packageName, "NEUTRAL", false, 0, isHidden, null, null)
}
