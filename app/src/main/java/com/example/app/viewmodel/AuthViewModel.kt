package com.example.app.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.models.Reminder
import com.example.app.models.ReminderEntity
import com.example.app.models.User
import com.example.app.models.toReminder
import com.example.app.network.AppDatabase
import com.example.app.network.RetrofitClient
import com.example.app.repository.AuthRepository
import com.example.app.repository.ReminderRepository
import com.example.app.screen.recordatorios.components.ReminderReceiver
import com.example.app.screen.recordatorios.components.scheduleReminder
import com.example.app.services.LocationReminderService
import com.example.app.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class AuthViewModel(private val context: Context) : ViewModel() {
    private val repository = AuthRepository()
    private val sessionManager = SessionManager.getInstance(context)

    companion object {
        private const val TAG = "WS_SessionManager"
    }

    // Estados de la UI
    var isLoading by mutableStateOf(false)
        private set
    var user by mutableStateOf<User?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set
    var accessToken by mutableStateOf<String?>(null)
        private set

    init {
        // 🔹 Restaurar estado inmediatamente desde SharedPreferences
        restoreStateFromPrefs()
        restoreSession()
        startAutoRefresh()
    }

    // 🔹 Restaurar estado desde SharedPreferences
    private fun restoreStateFromPrefs() {
        isLoggedIn = sessionManager.isLoggedIn()
        accessToken = sessionManager.getAccessToken()
        user = sessionManager.getUser()
    }

    // 🔹 Restaurar sesión automáticamente al iniciar la app
    private fun restoreSession() {
        val savedRefresh = sessionManager.getRefreshToken()
        if (savedRefresh != null && sessionManager.hasValidSession()) {
            viewModelScope.launch {
                isLoading = true
                repository.refreshToken(savedRefresh).fold(
                    onSuccess = { response ->
                        accessToken = response.accessToken
                        isLoggedIn = true
                        sessionManager.saveTokens(response.accessToken, response.refreshToken)
                        sessionManager.saveLoginState(true)

                        if (user == null) {
                            getCurrentUser {
                                // ✅ Cambiado: usar context en lugar de applicationContext
                                restoreUserReminders(context, response.accessToken)
                            }
                        } else {
                            // ✅ Cambiado: usar context en lugar de applicationContext
                            restoreUserReminders(context, response.accessToken)
                            isLoading = false
                        }
                    },
                    onFailure = {
                        clearLocalSession()
                        isLoading = false
                    }
                )
            }
        } else if (!sessionManager.hasValidSession()) {
            clearLocalSession()
            isLoading = false
        } else {
            isLoading = false
        }
    }

    // 🔹 Función de login (actualizada)
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null // Limpiar errores anteriores

            repository.login(email, password, Build.MODEL, getAppVersion(context), obtenerIp())
                .fold(
                    onSuccess = { loginResponse ->
                        accessToken = loginResponse.accessToken
                        isLoggedIn = true

                        sessionManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                        sessionManager.saveLoginState(true)

                        getCurrentUser {
                            // ✅ NUEVO: Restaurar recordatorios después del login
                            restoreUserReminders(context, loginResponse.accessToken)
                            onSuccess()
                        }                    },
                    onFailure = { exception ->
                        isLoggedIn = false
                        sessionManager.saveLoginState(false)
                        isLoading = false
                        errorMessage = when {
                            exception.message?.contains("INVALID_CREDENTIALS") == true -> {
                                "Correo o contraseña incorrectos"
                            }
                            exception.message?.contains("USER_NOT_FOUND") == true -> {
                                "No existe una cuenta con este correo"
                            }
                            exception.message?.contains("ACCOUNT_LOCKED") == true -> {
                                "Cuenta bloqueada. Contacta soporte"
                            }
                            exception.message?.contains("NETWORK_ERROR") == true -> {
                                "Error de conexión. Verifica tu internet"
                            }
                            exception.message?.contains("401") == true -> {
                                "Credenciales inválidas"
                            }
                            exception.message?.contains("500") == true -> {
                                "Error del servidor. Inténtalo más tarde"
                            }
                            else -> {
                                "Error al iniciar sesión. Inténtalo nuevamente"
                            }
                        }
                    }
                )
        }
    }
    private fun restoreUserReminders(context: Context, token: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Restaurando recordatorios del usuario...")

                val database = AppDatabase.getDatabase(context)
                val repository = ReminderRepository(database.reminderDao())

                // 1️⃣ Obtener recordatorios desde la API
                val response = RetrofitClient.reminderService.getReminders("Bearer $token")

                if (response.isSuccessful) {
                    val apiReminders = response.body() ?: emptyList()
                    Log.d(TAG, "📥 ${apiReminders.size} recordatorios obtenidos de la API")

                    // 2️⃣ Guardar en BD local y reprogramar alarmas
                    apiReminders.forEach { reminderResponse ->
                        val reminder = reminderResponse.toReminder()

                        // Convertir a ReminderEntity
                        val reminderEntity = ReminderEntity(
                            id = reminderResponse.id,
                            title = reminderResponse.title,
                            description = reminderResponse.description,
                            reminder_type = reminderResponse.reminder_type,
                            trigger_type = reminderResponse.trigger_type,
                            sound_type = reminderResponse.sound_type,
                            vibration = reminderResponse.vibration,
                            sound = reminderResponse.sound,
                            days = reminderResponse.days,
                            time = reminderResponse.time,
                            location = reminderResponse.location,
                            latitude = reminderResponse.latitude,
                            longitude = reminderResponse.longitude,
                            radius = reminderResponse.radius?.toFloat(),
                            user_id = reminderResponse.user_id,
                            is_active = reminderResponse.is_active,
                            is_deleted = reminderResponse.is_deleted
                        )

                        // Guardar localmente
                        repository.saveReminder(reminderEntity)

                        // Reprogramar alarmas solo si está activo
                        if (reminderEntity.is_active && !reminderEntity.is_deleted) {
                            when (reminderEntity.reminder_type) {
                                "datetime" -> {
                                    reprogramarAlarmasFechaHora(context, reminder, reminderEntity)
                                }
                                "location" -> {
                                    LocationReminderService.start(context)
                                }
                                "both" -> {
                                    reprogramarAlarmasFechaHora(context, reminder, reminderEntity)
                                    LocationReminderService.start(context)
                                }
                            }
                        }
                    }

                    Log.d(TAG, "✅ Recordatorios restaurados y alarmas reprogramadas")
                } else {
                    Log.e(TAG, "❌ Error al obtener recordatorios: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error restaurando recordatorios: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun reprogramarAlarmasFechaHora(
        context: Context,
        reminder: Reminder,
        reminderEntity: ReminderEntity
    ) {
        if (reminder.days.isNullOrEmpty() || reminder.time.isNullOrEmpty()) {
            return
        }

        Log.d(TAG, "⏰ Reprogramando alarmas para: ${reminder.title}")

        reminder.days.forEachIndexed { index, day ->
            val uniqueId = reminderEntity.id * 100 + index

            val singleDayReminder = reminderEntity.copy(
                id = uniqueId,
                days = day
            )

            scheduleReminder(context, singleDayReminder)
        }
    }
    // 🔹 Función para obtener usuario (actualizada)
    fun getCurrentUser(onSuccess: () -> Unit = {}) {
        accessToken?.let { token ->
            viewModelScope.launch {
                isLoading = true
                repository.getCurrentUser("Bearer $token").fold(
                    onSuccess = { currentUser ->
                        user = currentUser
                        // 🔹 Guardar usuario en SharedPreferences
                        sessionManager.saveUser(currentUser)
                        isLoading = false
                        errorMessage = null
                        onSuccess()
                    },
                    onFailure = {
                        user = null
                        isLoggedIn = false
                        accessToken = null
                        sessionManager.saveLoginState(false)
                        isLoading = false
                        errorMessage = it.message
                    }
                )
            }
        } ?: run {
            user = null
            isLoggedIn = false
            sessionManager.saveLoginState(false)
            errorMessage = "No hay token de acceso"
            isLoading = false
        }
    }

    // Dentro de AuthViewModel
    private lateinit var reminderViewModel: ReminderViewModel // O inyecta el repository

    fun logout(context: Context, onComplete: (() -> Unit)? = null) {
        val savedRefresh = sessionManager.getRefreshToken()

        viewModelScope.launch {
            // 1️⃣ Cancelar todas las alarmas y limpiar BD local
            try {
                cancelAllRemindersAndCleanup(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error limpiando recordatorios: ${e.message}")
            }

            // 2️⃣ Hacer logout en backend
            if (savedRefresh != null) {
                repository.logout(savedRefresh).fold(
                    onSuccess = {
                        clearLocalSession()
                        onComplete?.invoke()
                    },
                    onFailure = { exception ->
                        clearLocalSession()
                        onComplete?.invoke()
                    }
                )
            } else {
                clearLocalSession()
                onComplete?.invoke()
            }
        }
    }

    private suspend fun cancelAllRemindersAndCleanup(context: Context) {
        // Necesitas acceso al ReminderRepository aquí
        val database = AppDatabase.getDatabase(context)
        val repository = ReminderRepository(database.reminderDao())

        Log.d(TAG, "🧹 Limpiando recordatorios del usuario anterior...")

        // Obtener todos los recordatorios
        val allReminders = repository.getLocalReminders()

        // Cancelar alarmas
        allReminders.forEach { reminder ->
            if (reminder.reminder_type == "datetime" || reminder.reminder_type == "both") {
                val days = reminder.days?.split(",") ?: emptyList()
                days.forEachIndexed { index, _ ->
                    val uniqueId = reminder.id * 100 + index
                    cancelAlarm(context, uniqueId)
                }
            }
        }

        // Detener servicio de ubicación
        LocationReminderService.stop(context)

        // Limpiar base de datos
        repository.clearAllReminders()

        Log.d(TAG, "✅ Limpieza completada")
    }

    private fun cancelAlarm(context: Context, reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // 🔹 Helper para limpiar sesión local
    private fun clearLocalSession() {
        user = null
        accessToken = null
        isLoggedIn = false
        isLoading = false
        errorMessage = null
        sessionManager.clear()
    }

    // 🔹 Auto refresh actualizado
    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // 5 minutos
                val savedRefresh = sessionManager.getRefreshToken()

                if (savedRefresh != null && isLoggedIn) {
                    Log.d(TAG, "🔄 Iniciando auto-refresh del token...")

                    repository.refreshToken(savedRefresh).fold(
                        onSuccess = { response ->
                            Log.d(TAG, "✅ Token renovado exitosamente")
                            Log.d(TAG, "   Nuevo token: ${response.accessToken.take(20)}...")

                            accessToken = response.accessToken

                            // 🆕 CRÍTICO: Guardar tokens (esto notifica a ChatGrupoViewModel)
                            sessionManager.saveTokens(response.accessToken, response.refreshToken)

                            Log.d(TAG, "📢 Token guardado, listeners deberían ser notificados")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "❌ Error en auto-refresh: ${error.message}")
                            logout(context)
                        }
                    )
                } else {
                    Log.w(TAG, "⚠️ No hay refresh token o usuario no logueado")
                }
            }
        }
    }

    fun obtenerIp(): String {
        return try {
            val en = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in en) {
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            "Desconocida"
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Desconocida"
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    fun clearError() {
        errorMessage = null
    }

    class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun registerUser(nombre: String, apellido: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null // Limpiar errores anteriores

            repository.register(nombre, apellido, email, password).fold(
                onSuccess = {
                    isLoading = false
                    onResult(true)
                },
                onFailure = { exception ->
                    isLoading = false

                    // Manejar diferentes tipos de errores del backend
                    errorMessage = when {
                        exception.message?.contains("USER_ALREADY_EXISTS") == true -> {
                            "Ya existe una cuenta con este correo electrónico"
                        }
                        exception.message?.contains("INVALID_EMAIL") == true -> {
                            "El formato del correo electrónico no es válido"
                        }
                        exception.message?.contains("WEAK_PASSWORD") == true -> {
                            "La contraseña debe ser más fuerte"
                        }
                        exception.message?.contains("NETWORK_ERROR") == true -> {
                            "Error de conexión. Verifica tu internet"
                        }
                        exception.message?.contains("SERVER_ERROR") == true -> {
                            "Error del servidor. Inténtalo más tarde"
                        }
                        exception.message?.contains("400") == true -> {
                            "Datos inválidos. Verifica la información ingresada"
                        }
                        exception.message?.contains("500") == true -> {
                            "Error interno del servidor. Inténtalo más tarde"
                        }
                        else -> {
                            // Mensaje genérico amigable
                            "Error al crear la cuenta. Inténtalo nuevamente"
                        }
                    }

                    onResult(false)
                }
            )
        }
    }
}