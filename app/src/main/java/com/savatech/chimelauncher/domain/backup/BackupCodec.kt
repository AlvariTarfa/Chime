package com.savatech.chimelauncher.domain.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(backup: BackupModel): String = json.encodeToString(backup)

    fun decode(source: String): BackupModel {
        val backup = try {
            json.decodeFromString<BackupModel>(source)
        } catch (exception: SerializationException) {
            throw BackupError.MalformedJson(exception)
        } catch (exception: IllegalArgumentException) {
            throw BackupError.MalformedJson(exception)
        }
        when {
            backup.schemaVersion > CURRENT_SCHEMA_VERSION ->
                throw BackupError.NewerSchemaVersion(backup.schemaVersion, CURRENT_SCHEMA_VERSION)
            backup.schemaVersion <= 0 -> throw BackupError.InvalidSchemaVersion(backup.schemaVersion)
        }
        return backup
    }

    const val CURRENT_SCHEMA_VERSION = 1
}
