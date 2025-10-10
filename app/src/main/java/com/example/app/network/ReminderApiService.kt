package com.example.app.network

import com.example.app.models.Reminder
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ReminderApiService {

    @POST("/reminders/crear")
    suspend fun createReminder(
        @Header("Authorization") token: String,
        @Body reminder: Reminder
    ): Response<Reminder>

    @GET("/reminders/listar")
    suspend fun getReminders(
        @Header("Authorization") token: String
    ): Response<List<Reminder>>
}