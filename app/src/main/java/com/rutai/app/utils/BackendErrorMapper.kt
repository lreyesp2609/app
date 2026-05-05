package com.rutai.app.utils

import android.content.Context
import com.rutai.app.R

object BackendErrorMapper {
    fun resolve(context: Context, code: String?): String {
        if (code == null) return context.getString(R.string.error_generic_message, "Unknown")
        
        // Limpiar el código si viene envuelto en JSON o tiene espacios
        val rawCode = code.trim()
            .removePrefix("{\"detail\":\"")
            .removeSuffix("\"}")
            .removePrefix("detail=") // Por si acaso viene de un toString() de Map o similar

        val resId = when (rawCode) {
            // Zonas
            "ZONE_NOT_FOUND" -> R.string.error_zone_not_found
            "INVALID_COORDINATES" -> R.string.error_invalid_coordinates
            "ZONE_ALREADY_OWNED" -> R.string.error_zone_already_owned
            "ZONE_NAME_ALREADY_EXISTS" -> R.string.error_zone_name_exists
            "ZONE_NOT_FOUND_OR_FORBIDDEN" -> R.string.error_zone_not_found_or_forbidden
            "ZONE_MARK_ERROR" -> R.string.error_zone_mark
            "ZONES_FETCH_ERROR" -> R.string.error_zones_fetch
            "ZONE_UPDATE_ERROR" -> R.string.error_zone_update
            "ZONE_DELETE_ERROR" -> R.string.error_zone_delete
            "ZONE_STATUS_UPDATED" -> R.string.success_zone_status_updated
            "ZONE_STATUS_CHANGE_ERROR" -> R.string.error_zone_status_change
            "ZONE_ADOPT_ERROR" -> R.string.error_zone_adopt
            "SUGGESTED_ZONES_FETCH_ERROR" -> R.string.error_suggested_zones_fetch
            "SUGGESTED_ZONE_NOT_FOUND" -> R.string.error_suggested_zone_not_found
            
            // Grupos
            "GROUP_NAME_ALREADY_EXISTS" -> R.string.error_group_name_exists
            "INVALID_INVITATION_CODE" -> R.string.error_invalid_code
            "ALREADY_IN_GROUP" -> R.string.error_already_in_group
            "CREATOR_ALREADY_IN_GROUP" -> R.string.error_creator_already_in_group
            "CREATOR_CANNOT_LEAVE" -> R.string.error_creator_cannot_leave
            "GROUP_NOT_FOUND" -> R.string.error_group_not_found
            "NOT_IN_GROUP" -> R.string.error_not_in_group
            "GROUP_LEFT_SUCCESS" -> R.string.success_group_left
            "GROUP_DELETED_SUCCESS" -> R.string.success_group_deleted
            "ONLY_CREATOR_CAN_DELETE" -> R.string.error_only_creator_can_delete
            
            // Recordatorios
            "REMINDER_TITLE_DUPLICATE" -> R.string.error_reminder_duplicate
            "REMINDER_NOT_FOUND" -> R.string.error_reminder_not_found
            "REMINDER_CREATE_ERROR" -> R.string.error_reminder_create
            "REMINDER_UPDATE_ERROR" -> R.string.error_reminder_update
            "REMINDER_DELETED_SUCCESS" -> R.string.success_reminder_deleted
            
            // Auth / sesión
            "TOKEN_EXPIRED" -> R.string.error_session_expired
            "INVALID_TOKEN" -> R.string.error_invalid_credentials_short
            "USER_NOT_FOUND" -> R.string.error_user_not_found
            "USER_ALREADY_EXISTS" -> R.string.error_user_already_exists
            
            // Ubicaciones
            "LOCATION_NOT_FOUND" -> R.string.error_location_not_found
            "INVALID_TRANSPORT_TYPE" -> R.string.error_invalid_transport
            "ROUTE_NOT_FOUND" -> R.string.error_route_not_found
            
            // FCM
            "FCM_TOKEN_UPDATED" -> R.string.success_fcm_updated
            "FCM_TOKEN_NOT_FOUND" -> R.string.error_fcm_not_found
            
            else -> null
        }
        
        return if (resId != null) context.getString(resId)
        else context.getString(R.string.error_generic_message, rawCode)
    }
}
