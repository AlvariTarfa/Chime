package com.savatech.chimelauncher.data.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.RequiresApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

fun interface UsageAccess {
    fun isGranted(): Boolean
}

interface OnboardingUsageAccess {
    val hasPermission: StateFlow<Boolean>
    fun refresh()
    fun settingsIntent(): Intent
}

@Singleton
class UsagePermission @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : UsageAccess, OnboardingUsageAccess {
    private val appOpsManager =
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val permissionState = MutableStateFlow(checkPermission())

    override val hasPermission: StateFlow<Boolean> = permissionState

    override fun refresh() {
        permissionState.value = checkPermission()
    }

    override fun isGranted(): Boolean = checkPermission()

    override fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }

    private fun checkPermission(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= API_BAKLAVA) {
            checkOpWithAttribution()
        } else {
            // API 26–35 use the uid/package overload; the attribution-tag overload requires API 36.
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @RequiresApi(API_BAKLAVA)
    private fun checkOpWithAttribution(): Int = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
        context.attributionTag,
    )

    private companion object {
        const val API_BAKLAVA = 36
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UsagePermissionModule {
    @Binds
    @Singleton
    abstract fun bindUsageAccess(permission: UsagePermission): UsageAccess

    @Binds
    @Singleton
    abstract fun bindOnboardingUsageAccess(permission: UsagePermission): OnboardingUsageAccess
}
