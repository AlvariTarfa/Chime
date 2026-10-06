package com.savatech.chimelauncher.data.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.content.ComponentName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun iconCacheKey(app: AppInfo): String = app.key

@Singleton
class IconCache @Inject constructor(
    @ApplicationContext context: Context,
    private val iconPackRepository: IconPackRepository,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(android.os.UserManager::class.java)
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16).coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1),
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
    }

    private var selectedPack: String? = null

    suspend fun get(
        app: AppInfo,
        sizePx: Int = DEFAULT_ICON_SIZE_PX,
        iconPackPackage: String? = null,
    ): ImageBitmap? =
        withContext(Dispatchers.Default) {
            require(sizePx > 0) { "Icon size must be positive." }
            val effectivePack = iconPackPackage?.takeIf { iconPackRepository.isInstalled(it) }
            synchronized(cache) {
                if (selectedPack != effectivePack) {
                    cache.evictAll()
                    selectedPack = effectivePack
                }
            }
            val key = "${effectivePack.orEmpty()}:${iconCacheKey(app)}:$sizePx"
            cache.get(key)?.takeIf { it.width == sizePx && it.height == sizePx }
                ?.let { return@withContext it.asImageBitmap() }

            val bitmap = effectivePack?.let { pack ->
                iconPackRepository.iconBitmap(
                    pack,
                    ComponentName(app.packageName, app.className),
                    sizePx,
                )
            } ?: run {
                val user = userManager.getUserForSerialNumber(app.userSerial)
                    ?: return@withContext null
                val activity = launcherApps.getActivityList(app.packageName, user)
                    .firstOrNull { it.name == app.className }
                    ?: return@withContext null
                activity.getBadgedIcon(0).toBitmap(sizePx)
            }
            cache.put(key, bitmap)
            bitmap.asImageBitmap()
        }

    private fun Drawable.toBitmap(sizePx: Int): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val previousBounds = bounds
        try {
            setBounds(0, 0, sizePx, sizePx)
            draw(canvas)
        } finally {
            setBounds(previousBounds)
        }
        return bitmap
    }

    private companion object {
        const val DEFAULT_ICON_SIZE_PX = 96
    }
}
