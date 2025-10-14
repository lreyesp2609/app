package com.example.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderType {
    LOCATION, DATETIME, BOTH
}

enum class TriggerType {
    ENTER, EXIT, BOTH
}

enum class SoundType {
    DEFAULT, GENTLE, ALERT, CHIME
}

data class ReminderResponse(
    val id: Int,
    val user_id: Int,
    val title: String,
    val description: String? = null,
    val reminder_type: String,
    val trigger_type: String,
    val vibration: Boolean = false,
    val sound: Boolean = false,
    val sound_type: String? = null,
    val days: String? = null,
    val time: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Double?,
    val is_active: Boolean = true,
    val is_deleted: Boolean = false
)

data class Reminder(
    val id: Int? = null,
    val user_id: Int? = null,
    val title: String,
    val description: String? = null,
    val reminder_type: String,
    val trigger_type: String,
    val vibration: Boolean = false,
    val sound: Boolean = false,
    val sound_type: String? = null,
    val days: List<String>? = null,
    val time: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Double?,
    val is_active: Boolean = true,
    val is_deleted: Boolean = false
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String?,
    val reminder_type: String,
    val trigger_type: String,
    val sound_type: String?,
    val vibration: Boolean,
    val sound: Boolean,
    val days: String?,
    val time: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Float?,
    val user_id: Int,
    val is_active: Boolean = true,
    val is_deleted: Boolean = false
)

fun ReminderResponse.toReminder(): Reminder {
    return Reminder(
        id = id,
        user_id = user_id,
        title = title,
        description = description,
        reminder_type = reminder_type,
        trigger_type = trigger_type,
        vibration = vibration,
        sound = sound,
        sound_type = sound_type,
        days = days?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
        time = time,
        location = location,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        is_active = is_active,
        is_deleted = is_deleted
    )
}

fun Reminder.toReminderResponse(): ReminderResponse {
    return ReminderResponse(
        id = id ?: 0,
        user_id = user_id ?: 0,
        title = title,
        description = description,
        reminder_type = reminder_type,
        trigger_type = trigger_type,
        vibration = vibration,
        sound = sound,
        sound_type = sound_type,
        days = days?.joinToString(","),
        time = time,
        location = location,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        is_active = is_active,
        is_deleted = is_deleted
    )
}

fun ReminderEntity.toReminder(): Reminder {
    return Reminder(
        id = id,
        user_id = user_id,
        title = title,
        description = description,
        reminder_type = reminder_type,
        trigger_type = trigger_type,
        vibration = vibration,
        sound = sound,
        sound_type = sound_type,
        days = days?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
        time = time,
        location = location,
        latitude = latitude,
        longitude = longitude,
        radius = radius?.toDouble(),
        is_active = is_active,
        is_deleted = is_deleted
    )
}