package com.arete.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun setTypeToString(value: SetType): String = value.name

    @TypeConverter
    fun stringToSetType(value: String): SetType = SetType.valueOf(value)

    @TypeConverter
    fun sessionStatusToString(value: SessionStatus): String = value.name

    @TypeConverter
    fun stringToSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}

@Serializable
enum class SetType { WARMUP, WORK }

@Serializable
enum class SessionStatus { IN_PROGRESS, COMPLETED, DISCARDED }
