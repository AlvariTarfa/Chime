package com.savatech.chimelauncher.data.intercept

import com.savatech.chimelauncher.data.db.dao.AppGrantDao
import com.savatech.chimelauncher.data.db.dao.InterceptEventDao
import com.savatech.chimelauncher.data.db.entities.AppGrant
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import com.savatech.chimelauncher.domain.model.InterceptOutcome
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface InterceptRepository {
    suspend fun hasActiveGrant(packageName: String): Boolean
    suspend fun openedTodayCount(packageName: String): Int
    suspend fun logEvent(
        packageName: String,
        outcome: InterceptOutcome,
        grantedMinutes: Int? = null,
        reason: String? = null,
    )
    suspend fun createGrant(packageName: String, minutes: Int): AppGrant
    suspend fun eventsBetween(startInclusive: Long, endExclusive: Long): List<InterceptEvent>
}

@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindInterceptRepository(repository: RoomInterceptRepository): InterceptRepository
}

@Singleton
class RoomInterceptRepository @Inject constructor(
    private val grantDao: AppGrantDao,
    private val eventDao: InterceptEventDao,
    private val clock: Clock,
) : InterceptRepository {
    override suspend fun eventsBetween(
        startInclusive: Long,
        endExclusive: Long,
    ): List<InterceptEvent> = eventDao.getBetween(startInclusive, endExclusive)

    override suspend fun hasActiveGrant(packageName: String): Boolean {
        val now = clock.millis()
        grantDao.deleteExpired(now)
        return grantDao.getActiveByPackage(packageName, now) != null
    }

    override suspend fun openedTodayCount(packageName: String): Int {
        val window = localDayWindow(LocalDate.now(clock), clock)
        return eventDao.countOpenedBetween(packageName, window.first, window.second)
    }

    override suspend fun logEvent(
        packageName: String,
        outcome: InterceptOutcome,
        grantedMinutes: Int?,
        reason: String?,
    ) {
        eventDao.insert(
            InterceptEvent(
                packageName = packageName,
                timestamp = clock.millis(),
                outcome = outcome.name,
                grantedMinutes = grantedMinutes,
                reason = reason,
            ),
        )
    }

    override suspend fun createGrant(packageName: String, minutes: Int): AppGrant {
        require(minutes > 0) { "Grant duration must be positive." }
        val now = clock.millis()
        grantDao.deleteExpired(now)
        val grant = AppGrant(packageName, now + minutes * MILLIS_PER_MINUTE)
        grantDao.upsert(grant)
        return grant
    }
}

internal fun localDayWindow(date: LocalDate, clock: Clock): Pair<Long, Long> =
    date.atStartOfDay(clock.zone).toInstant().toEpochMilli() to
        date.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

private const val MILLIS_PER_MINUTE = 60_000L
