package com.savatech.chimelauncher.data.focus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import com.savatech.chimelauncher.core.di.IoDispatcher
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AlwaysAllowedPackages @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun resolve(): Set<String> = withContext(ioDispatcher) {
        buildSet {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
                ?.let(::add)
            Telephony.Sms.getDefaultSmsPackage(context)?.let(::add)
            context.packageManager.resolveActivity(
                Intent(Settings.ACTION_SETTINGS),
                0,
            )?.activityInfo?.packageName?.let(::add)
        }
    }
}
