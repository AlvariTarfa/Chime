package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.data.apps.AppConfigRepository
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.focus.AlwaysAllowedPackages
import com.savatech.chimelauncher.data.focus.FocusRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

fun combineVisibleApps(
    apps: List<AppInfo>,
    configurations: Map<String, AppConfig>,
): List<AppInfo> = combineVisibleApps(apps, configurations, null, emptySet())

fun combineVisibleApps(
    apps: List<AppInfo>,
    configurations: Map<String, AppConfig>,
    activeMode: FocusMode?,
    alwaysAllowedPackages: Set<String>,
): List<AppInfo> = apps.filter { app ->
    configurations[app.packageName]?.hidden != true &&
        (activeMode == null || app.packageName in alwaysAllowedPackages ||
            app.packageName in activeMode.allowedPackages.decodePackageList())
}

class ObserveVisibleAppsUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val appConfigRepository: AppConfigRepository,
    private val focusRepository: FocusRepository,
    private val alwaysAllowedPackages: AlwaysAllowedPackages,
) {
    operator fun invoke(): Flow<List<AppInfo>> = flow {
        val alwaysAllowed = alwaysAllowedPackages.resolve()
        emitAll(
            kotlinx.coroutines.flow.combine(
                appRepository.apps,
                appConfigRepository.observeAll(),
                focusRepository.activeMode,
            ) { apps, configurations, activeMode ->
                combineVisibleApps(apps, configurations, activeMode, alwaysAllowed)
            },
        )
    }
}

private fun String.decodePackageList(): Set<String> =
    kotlinx.serialization.json.Json.decodeFromString<List<String>>(this).toSet()
