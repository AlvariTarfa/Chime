package com.savatech.chimelauncher.data.apps

import androidx.room.withTransaction
import com.savatech.chimelauncher.data.db.AppDatabase
import com.savatech.chimelauncher.data.db.dao.AppConfigDao
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.domain.model.AppCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface AppConfigResult {
    data object Updated : AppConfigResult
    data object PinLimitExceeded : AppConfigResult
    data object DuplicatePackage : AppConfigResult
}

fun interface AppConfigSource {
    suspend fun getConfig(packageName: String): AppConfig?
    suspend fun getAllConfigs(): Map<String, AppConfig> = emptyMap()
}

interface AppClassificationStore {
    suspend fun getAllConfigs(): Map<String, AppConfig>
    suspend fun setCategories(categories: Map<String, AppCategory>)
}

internal data class PinUpdate(
    val packageNames: List<String>?,
    val result: AppConfigResult,
)

internal fun updatePinnedPackages(
    pinnedPackageNames: List<String>,
    packageName: String,
    pinned: Boolean,
): PinUpdate {
    val current = pinnedPackageNames.distinct()
    if (!pinned) {
        return PinUpdate(current.filterNot { it == packageName }, AppConfigResult.Updated)
    }
    if (packageName in current) return PinUpdate(current, AppConfigResult.Updated)
    if (current.size >= MAX_PINNED_APPS) {
        return PinUpdate(null, AppConfigResult.PinLimitExceeded)
    }
    return PinUpdate(current + packageName, AppConfigResult.Updated)
}

internal fun reorderPinnedPackages(packageNames: List<String>): PinUpdate {
    if (packageNames.size > MAX_PINNED_APPS) {
        return PinUpdate(null, AppConfigResult.PinLimitExceeded)
    }
    if (packageNames.distinct().size != packageNames.size) {
        return PinUpdate(null, AppConfigResult.DuplicatePackage)
    }
    return PinUpdate(packageNames.toList(), AppConfigResult.Updated)
}

internal const val MAX_PINNED_APPS = 4
private const val MAX_DAILY_LIMIT_MINUTES = 720

@Singleton
class AppConfigRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: AppConfigDao,
) : AppConfigSource, AppClassificationStore {
    override suspend fun getConfig(packageName: String): AppConfig? = dao.getById(packageName)

    override suspend fun getAllConfigs(): Map<String, AppConfig> =
        dao.getAll().associateBy(AppConfig::packageName)

    override suspend fun setCategories(categories: Map<String, AppCategory>) {
        database.withTransaction {
            val existing = dao.getAll().associateBy(AppConfig::packageName)
            categories.forEach { (packageName, category) ->
                save(
                    existing[packageName].orDefault(packageName).copy(category = category.name),
                )
            }
        }
    }

    fun observeAll(): Flow<Map<String, AppConfig>> =
        dao.observeAll().map { configs -> configs.associateBy(AppConfig::packageName) }

    suspend fun setPinned(packageName: String, pinned: Boolean): AppConfigResult =
        database.withTransaction {
            val configs = dao.getAll()
            val currentPins = configs.filter(AppConfig::pinned)
                .sortedBy(AppConfig::pinOrder)
                .map(AppConfig::packageName)
            val update = updatePinnedPackages(currentPins, packageName, pinned)
            val packageNames = update.packageNames ?: return@withTransaction update.result
            packageNames.forEachIndexed { index, name ->
                save(configs.firstOrNull { it.packageName == name }.orDefault(name).copy(
                    pinned = true,
                    pinOrder = index,
                ))
            }
            if (!pinned) {
                configs.filter { it.pinned && it.packageName !in packageNames }.forEach {
                    save(it.copy(pinned = false, pinOrder = 0))
                }
            }
            update.result
        }

    suspend fun reorderPinned(packageNames: List<String>): AppConfigResult {
        val update = reorderPinnedPackages(packageNames)
        val orderedPackageNames = update.packageNames ?: return update.result
        return database.withTransaction {
            val configs = dao.getAll()
            configs.filter { it.pinned && it.packageName !in orderedPackageNames }.forEach {
                save(it.copy(pinned = false, pinOrder = 0))
            }
            orderedPackageNames.forEachIndexed { index, name ->
                save(configs.firstOrNull { it.packageName == name }.orDefault(name).copy(
                    pinned = true,
                    pinOrder = index,
                ))
            }
            AppConfigResult.Updated
        }
    }

    suspend fun setHidden(packageName: String, hidden: Boolean) {
        update(packageName) { it.copy(hidden = hidden) }
    }

    suspend fun setCategory(packageName: String, category: AppCategory) {
        update(packageName) { it.copy(category = category.name) }
    }

    suspend fun setDailyLimit(packageName: String, minutes: Int?) {
        require(minutes == null || minutes in 1..MAX_DAILY_LIMIT_MINUTES)
        update(packageName) { it.copy(dailyLimitMin = minutes) }
    }

    suspend fun setLinkedGoal(packageName: String, goalId: String?) {
        update(packageName) { it.copy(linkedGoalId = goalId) }
    }

    private suspend fun update(packageName: String, transform: (AppConfig) -> AppConfig) {
        database.withTransaction {
            save(transform(dao.getById(packageName).orDefault(packageName)))
        }
    }

    private suspend fun save(config: AppConfig) {
        if (dao.getById(config.packageName) == null) {
            dao.insert(config)
        } else {
            dao.update(config)
        }
    }

    private fun AppConfig?.orDefault(packageName: String): AppConfig =
        this ?: AppConfig(
            packageName = packageName,
            category = AppCategory.NEUTRAL.name,
            pinned = false,
            pinOrder = 0,
            hidden = false,
            dailyLimitMin = null,
            linkedGoalId = null,
        )
}
