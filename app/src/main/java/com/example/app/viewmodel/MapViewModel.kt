package com.example.app.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.models.*
import com.example.app.network.RetrofitClient
import com.example.app.network.RetrofitInstance
import com.example.app.repository.RutasRepository
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

class MapViewModel(
    private val rutasRepository: RutasRepository
) : ViewModel() {

    private val _route = mutableStateOf<DirectionsResponse?>(null)
    val route: State<DirectionsResponse?> = _route

    private var currentMode = "foot-walking" // foot-walking, cycling-regular, etc.
    private var currentMLType: String? = null
    private var currentToken: String? = null

    private val _rutaIdActiva = mutableStateOf<Int?>(null)
    val rutaIdActiva: State<Int?> = _rutaIdActiva

    private val _mostrarOpcionesFinalizar = mutableStateOf(false)
    val mostrarOpcionesFinalizar: State<Boolean> = _mostrarOpcionesFinalizar

    // 🔥 NUEVAS VARIABLES para guardar datos de la ruta actual
    private var rutaActualUbicacionId: Int? = null
    private var rutaActualDistancia: Double? = null
    private var rutaActualDuracion: Double? = null

    private val _puntosGPSReales = mutableListOf<PuntoGPS>()

    private val _mostrarAlertaDesobediencia = mutableStateOf(false)
    val mostrarAlertaDesobediencia: State<Boolean> = _mostrarAlertaDesobediencia

    private val _mensajeAlertaDesobediencia = mutableStateOf<String?>(null)
    val mensajeAlertaDesobediencia: State<String?> = _mensajeAlertaDesobediencia

    fun agregarPuntoGPSReal(lat: Double, lng: Double) {
        val punto = PuntoGPS(
            lat = lat,
            lng = lng,
            timestamp = System.currentTimeMillis()
        )
        _puntosGPSReales.add(punto)
        Log.d("MapViewModel", "📍 Punto GPS agregado: $punto")
    }

    fun setMode(mode: String) {
        currentMode = mode
    }

    fun setToken(token: String) {
        currentToken = token
    }

    // Función principal con ML y autenticación
    fun fetchRouteWithML(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        token: String,
        ubicacionId: Int,
        transporteTexto: String? = null
    ) {
        currentToken = token
        rutaActualUbicacionId = ubicacionId // 🔥 GUARDAR para el feedback

        viewModelScope.launch {
            try {
                // PASO 1: Obtener recomendación ML
                Log.d("MapViewModel", "Consultando ML con token...")

                val recomendacion = RetrofitClient.mlService.getRecomendacionTipoRuta(
                    "Bearer $token",
                    TipoRutaRequest(ubicacionId)
                )

                currentMLType = recomendacion.tipo_ruta
                Log.d(
                    "MapViewModel",
                    "ML recomienda: ${recomendacion.tipo_ruta} para usuario ${recomendacion.usuario_id}"
                )

                // PASO 2: Configurar OpenRouteService según ML
                val (preference, avoidOptions) = recomendacion.tipo_ruta.toORSConfig()

                val request = DirectionsRequest(
                    coordinates = listOf(
                        listOf(start.second, start.first),
                        listOf(end.second, end.first)
                    ),
                    preference = preference,
                    options = avoidOptions
                )

                Log.d("MapViewModel", "Enviando request ORS: $request")
                Log.d(
                    "MapViewModel",
                    "🔥 USANDO TIPO ML: ${recomendacion.tipo_ruta} -> ORS preference: $preference"
                )

                // PASO 3: Obtener ruta de OpenRouteService
                val response = RetrofitInstance.api.getRoute(currentMode, request)
                val responseWithProfile = response.copy(profile = currentMode)

                // 🔥 GUARDAR datos de la ruta para el feedback
                response.routes.firstOrNull()?.summary?.let { summary ->
                    rutaActualDistancia = summary.distance.toDouble()
                    rutaActualDuracion = summary.duration
                }

                Log.d(
                    "MapViewModel",
                    "Ruta obtenida: distancia=${response.routes.firstOrNull()?.summary?.distance}m"
                )

                _route.value = responseWithProfile

                // PASO 4: Guardar ruta CON el tipo usado
                if (transporteTexto != null) {
                    guardarRutaEnBackend(
                        responseWithProfile,
                        token,
                        ubicacionId,
                        transporteTexto,
                        recomendacion.tipo_ruta
                    )
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching route with ML", e)
                _route.value = null
            }
        }
    }

    private suspend fun guardarRutaEnBackend(
        response: DirectionsResponse,
        token: String,
        ubicacionId: Int,
        transporteTexto: String,
        tipoRutaUsado: String
    ) {
        try {
            val rutaJson = response.toRutaUsuarioJson(
                ubicacionId = ubicacionId,
                transporteTexto = transporteTexto,
                tipoRutaUsado = tipoRutaUsado
            )

            Log.d("MapViewModel", "📅 Fecha de inicio generada en Android: ${rutaJson.fecha_inicio}")

            Log.d("MapViewModel", "Guardando ruta con tipo ML: $tipoRutaUsado")

            val result = rutasRepository.guardarRuta(token, rutaJson)
            result.onSuccess { rutaGuardada ->
                Log.d("MapViewModel", "✅ Ruta guardada correctamente con tipo: $tipoRutaUsado")
                Log.d("MapViewModel", "Ruta completa: $rutaGuardada")
                _rutaIdActiva.value = rutaGuardada.id
                _mostrarOpcionesFinalizar.value = true
            }.onFailure { error ->
                Log.e("MapViewModel", "❌ Error al guardar ruta: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Excepción al guardar ruta", e)
        }
    }

    fun clearRoute() {
        _route.value = null
        currentMLType = null
        rutaActualUbicacionId = null
        rutaActualDistancia = null
        rutaActualDuracion = null
        _puntosGPSReales.clear()
    }

    // En MapViewModel, reemplaza tu función calcularSimilitudRuta() con esta versión mejorada
    // Versión mejorada de calcularSimilitudRuta con logging más robusto
    private fun calcularSimilitudRuta(): Pair<Boolean, Double> {
        Log.d("MapViewModel", "🚀 INICIANDO calcularSimilitudRuta()...")

        val rutaRecomendada = _route.value?.routes?.firstOrNull()?.geometry
        val puntosReales = _puntosGPSReales.toList()

        Log.d("MapViewModel", "📊 Datos iniciales:")
        Log.d("MapViewModel", "- Ruta recomendada existe: ${rutaRecomendada != null}")
        Log.d("MapViewModel", "- Puntos GPS reales: ${puntosReales.size}")

        if (rutaRecomendada == null || puntosReales.isEmpty()) {
            Log.w("MapViewModel", "❌ No hay datos suficientes para calcular similitud")
            return Pair(false, 0.0)
        }

        // Decodificar polyline de la ruta recomendada
        val puntosRecomendados = try {
            Log.d("MapViewModel", "🔄 Decodificando polyline...")
            val puntos = rutaRecomendada.decodePolyline()
            Log.d("MapViewModel", "✅ Polyline decodificado: ${puntos.size} puntos")
            puntos
        } catch (e: Exception) {
            Log.e("MapViewModel", "❌ Error decodificando polyline: ${e.message}", e)
            return Pair(false, 0.0)
        }

        if (puntosRecomendados.isEmpty()) {
            Log.w("MapViewModel", "❌ No se pudo decodificar polyline recomendado")
            return Pair(false, 0.0)
        }

        // ALGORITMO MEJORADO: Detectar incorporación a la ruta
        val tolerancia = 80.0 // 80 metros (más realista para navegación urbana)
        var puntosEnRuta = 0
        var mejorSecuenciaConsecutiva = 0
        var secuenciaActual = 0
        var ultimosNPuntosEnRuta = 0

        // Analizar cada punto GPS real
        val distanciasDetalladas = mutableListOf<Double>()

        Log.d("MapViewModel", "🔄 Analizando cada punto GPS...")

        for (i in puntosReales.indices) {
            val puntoReal = puntosReales[i]
            val puntoGeoReal = GeoPoint(puntoReal.lat, puntoReal.lng)

            // Buscar el punto recomendado más cercano
            val distanciaMinima = try {
                puntosRecomendados.minOfOrNull { puntoRec ->
                    puntoGeoReal.distanceToAsDouble(puntoRec)
                } ?: Double.MAX_VALUE
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error calculando distancia en punto $i: ${e.message}")
                Double.MAX_VALUE
            }

            distanciasDetalladas.add(distanciaMinima)

            if (distanciaMinima <= tolerancia) {
                puntosEnRuta++
                secuenciaActual++
                mejorSecuenciaConsecutiva = maxOf(mejorSecuenciaConsecutiva, secuenciaActual)

                // Contar últimos 5 puntos (para detectar si termina en la ruta)
                if (i >= puntosReales.size - 5) {
                    ultimosNPuntosEnRuta++
                }
            } else {
                secuenciaActual = 0
            }

            // Log cada 10 puntos para no saturar
            if (i % 10 == 0 || i == puntosReales.size - 1) {
                Log.d("MapViewModel", "📍 Punto $i: distancia=${distanciaMinima.roundToInt()}m, enRuta=${distanciaMinima <= tolerancia}")
            }
        }

        Log.d("MapViewModel", "📊 Análisis de puntos completado:")
        Log.d("MapViewModel", "- Puntos en ruta: $puntosEnRuta/${puntosReales.size}")
        Log.d("MapViewModel", "- Mejor secuencia consecutiva: $mejorSecuenciaConsecutiva")

        // Analizar inicio y fin de la ruta
        val inicioEnRuta = if (puntosReales.isNotEmpty()) {
            val puntoInicial = GeoPoint(puntosReales[0].lat, puntosReales[0].lng)
            val distanciaInicio = try {
                puntosRecomendados.minOfOrNull { puntoRec ->
                    puntoInicial.distanceToAsDouble(puntoRec)
                } ?: Double.MAX_VALUE
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error calculando distancia inicio: ${e.message}")
                Double.MAX_VALUE
            }
            Log.d("MapViewModel", "📍 Distancia inicio: ${distanciaInicio.roundToInt()}m")
            distanciaInicio <= tolerancia * 1.5
        } else false

        val finEnRuta = if (puntosReales.isNotEmpty()) {
            val puntoFinal = GeoPoint(puntosReales.last().lat, puntosReales.last().lng)
            val distanciaFin = try {
                puntosRecomendados.minOfOrNull { puntoRec ->
                    puntoFinal.distanceToAsDouble(puntoRec)
                } ?: Double.MAX_VALUE
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error calculando distancia fin: ${e.message}")
                Double.MAX_VALUE
            }
            Log.d("MapViewModel", "📍 Distancia fin: ${distanciaFin.roundToInt()}m")
            distanciaFin <= tolerancia * 1.5
        } else false

        // Calcular métricas
        val similitudTotal = (puntosEnRuta.toDouble() / puntosReales.size) * 100
        val porcentajeSecuenciaConsecutiva = (mejorSecuenciaConsecutiva.toDouble() / puntosReales.size) * 100
        val porcentajeUltimoTramo = (ultimosNPuntosEnRuta.toDouble() / minOf(5, puntosReales.size)) * 100

        // LOGGING DETALLADO FORZADO (con println para asegurar que aparezca)
        println("🔥 MAPVIEWMODEL - ANÁLISIS DETALLADO:")
        println("🔥 Total puntos GPS: ${puntosReales.size}")
        println("🔥 Puntos recomendados: ${puntosRecomendados.size}")
        println("🔥 Puntos en ruta: $puntosEnRuta/${puntosReales.size} (${similitudTotal.roundToInt()}%)")
        println("🔥 Mejor secuencia consecutiva: $mejorSecuenciaConsecutiva (${porcentajeSecuenciaConsecutiva.roundToInt()}%)")
        println("🔥 Últimos puntos en ruta: $ultimosNPuntosEnRuta/5 (${porcentajeUltimoTramo.roundToInt()}%)")
        println("🔥 Inicio en ruta: $inicioEnRuta")
        println("🔥 Fin en ruta: $finEnRuta")
        println("🔥 Tolerancia usada: ${tolerancia}m")

        Log.d("MapViewModel", "=== ANÁLISIS DE RUTA DETALLADO ===")
        Log.d("MapViewModel", "Puntos GPS reales: ${puntosReales.size}")
        Log.d("MapViewModel", "Puntos recomendados: ${puntosRecomendados.size}")
        Log.d("MapViewModel", "Puntos en ruta: $puntosEnRuta/${puntosReales.size} (${similitudTotal.roundToInt()}%)")
        Log.d("MapViewModel", "Mejor secuencia consecutiva: $mejorSecuenciaConsecutiva (${porcentajeSecuenciaConsecutiva.roundToInt()}%)")
        Log.d("MapViewModel", "Últimos puntos en ruta: $ultimosNPuntosEnRuta/5 (${porcentajeUltimoTramo.roundToInt()}%)")
        Log.d("MapViewModel", "Inicio en ruta: $inicioEnRuta")
        Log.d("MapViewModel", "Fin en ruta: $finEnRuta")
        Log.d("MapViewModel", "Tolerancia usada: ${tolerancia}m")

        // VALIDACIÓN MANUAL con println
        println("🔥 VALIDACIÓN MANUAL:")
        println("🔥 Total puntos GPS: ${puntosReales.size}")
        println("🔥 Puntos dentro de 80m de la ruta: $puntosEnRuta")
        println("🔥 Cálculo manual: ${puntosEnRuta}/${puntosReales.size} = ${(puntosEnRuta.toDouble()/puntosReales.size)*100}%")

        Log.d("MapViewModel", "VALIDACIÓN MANUAL:")
        Log.d("MapViewModel", "Total puntos GPS: ${puntosReales.size}")
        Log.d("MapViewModel", "Puntos dentro de 80m de la ruta: $puntosEnRuta")
        Log.d("MapViewModel", "Cálculo manual: ${puntosEnRuta}/${puntosReales.size} = ${(puntosEnRuta.toDouble()/puntosReales.size)*100}%")

        // CRITERIO MEJORADO PARA DETECTAR SI SIGUIÓ LA RUTA:
        val siguioRuta = when {
            // Caso 1: Siguió la ruta desde el inicio (ruta perfecta)
            similitudTotal >= 70.0 && inicioEnRuta -> {
                Log.d("MapViewModel", "✅ Caso 1: Ruta seguida desde el inicio")
                println("🔥 Caso 1: Ruta seguida desde el inicio")
                true
            }

            // Caso 2: Se incorporó tarde pero siguió bien el resto (tu caso)
            similitudTotal >= 50.0 && porcentajeSecuenciaConsecutiva >= 40.0 && finEnRuta -> {
                Log.d("MapViewModel", "✅ Caso 2: Incorporación tardía pero siguió la ruta")
                println("🔥 Caso 2: Incorporación tardía pero siguió la ruta")
                true
            }

            // Caso 3: Terminó bien en la ruta (últimos puntos en ruta)
            similitudTotal >= 40.0 && porcentajeUltimoTramo >= 60.0 -> {
                Log.d("MapViewModel", "✅ Caso 3: Terminó siguiendo la ruta correctamente")
                println("🔥 Caso 3: Terminó siguiendo la ruta correctamente")
                true
            }

            // Caso 4: Secuencia larga consecutiva (siguió un tramo largo)
            porcentajeSecuenciaConsecutiva >= 60.0 -> {
                Log.d("MapViewModel", "✅ Caso 4: Siguió un tramo largo de la ruta")
                println("🔥 Caso 4: Siguió un tramo largo de la ruta")
                true
            }

            else -> {
                Log.d("MapViewModel", "❌ No cumple criterios para 'siguió la ruta'")
                println("🔥 No cumple criterios para 'siguió la ruta'")
                false
            }
        }

        // Log de las primeras distancias para debug
        if (distanciasDetalladas.size >= 5) {
            val primeras5 = distanciasDetalladas.take(5).map { "${it.roundToInt()}m" }
            Log.d("MapViewModel", "Primeras 5 distancias: $primeras5")
            println("🔥 Primeras 5 distancias: $primeras5")
        }

        println("🔥 RESULTADO FINAL: siguioRuta=$siguioRuta, similitud=${similitudTotal.roundToInt()}%")
        Log.d("MapViewModel", "🏁 RESULTADO FINAL: siguioRuta=$siguioRuta, similitud=${similitudTotal.roundToInt()}%")

        return Pair(siguioRuta, similitudTotal)
    }

    // También actualiza la función finalizarRutaBackend para usar el Pair
    fun finalizarRutaBackend(rutaId: Int) {
        viewModelScope.launch {
            try {
                val fechaFin = System.currentTimeMillis().toLocalISOString()
                val (siguioRuta, porcentajeSimilitud) = calcularSimilitudRuta()

                Log.d("MapViewModel", "📊 RESULTADO FINAL CALCULADO:")
                Log.d("MapViewModel", "- siguió ruta: $siguioRuta")
                Log.d("MapViewModel", "- porcentaje similitud: ${porcentajeSimilitud.roundToInt()}%")

                Log.d("MapViewModel", "📅 Fecha de fin generada en Android (finalizar): $fechaFin")
                Log.d("MapViewModel", "📤 Enviando rutaId: $rutaId con ${_puntosGPSReales.size} puntos GPS")
                Log.d("MapViewModel", "Usuario siguió ruta: $siguioRuta (${porcentajeSimilitud.roundToInt()}% similitud)")

                Log.d("MapViewModel", "ANTES DE ENVIAR AL BACKEND:")
                Log.d("MapViewModel", "- siguioRuta: $siguioRuta")
                Log.d("MapViewModel", "- similitud: $porcentajeSimilitud")

                // 🔥 ENVIAR puntos GPS reales al backend
                val result = rutasRepository.finalizarRuta(
                    rutaId = rutaId,
                    fechaFin = fechaFin,
                    puntosGPS = _puntosGPSReales.toList(),
                    siguioRutaRecomendada = siguioRuta,
                    porcentajeSimilitud = porcentajeSimilitud
                )

                result.onSuccess { response ->
                    Log.d("MapViewModel", "✅ Ruta finalizada: ${response.success}")

                    // 🔥 VERIFICAR si hay alerta de desobediencia
                    if (response.alerta_desobediencia && response.mensaje_alerta != null) {
                        Log.d("MapViewModel", "🚨 ALERTA DESOBEDIENCIA: ${response.mensaje_alerta}")
                        _mostrarAlertaDesobediencia.value = true
                        _mensajeAlertaDesobediencia.value = response.mensaje_alerta
                    }

                    _mostrarOpcionesFinalizar.value = false
                    _route.value = null
                    _puntosGPSReales.clear()

                }.onFailure { error ->
                    Log.e("MapViewModel", "❌ Error finalizando ruta: ${error.message}")
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error finalizando ruta", e)
            }
        }
    }

    // 🔥 FUNCIÓN CANCELAR - ahora con fecha_fin
    fun cancelarRutaBackend(rutaId: Int) {
        viewModelScope.launch {
            try {
                val fechaFin = System.currentTimeMillis().toLocalISOString()
                Log.d("MapViewModel", "📅 Fecha de fin generada en Android (cancelar): $fechaFin")

                rutasRepository.cancelarRuta(rutaId, fechaFin)  // Sin puntos GPS
                Log.d("MapViewModel", "✅ Ruta cancelada en backend")

                _mostrarOpcionesFinalizar.value = false
                _route.value = null
                _puntosGPSReales.clear()  // Limpiar puntos GPS

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error cancelando ruta", e)
            }
        }
    }

    // 🔥 FUNCIÓN para cerrar alerta de desobediencia
    fun cerrarAlertaDesobediencia() {
        _mostrarAlertaDesobediencia.value = false
        _mensajeAlertaDesobediencia.value = null
    }

    // 🔥 NUEVA FUNCIÓN para enviar feedback al UCB
    private suspend fun enviarFeedbackUCB(completada: Boolean) {
        try {
            val token = currentToken
            val tipoUsado = currentMLType
            val ubicacionId = rutaActualUbicacionId
            val distancia = rutaActualDistancia
            val duracion = rutaActualDuracion

            if (token == null || tipoUsado == null || ubicacionId == null) {
                Log.w("MapViewModel", "❌ Faltan datos para enviar feedback UCB")
                return
            }

            val feedbackRequest = FeedbackRequest(
                tipo_usado = tipoUsado,
                completada = completada,
                ubicacion_id = ubicacionId,
                distancia = distancia,
                duracion = duracion
            )

            Log.d("MapViewModel", "📤 Enviando feedback UCB: $feedbackRequest")

            val response = RetrofitClient.mlService.enviarFeedback(
                "Bearer $token",
                feedbackRequest
            )

            Log.d("MapViewModel", "✅ Feedback UCB enviado: ${response.mensaje}")

        } catch (e: Exception) {
            Log.e("MapViewModel", "❌ Error enviando feedback UCB: ${e.message}", e)
        }
    }

    fun ocultarOpcionesFinalizar() {
        _mostrarOpcionesFinalizar.value = false
        _rutaIdActiva.value = null
    }
}