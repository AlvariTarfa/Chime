package com.savatech.chimelauncher.data.focus

import android.content.Context
import com.savatech.chimelauncher.data.db.dao.FocusModeDao
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.focus.ActiveModeResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@Singleton
class FocusRepository @Inject constructor(
    private val modeDao: FocusModeDao,
    private val settings: SettingsRepository,
    private val resolver: ActiveModeResolver,
    private val clock: Clock,
    @param:ApplicationContext private val context: Context,
) {
    fun observeModes(): Flow<List<FocusMode>> = modeDao.observeAll()

    val activeMode: Flow<FocusMode?> = combine(
        modeDao.observeAll(),
        settings.manualModeOverride,
        minuteTicker(clock),
    ) { modes, manual, now ->
        resolver.resolve(modes, manual, now)
    }

    suspend fun save(mode: FocusMode) {
        if (modeDao.getById(mode.id) == null) modeDao.insert(mode) else modeDao.update(mode)
    }

    suspend fun delete(mode: FocusMode) {
        require(!mode.isBuiltIn) { "Built-in focus modes cannot be deleted." }
        modeDao.delete(mode)
        if (settings.manualModeOverride.first()?.modeId == mode.id) {
            settings.clearManualModeOverride()
        }
    }

    suspend fun setManualMode(modeId: String?, until: LocalDateTime? = null) {
        require(modeId == null || modeDao.getById(modeId) != null) { "Unknown focus mode: $modeId" }
        settings.setManualModeOverride(modeId, until)
    }

    suspend fun clearManualMode() = settings.clearManualModeOverride()

    suspend fun seedBuiltInModesIfNeeded() {
        if (settings.focusModesSeeded.first()) return
        val existing = modeDao.observeAll().first().associateBy(FocusMode::id)
        builtIns().forEach { mode ->
            if (mode.id !in existing) modeDao.insert(mode)
        }
        settings.markFocusModesSeeded()
    }

    private fun builtIns(): List<FocusMode> =
        listOf(
            "builtin-work" to com.savatech.chimelauncher.R.string.focus_mode_work,
            "builtin-study" to com.savatech.chimelauncher.R.string.focus_mode_study,
            "builtin-sleep" to com.savatech.chimelauncher.R.string.focus_mode_sleep,
            "builtin-deep-work" to com.savatech.chimelauncher.R.string.focus_mode_deep_work,
        ).map { (id, nameResource) ->
            FocusMode(
                id = id,
                name = context.getString(nameResource),
                allowedPackages = "[]",
                scheduleJson = null,
                suppressNotifications = false,
                isBuiltIn = true,
            )
        }
}

private fun minuteTicker(clock: Clock): Flow<LocalDateTime> = flow {
    while (true) {
        emit(LocalDateTime.now(clock))
        val elapsed = Math.floorMod(clock.millis(), MILLIS_PER_MINUTE)
        delay(MILLIS_PER_MINUTE - elapsed)
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
