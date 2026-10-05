package com.savatech.chimelauncher.data.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class IconCacheTest {
    @Test
    fun cacheKeyIncludesComponentAndUserProfile() {
        val app = AppInfo("Mail", "com.example.mail", "com.example.mail.MainActivity", 42, true)

        assertEquals("com.example.mail/com.example.mail.MainActivity/42", iconCacheKey(app))
    }
}
