package com.savatech.chimelauncher.feature.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.savatech.chimelauncher.core.di.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface DefaultHomeStatus {
    suspend fun isDefaultHome(): Boolean
}

@Singleton
class PackageManagerDefaultHomeStatus @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DefaultHomeStatus {
    override suspend fun isDefaultHome(): Boolean = withContext(ioDispatcher) {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName == context.packageName
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DefaultHomeStatusModule {
    @Binds
    @Singleton
    abstract fun bindDefaultHomeStatus(
        status: PackageManagerDefaultHomeStatus,
    ): DefaultHomeStatus
}
