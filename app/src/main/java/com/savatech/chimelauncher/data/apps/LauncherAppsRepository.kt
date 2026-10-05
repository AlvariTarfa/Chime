package com.savatech.chimelauncher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.content.ActivityNotFoundException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
@Singleton
class LauncherAppsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AppRepository, AutoCloseable {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val ownPackageName = context.packageName
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private val mutableApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val mutableIsLoaded = MutableStateFlow(false)

    override val apps: StateFlow<List<AppInfo>> = mutableApps.asStateFlow()
    override val isLoaded: StateFlow<Boolean> = mutableIsLoaded.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = requestRefresh()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = requestRefresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = requestRefresh()
        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = requestRefresh()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = requestRefresh()
    }

    init {
        launcherApps.registerCallback(callback)
        scope.launch {
            refreshRequests.receiveAsFlow().debounce(300).collect {
                refreshApps()
            }
        }
        requestRefresh()
    }

    override suspend fun launch(app: AppInfo): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.Default) {
        try {
            val user = userManager.getUserForSerialNumber(app.userSerial)
                ?: return@withContext Result.failure(
                    IllegalStateException("The app's user profile is no longer available."),
                )
            launcherApps.startMainActivity(
                ComponentName(app.packageName, app.className),
                user,
                null,
                null,
            )
            Result.success(Unit)
        } catch (exception: ActivityNotFoundException) {
            Result.failure(exception)
        } catch (exception: SecurityException) {
            Result.failure(exception)
        }
    }

    private fun requestRefresh() {
        refreshRequests.trySend(Unit)
    }

    private fun refreshApps() {
        val currentUser = Process.myUserHandle()
        val profiles = launcherApps.profiles
        val discoveredApps = profiles.flatMap { user ->
            val userSerial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user)
                .asSequence()
                .filterNot { it.applicationInfo.packageName == ownPackageName }
                .map { activity ->
                    AppInfo(
                        label = activity.label.toString(),
                        packageName = activity.applicationInfo.packageName,
                        className = activity.name,
                        userSerial = userSerial,
                        isWorkProfile = user != currentUser,
                        isSystemApp = activity.applicationInfo.flags and
                            (android.content.pm.ApplicationInfo.FLAG_SYSTEM or
                                android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    )
                }
                .toList()
        }
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }
        mutableApps.value = discoveredApps.sortedWith { left, right ->
            collator.compare(left.label, right.label)
        }
        mutableIsLoaded.value = true
    }

    override fun close() {
        launcherApps.unregisterCallback(callback)
        scope.cancel()
        refreshRequests.close()
    }
}
