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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun iconCacheKey(app: AppInfo): String = app.key

@Singleton
class IconCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(android.os.UserManager::class.java)
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16).coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1),
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
    }

    suspend fun get(app: AppInfo, sizePx: Int = DEFAULT_ICON_SIZE_PX): ImageBitmap? =
        withContext(Dispatchers.Default) {
            require(sizePx > 0) { "Icon size must be positive." }
            val key = iconCacheKey(app)
            cache.get(key)?.takeIf { it.width == sizePx && it.height == sizePx }
                ?.let { return@withContext it.asImageBitmap() }

            val user = userManager.getUserForSerialNumber(app.userSerial) ?: return@withContext null
            val activity = launcherApps.getActivityList(app.packageName, user)
                .firstOrNull { it.name == app.className }
                ?: return@withContext null
            val bitmap = activity.getBadgedIcon(0).toBitmap(sizePx)
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
