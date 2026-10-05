package com.savatech.chimelauncher.data.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigRepositoryTest {
    @Test
    fun fifthPinIsRejectedAndUnpinningFreesASlot() {
        var pins = emptyList<String>()
        listOf("one", "two", "three", "four").forEach { packageName ->
            val update = updatePinnedPackages(pins, packageName, pinned = true)
            assertEquals(AppConfigResult.Updated, update.result)
            pins = requireNotNull(update.packageNames)
        }

        val fifthPin = updatePinnedPackages(pins, "five", pinned = true)
        assertEquals(AppConfigResult.PinLimitExceeded, fifthPin.result)
        assertEquals(4, pins.size)

        pins = requireNotNull(updatePinnedPackages(pins, "two", pinned = false).packageNames)
        val availableSlot = updatePinnedPackages(pins, "five", pinned = true)
        assertEquals(AppConfigResult.Updated, availableSlot.result)
        assertEquals(listOf("one", "three", "four", "five"), availableSlot.packageNames)
    }

    @Test
    fun reorderChangesOrderWithoutChangingPinCount() {
        val pins = listOf("one", "two", "three", "four")

        val update = reorderPinnedPackages(listOf("four", "two", "one", "three"))

        assertEquals(AppConfigResult.Updated, update.result)
        assertEquals(4, update.packageNames?.size)
        assertEquals(listOf("four", "two", "one", "three"), update.packageNames)
    }
}
