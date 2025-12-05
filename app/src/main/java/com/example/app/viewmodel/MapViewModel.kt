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
import com.example.app.screen.rutas.components.getPreferenceDisplayName
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

class MapViewModel(
    private val rutasRepository: RutasRepository
) : ViewModel() {

    private val _route = mutableStateOf<DirectionsResponse?>(null)
    val route: State<DirectionsResponse?> = _route

    private var currentMode = "foot-walking"
    private var currentMLType: String? = null
    private var currentToken: String? = null

    private val _rutaIdActiva = mutableStateOf<Int?>(null)
    val rutaIdActiva: State<Int?> = _rutaIdActiva

    private val _mostrarOpcionesFinalizar = mutableStateOf(false)
    val mostrarOpcionesFinalizar: State<Boolean> = _mostrarOpcionesFinalizar

    private var rutaActualUbicacionId: Int? = null
    private var rutaActualDistancia: Double? = null
    private var rutaActualDuracion: Double? = null

    private val _puntosGPSReales = mutableListOf<PuntoGPS>()

    private val _mostrarAlertaDesobediencia = mutableStateOf(false)
    val mostrarAlertaDesobediencia: State<Boolean> = _mostrarAlertaDesobediencia

    private val _mensajeAlertaDesobediencia = mutableStateOf<String?>(null)
    val mensajeAlertaDesobediencia: State<String?> = _mensajeAlertaDesobediencia

    private val _alternativeRoutes = mutableStateOf<List<RouteAlternative>>(emptyList())
    val alternativeRoutes: State<List<RouteAlternative>> = _alternativeRoutes

    private val _showRouteSelector = mutableStateOf(false)
    val showRouteSelector: State<Boolean> = _showRouteSelector

    // 🆕 NUEVOS ESTADOS PARA SEGURIDAD
    private val _validacionSeguridad = mutableStateOf<ValidarRutasResponse?>(null)
    val validacionSeguridad: State<ValidarRutasResponse?> = _validacionSeguridad

    private val _mostrarAdvertenciaSeguridad = mutableStateOf(false)
    val mostrarAdvertenciaSeguridad: State<Boolean> = _mostrarAdvertenciaSeguridad

    private val _rutaSeleccionadaPendiente = mutableStateOf<RouteAlternative?>(null)

    // 🔥 FUNCIÓN PRINCIPAL MODIFICADA
    fun fetchAllRouteAlternatives(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        token: String,
        ubicacionId: Int,
        transporteTexto: String
    ) {
        currentToken = token
        rutaActualUbicacionId = ubicacionId

        viewModelScope.launch {
            try {
                Log.d("MapViewModel", "🔄 Calculando 3 rutas alternativas...")

                // 1. Calcular las 3 rutas con ORS (como antes)
                val routes = listOf("fastest", "shortest", "recommended").map { preference ->
                    async {
                        try {
                            val request = DirectionsRequest(
                                coordinates = listOf(
                                    listOf(start.second, start.first),
                                    listOf(end.second, end.first)
                                ),
                                preference = preference
                            )

                            val response = RetrofitInstance.api.getRoute(currentMode, request)
                            val route = response.routes.firstOrNull()

                            RouteAlternative(
                                type = preference,
                                displayName = getPreferenceDisplayName(preference),
                                response = response.copy(profile = currentMode),
                                distance = route?.summary?.distance ?: 0.0,
                                duration = route?.summary?.duration ?: 0.0,
                                isRecommended = false
                            )
                        } catch (e: Exception) {
                            Log.e("MapViewModel", "Error calculando ruta $preference", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                if (routes.isEmpty()) {
                    Log.e("MapViewModel", "❌ No se pudieron calcular rutas")
                    return@launch
                }

                // 2. 🆕 VALIDAR RUTAS CONTRA ZONAS PELIGROSAS
                try {
                    val rutasParaValidar = routes.map { route ->
                        RutaParaValidar(
                            tipo = route.type,
                            geometry = route.response.routes.first().geometry,
                            distance = route.distance,
                            duration = route.duration
                        )
                    }


                    val validacion = RetrofitClient.rutasApiService.validarRutas(
                        token = "Bearer $token",
                        request = ValidarRutasRequest(
                            rutas = rutasParaValidar,
                            ubicacionId = ubicacionId
                        )
                    )

                    _validacionSeguridad.value = validacion

                    Log.d("MapViewModel", "🔐 Validación de seguridad completada:")
                    Log.d("MapViewModel", "  - Todas seguras: ${validacion.todasSeguras}")
                    Log.d("MapViewModel", "  - ML recomienda: ${validacion.tipoMlRecomendado}")

                    // 3. Combinar rutas con información de seguridad
                    val routesConSeguridad = routes.mapIndexed { index, route ->
                        val validacionRuta = validacion.rutasValidadas[index]
                        route.copy(
                            isRecommended = route.type == validacion.tipoMlRecomendado,
                            esSegura = validacionRuta.esSegura,
                            nivelRiesgo = validacionRuta.nivelRiesgo,
                            zonasDetectadas = validacionRuta.zonasDetectadas,
                            mensajeSeguridad = validacionRuta.mensaje
                        )
                    }

                    _alternativeRoutes.value = routesConSeguridad
                    currentMLType = validacion.tipoMlRecomendado

                    Log.d("MapViewModel", "✅ ${routes.size} rutas con información de seguridad")

                } catch (e: Exception) {
                    Log.e("MapViewModel", "⚠️ Error validando seguridad, continuando sin validación", e)
                    // Si falla la validación, mostrar rutas sin información de seguridad
                    _alternativeRoutes.value = routes
                }

                _showRouteSelector.value = true

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error general calculando alternativas", e)
            }
        }
    }

    // 🆕 Seleccionar ruta con validación de seguridad
    fun selectRouteAlternative(alternative: RouteAlternative, token: String, ubicacionId: Int, transporteTexto: String) {
        viewModelScope.launch {
            // Si la ruta NO es segura Y tiene nivel de riesgo alto (>=3), mostrar advertencia
            val esRutaPeligrosa = alternative.esSegura == false &&
                    alternative.nivelRiesgo != null &&
                    alternative.nivelRiesgo >= 3

            if (esRutaPeligrosa) {
                _rutaSeleccionadaPendiente.value = alternative
                _mostrarAdvertenciaSeguridad.value = true
                Log.d("MapViewModel", "⚠️ Ruta insegura detectada, mostrando advertencia")
                return@launch
            }

            // Si es segura o el usuario ya aceptó el riesgo, continuar
            confirmarSeleccionRuta(alternative, token, ubicacionId, transporteTexto)
        }
    }

    // 🆕 Confirmar selección de ruta (después de aceptar riesgo)
    private suspend fun confirmarSeleccionRuta(
        alternative: RouteAlternative,
        token: String,
        ubicacionId: Int,
        transporteTexto: String
    ) {
        _route.value = alternative.response
        currentMLType = alternative.type
        rutaActualDistancia = alternative.distance
        rutaActualDuracion = alternative.duration
        _showRouteSelector.value = false
        _mostrarAdvertenciaSeguridad.value = false

        // Guardar en backend
        guardarRutaEnBackend(
            alternative.response,
            token,
            ubicacionId,
            transporteTexto,
            alternative.type
        )

        if (alternative.esSegura == false) {
            Log.d("MapViewModel", "⚠️ Usuario aceptó ruta con riesgo nivel ${alternative.nivelRiesgo}")
        }
    }

    // 🆕 Usuario acepta el riesgo de ruta insegura
    fun aceptarRiesgoRutaInsegura(token: String, ubicacionId: Int, transporteTexto: String) {
        viewModelScope.launch {
            _rutaSeleccionadaPendiente.value?.let { ruta ->
                confirmarSeleccionRuta(ruta, token, ubicacionId, transporteTexto)
                _rutaSeleccionadaPendiente.value = null
            }
        }
    }

    // 🆕 Usuario rechaza ruta insegura
    fun rechazarRutaInsegura() {
        _mostrarAdvertenciaSeguridad.value = false
        _rutaSeleccionadaPendiente.value = null
        Log.d("MapViewModel", "❌ Usuario rechazó ruta insegura")
    }

    fun hideRouteSelector() {
        _showRouteSelector.value = false
    }

    fun agregarPuntoGPSReal(lat: Double, lng: Double) {
        val punto = PuntoGPS(
            lat = lat,
            lng = lng,
            timestamp = System.currentTimeMillis()
        )
        _puntosGPSReales.add(punto)
    }

    fun setMode(mode: String) {
        currentMode = mode
    }

    fun setToken(token: String) {
        currentToken = token
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

            val result = rutasRepository.guardarRuta(token, rutaJson)
            result.onSuccess { rutaGuardada ->
                Log.d("MapViewModel", "✅ Ruta guardada correctamente")
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
        _validacionSeguridad.value = null
        _mostrarAdvertenciaSeguridad.value = false
        _rutaSeleccionadaPendiente.value = null
    }


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

        // Detectar incorporación a la ruta
        val tolerancia = 80.0
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

                //vENVIAR puntos GPS reales al backend
                val result = rutasRepository.finalizarRuta(
                    rutaId = rutaId,
                    fechaFin = fechaFin,
                    puntosGPS = _puntosGPSReales.toList(),
                    siguioRutaRecomendada = siguioRuta,
                    porcentajeSimilitud = porcentajeSimilitud
                )

                result.onSuccess { response ->
                    Log.d("MapViewModel", "✅ Ruta finalizada: ${response.success}")

                    // VERIFICAR si hay alerta de desobediencia
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

    fun ocultarOpcionesFinalizar() {
        _mostrarOpcionesFinalizar.value = false
        _rutaIdActiva.value = null
    }
}