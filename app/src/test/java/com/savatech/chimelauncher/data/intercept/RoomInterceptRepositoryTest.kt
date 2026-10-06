package com.savatech.chimelauncher.data.intercept

import com.savatech.chimelauncher.data.db.dao.AppGrantDao
import com.savatech.chimelauncher.data.db.dao.InterceptEventDao
import com.savatech.chimelauncher.data.db.entities.AppGrant
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RoomInterceptRepositoryTest {
    @Test
    fun openedCountUsesLocalDayStartAndExcludesTheNextDayBoundary() = runBlocking {
        val zone = ZoneId.of("America/Los_Angeles")
        val date = LocalDate.of(2026, 3, 8)
        val clock = Clock.fixed(Instant.parse("2026-03-08T19:00:00Z"), zone)
        val window = localDayWindow(date, clock)
        val eventDao = FakeEventDao().apply {
            events += InterceptEvent(packageName = PKG, timestamp = window.first - 1, outcome = "OPENED",
                grantedMinutes = null, reason = null)
            events += InterceptEvent(packageName = PKG, timestamp = window.first, outcome = "OPENED",
                grantedMinutes = null, reason = null)
            events += InterceptEvent(packageName = PKG, timestamp = window.second - 1, outcome = "OPENED",
                grantedMinutes = 5, reason = null)
            events += InterceptEvent(packageName = PKG, timestamp = window.second, outcome = "OPENED",
                grantedMinutes = null, reason = null)
            events += InterceptEvent(packageName = PKG, timestamp = window.first, outcome = "CANCELLED",
                grantedMinutes = null, reason = null)
        }
        val repository = RoomInterceptRepository(FakeGrantDao(), eventDao, clock)

        assertEquals(2, repository.openedTodayCount(PKG))
        assertEquals(23L * 60L * 60L * 1_000L, window.second - window.first)
    }

    @Test
    fun expiredGrantIsDeletedAndNotConsideredActive() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneId.of("UTC"))
        val grants = FakeGrantDao().apply {
            values[PKG] = AppGrant(PKG, clock.millis())
        }
        val repository = RoomInterceptRepository(grants, FakeEventDao(), clock)

        assertFalse(repository.hasActiveGrant(PKG))
        assertEquals(null, grants.values[PKG])
    }

    private class FakeGrantDao : AppGrantDao {
        val values = mutableMapOf<String, AppGrant>()
        override suspend fun insert(appGrant: AppGrant): Long {
            values[appGrant.packageName] = appGrant
            return 1
        }
        override suspend fun update(appGrant: AppGrant): Int = 0
        override suspend fun delete(appGrant: AppGrant): Int = if (values.remove(appGrant.packageName) != null) 1 else 0
        override suspend fun getById(packageName: String): AppGrant? = values[packageName]
        override suspend fun getActiveByPackage(packageName: String, now: Long): AppGrant? =
            values[packageName]?.takeIf { it.expiresAt > now }
        override suspend fun upsert(appGrant: AppGrant): Long {
            values[appGrant.packageName] = appGrant
            return 1
        }
        override suspend fun deleteExpired(now: Long) {
            values.entries.removeAll { it.value.expiresAt <= now }
        }
        override fun observeAll(): Flow<List<AppGrant>> = MutableStateFlow(values.values.toList())
    }

    private class FakeEventDao : InterceptEventDao {
        val events = mutableListOf<InterceptEvent>()
        override suspend fun insert(interceptEvent: InterceptEvent): Long {
            events += interceptEvent.copy(id = events.size.toLong() + 1)
            return events.size.toLong()
        }
        override suspend fun update(interceptEvent: InterceptEvent): Int = 0
        override suspend fun delete(interceptEvent: InterceptEvent): Int = 0
        override suspend fun getById(id: Long): InterceptEvent? = events.firstOrNull { it.id == id }
        override suspend fun getBetween(
            startInclusive: Long,
            endExclusive: Long,
        ): List<InterceptEvent> = events.filter {
            it.timestamp >= startInclusive && it.timestamp < endExclusive
        }
        override suspend fun countOpenedBetween(
            packageName: String,
            startInclusive: Long,
            endExclusive: Long,
        ): Int = events.count {
            it.packageName == packageName && it.outcome == "OPENED" &&
                it.timestamp >= startInclusive && it.timestamp < endExclusive
        }
        override fun observeAll(): Flow<List<InterceptEvent>> = MutableStateFlow(events.toList())
    }

    private companion object {
        const val PKG = "com.example.distracting"
    }
}
