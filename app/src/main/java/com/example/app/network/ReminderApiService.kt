package com.example.app.network

import com.example.app.models.Reminder
import com.example.app.models.ReminderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ReminderApiService {

    @POST("/reminders/crear")
    suspend fun createReminder(
        @Header("Authorization") token: String,
        @Body reminder: ReminderResponse
    ): Response<ReminderResponse>

    @GET("/reminders/listar")
    suspend fun getReminders(
        @Header("Authorization") token: String
    ): Response<List<ReminderResponse>>

    @PATCH("/reminders/{reminder_id}/toggle")
    suspend fun toggleReminder(
        @Header("Authorization") token: String,
        @Path("reminder_id") reminderId: Int
    ): Response<ReminderResponse>

    @DELETE("/reminders/{reminder_id}/delete")
    suspend fun deleteReminder(
        @Header("Authorization") token: String,
        @Path("reminder_id") reminderId: Int
    ): Response<Unit>
}