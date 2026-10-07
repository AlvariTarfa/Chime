package com.savatech.chimelauncher.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.savatech.chimelauncher.domain.backup.BackupError
import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

internal class SettingsBackupAdapter(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun export(): Map<String, JsonElement> {
        val known = knownKeys
        return dataStore.data.first().asMap().mapNotNull { (key, value) ->
            if (key.name !in known) return@mapNotNull null
            val jsonValue = when (value) {
                is Boolean -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Set<*> -> JsonArray(value.filterIsInstance<String>().sorted().map { JsonPrimitive(it) })
                else -> null
            } ?: return@mapNotNull null
            key.name to jsonValue
        }.toMap()
    }

    fun validate(settings: Map<String, JsonElement>) {
        settings.forEach { (name, value) ->
            when {
                name in stringKeys && value is JsonPrimitive && value.isString &&
                    validStringValue(name, value.content) -> Unit
                name in booleanKeys && value is JsonPrimitive && value.booleanOrNull != null -> Unit
                name in integerKeys && value is JsonPrimitive && value.intOrNull != null &&
                    validIntegerValue(name, value.intOrNull) -> Unit
                name == DIGEST_ALLOW_LIST && value is JsonArray &&
                    value.all { it is JsonPrimitive && it.isString } -> Unit
                name in knownKeys -> throw BackupError.InvalidSettings(name)
            }

        }
    }

    fun recognized(settings: Map<String, JsonElement>): Map<String, JsonElement> =
        settings.filterKeys { it in knownKeys }

    suspend fun import(settings: Map<String, JsonElement>, replace: Boolean) {
        validate(settings)
        dataStore.edit { preferences ->
            if (replace) preferences.clear()
            settings.forEach { (name, value) ->
                when {
                    name in stringKeys -> preferences[stringPreferencesKey(name)] = value.jsonPrimitive.content
                    name in booleanKeys ->
                        preferences[booleanPreferencesKey(name)] = checkNotNull(value.jsonPrimitive.booleanOrNull)
                    name in integerKeys ->
                        preferences[intPreferencesKey(name)] = checkNotNull(value.jsonPrimitive.intOrNull)
                    name == DIGEST_ALLOW_LIST -> preferences[stringSetPreferencesKey(name)] =
                        value.jsonArray.map { it.jsonPrimitive.content }.toSet()
                }
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private val knownKeys: Set<String>
        get() = stringKeys + booleanKeys + integerKeys + DIGEST_ALLOW_LIST

    private val stringKeys = setOf(
        "drawer_mode",
        "friction_level",
        "morning_check_in_time",
        "evening_check_in_time",
        "weekly_check_in_day",
        "weekly_check_in_time",
        "quiet_hours_start",
        "quiet_hours_end",
        "digest_first_time",
        "digest_second_time",
        "theme_mode",
        "icon_shape",
        "font_preset",
        "layout_density",
        "icon_pack_package",
        "left_swipe_target",
        "right_swipe_target",
        "manual_mode_id",
        "manual_mode_until",
    )

    private val booleanKeys = setOf(
        "morning_check_in_enabled",
        "evening_check_in_enabled",
        "weekly_check_in_enabled",
        "nudges_enabled",
        "digest_enabled",
        "digest_consent_accepted",
        "use_dynamic_color",
        "focus_score_enabled",
        "onboarding_completed",
        "focus_modes_seeded",
        "manual_mode_override_set",
    )

    private val integerKeys =
        setOf("base_delay_seconds", "accent_color_index", "assumed_minutes_per_open") +
            AppCategory.entries.map { "category_daily_limit_${it.name.lowercase()}" }

    private fun validStringValue(name: String, value: String): Boolean = when (name) {
        "drawer_mode" -> DrawerMode.entries.any { it.name == value }
        "friction_level" -> FrictionLevel.entries.any { it.name == value }
        "morning_check_in_time", "evening_check_in_time", "weekly_check_in_time",
        "quiet_hours_start", "quiet_hours_end", "digest_first_time", "digest_second_time" ->
            parsesTime(value)
        "weekly_check_in_day" -> DayOfWeek.entries.any { it.name == value }
        "theme_mode" -> ThemeMode.entries.any { it.name == value }
        "icon_shape" -> IconShape.entries.any { it.name.equals(value, ignoreCase = true) }
        "font_preset" -> FontPreset.fromStorage(value) != FontPreset.DEFAULT ||
            value.equals("DEFAULT", ignoreCase = true)
        "layout_density" ->
            LayoutDensityPreset.entries.any { it.name.equals(value, ignoreCase = true) }
        "manual_mode_until" -> parsesDateTime(value)
        else -> true
    }

    private fun validIntegerValue(name: String, value: Int?): Boolean {
        val number = checkNotNull(value)
        return when {
            name == "accent_color_index" -> number in 0..7
            name.startsWith("category_daily_limit_") -> number in 1..720
            else -> true
        }
    }

    private fun parsesTime(value: String): Boolean = try {
        LocalTime.parse(value)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    private fun parsesDateTime(value: String): Boolean = try {
        LocalDateTime.parse(value)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    private companion object {
        const val DIGEST_ALLOW_LIST = "digest_allow_list"
    }
}
