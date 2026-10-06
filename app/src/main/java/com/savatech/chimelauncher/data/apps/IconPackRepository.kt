package com.savatech.chimelauncher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

data class InstalledIconPack(val packageName: String, val label: String)

data class AppFilterComponent(val packageName: String, val className: String)

fun parseAppFilter(input: InputStream): Map<AppFilterComponent, String> {
    val bufferedInput = BufferedInputStream(input)
    bufferedInput.mark(1)
    if (bufferedInput.read() == -1) return emptyMap()
    bufferedInput.reset()
    val factory = SAXParserFactory.newInstance().apply {
        isNamespaceAware = false
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }
    val items = linkedMapOf<AppFilterComponent, String>()
    factory.newSAXParser().parse(bufferedInput, object : DefaultHandler() {
        override fun startElement(
            uri: String?,
            localName: String?,
            qualifiedName: String?,
            attributes: Attributes,
        ) {
            if (qualifiedName != "item") return
            val component = componentFromAppFilter(attributes.getValue("component"))
            val drawable = attributes.getValue("drawable")
            if (component != null && !drawable.isNullOrBlank()) items[component] = drawable
        }
    })
    return items
}

private fun componentFromAppFilter(value: String?): AppFilterComponent? {
    val content = value?.takeIf { it.startsWith("ComponentInfo{") && it.endsWith("}") }
        ?.removePrefix("ComponentInfo{")
        ?.removeSuffix("}")
        ?: return null
    val separator = content.indexOf('/')
    if (separator <= 0 || separator == content.lastIndex) return null
    val packageName = content.substring(0, separator)
    val className = content.substring(separator + 1)
    if (packageName.isBlank() || className.isBlank() || className.contains('/')) return null
    val normalizedClassName = if (className.startsWith(".")) "$packageName$className" else className
    return AppFilterComponent(packageName, normalizedClassName)
}

private fun Map<AppFilterComponent, String>.asComponentNameMap(): Map<ComponentName, String> =
    mapKeys { (component, _) -> ComponentName(component.packageName, component.className) }

@Singleton
class IconPackRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:com.savatech.chimelauncher.core.di.IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val packageManager: PackageManager = context.packageManager
    private val appFilterCache = ConcurrentHashMap<String, Map<ComponentName, String>>()

    suspend fun isInstalled(packPackage: String): Boolean = withContext(ioDispatcher) {
        try {
            packageManager.getResourcesForApplication(packPackage)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun discoverInstalledPacks(): List<InstalledIconPack> = withContext(ioDispatcher) {
        ICON_PACK_ACTIONS
            .flatMap { action ->
                packageManager.queryIntentActivities(Intent(action), 0).map { result ->
                    InstalledIconPack(
                        packageName = result.activityInfo.packageName,
                        label = result.activityInfo.applicationInfo.loadLabel(packageManager).toString(),
                    )
                }
            }
            .distinctBy(InstalledIconPack::packageName)
            .sortedBy(InstalledIconPack::label)
    }

    suspend fun iconBitmap(
        packPackage: String,
        component: ComponentName,
        sizePx: Int,
    ): Bitmap? = withContext(ioDispatcher) {
        try {
            val mapping = appFilterCache[packPackage] ?: loadAppFilter(packPackage).also {
                appFilterCache[packPackage] = it
            }
            val drawableName = mapping[component] ?: return@withContext null
            val resources = packageManager.getResourcesForApplication(packPackage)
            val drawableId = resources.getIdentifier(drawableName, "drawable", packPackage)
                .takeIf { it != 0 }
                ?: resources.getIdentifier(drawableName, "mipmap", packPackage)
                    .takeIf { it != 0 }
                ?: return@withContext null
            resources.getDrawable(drawableId, null).toBitmap(sizePx)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Resources.NotFoundException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: SAXException) {
            null
        } catch (_: javax.xml.parsers.ParserConfigurationException) {
            null
        }
    }

    private fun loadAppFilter(packPackage: String): Map<ComponentName, String> {
        val packContext = context.createPackageContext(packPackage, 0)
        val assetStream = try {
            packContext.assets.open("appfilter.xml")
        } catch (_: FileNotFoundException) {
            null
        }
        if (assetStream != null) {
            assetStream.use(::parseAppFilter).asComponentNameMap().let { return it }
        }
        val resources = packageManager.getResourcesForApplication(packPackage)
        val xmlId = resources.getIdentifier("appfilter", "xml", packPackage)
        if (xmlId == 0) return emptyMap()
        return resources.openRawResource(xmlId).use(::parseAppFilter).asComponentNameMap()
    }

    private fun Drawable.toBitmap(sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
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
        val ICON_PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
        )
    }
}
