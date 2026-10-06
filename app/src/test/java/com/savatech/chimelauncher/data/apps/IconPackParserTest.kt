package com.savatech.chimelauncher.data.apps

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPackParserTest {
    @Test
    fun parsesValidItem() {
        val parsed = parse("""<resources><item component="ComponentInfo{com.example.app/com.example.app.Main}" drawable="example_icon"/></resources>""")

        assertEquals(
            mapOf(AppFilterComponent("com.example.app", "com.example.app.Main") to "example_icon"),
            parsed,
        )
    }

    @Test
    fun skipsMalformedComponentStrings() {
        val parsed = parse("""<resources><item component="not-a-component" drawable="icon"/></resources>""")

        assertTrue(parsed.isEmpty())
    }

    @Test
    fun skipsItemsWithoutDrawableAttribute() {
        val parsed = parse("""<resources><item component="ComponentInfo{com.example.app/.Main}"/></resources>""")

        assertTrue(parsed.isEmpty())
    }

    @Test
    fun laterDuplicateComponentReplacesEarlierDrawable() {
        val parsed = parse(
            """<resources><item component="ComponentInfo{com.example.app/.Main}" drawable="first"/><item component="ComponentInfo{com.example.app/.Main}" drawable="second"/></resources>""",
        )

        assertEquals("second", parsed[AppFilterComponent("com.example.app", "com.example.app.Main")])
    }

    @Test
    fun emptyFileProducesEmptyMap() {
        assertTrue(parse("").isEmpty())
    }

    private fun parse(xml: String) = parseAppFilter(ByteArrayInputStream(xml.toByteArray()))
}
