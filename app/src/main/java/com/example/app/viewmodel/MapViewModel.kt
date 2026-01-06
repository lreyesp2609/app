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


    private val _isRegeneratingRoutes = mutableStateOf(false)
    val isRegeneratingRoutes: State<Boolean> = _isRegeneratingRoutes

    private val _rutasGeneradasEvitandoZonas = mutableStateOf(false)
    val rutasGeneradasEvitandoZonas: State<Boolean> = _rutasGeneradasEvitandoZonas

    private val _zonasPeligrosas = mutableStateOf<List<ZonaPeligrosaResponse>>(emptyList())
    val zonasPeligrosas: State<List<ZonaPeligrosaResponse>> = _zonasPeligrosas

    private val _mostrarZonasPeligrosas = mutableStateOf(true)
    val mostrarZonasPeligrosas: State<Boolean> = _mostrarZonasPeligrosas

    private val _cargandoZonas = mutableStateOf(false)
    val cargandoZonas: State<Boolean> = _cargandoZonas

    // 🆕 FUNCIÓN PARA CARGAR ZONAS PELIGROSAS
    fun cargarZonasPeligrosas(token: String) {
        viewModelScope.launch {
            try {
                _cargandoZonas.value = true
                Log.d("MapViewModel", "🔄 Cargando zonas peligrosas del usuario...")

                val zonas = RetrofitClient.rutasApiService.obtenerMisZonasPeligrosas(
                    token = "Bearer $token",
                    activasSolo = true
                )

                _zonasPeligrosas.value = zonas
                Log.d("MapViewModel", "✅ ${zonas.size} zonas peligrosas cargadas")

                // 🔍 Debug: Mostrar las primeras coordenadas
                zonas.firstOrNull()?.let { zona ->
                    Log.d("MapViewModel", "📍 Primera zona: ${zona.nombre}")
                    Log.d("MapViewModel", "   Centro: lat=${zona.poligono.firstOrNull()?.lat}, lon=${zona.poligono.firstOrNull()?.lon}")
                    Log.d("MapViewModel", "   Radio: ${zona.radioMetros}m")
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "❌ Error cargando zonas peligrosas: ${e.message}", e)
                _zonasPeligrosas.value = emptyList()
            } finally {
                _cargandoZonas.value = false
            }
        }
    }

    // 🆕 TOGGLE PARA MOSTRAR/OCULTAR ZONAS
    fun toggleMostrarZonas() {
        _mostrarZonasPeligrosas.value = !_mostrarZonasPeligrosas.value
        Log.d("MapViewModel", "👁️ Zonas peligrosas: ${if (_mostrarZonasPeligrosas.value) "VISIBLES" else "OCULTAS"}")
    }

    // 🆕 FUNCIÓN PARA OBTENER COLOR SEGÚN NIVEL DE PELIGRO
    fun getColorForDangerLevel(nivel: Int, isDarkTheme: Boolean): Int {
        return when (nivel) {
            1, 2 -> if (isDarkTheme) {
                android.graphics.Color.argb(100, 251, 191, 36) // Warning con alpha
            } else {
                android.graphics.Color.argb(100, 245, 158, 11)
            }
            3, 4 -> if (isDarkTheme) {
                android.graphics.Color.argb(120, 249, 115, 22) // WarningDark con alpha
            } else {
                android.graphics.Color.argb(120, 234, 88, 12)
            }
            5 -> if (isDarkTheme) {
                android.graphics.Color.argb(140, 239, 68, 68) // Danger con alpha
            } else {
                android.graphics.Color.argb(140, 220, 38, 38)
            }
            else -> android.graphics.Color.argb(100, 156, 163, 175) // Gray por defecto
        }
    }

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
                            Log.d("MapViewModel", "🚗 Modo actual: $currentMode")
                            Log.d("MapViewModel", "📤 Enviando request a ORS con avoid_polygons")

                            RouteAlternative(
                                type = preference,
                                displayName = getPreferenceDisplayName(preference),
                                response = response.copy(profile = currentMode),
                                distance = (route?.summary?.distance ?: 0.0) * 1000,
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

                    // 🔥 AGREGAR ESTOS LOGS ANTES DE mapIndexed
                    Log.d("MapViewModel", "🔍 ════════════════════════════════════════")
                    Log.d("MapViewModel", "🔍 VALIDACIÓN RECIBIDA DEL BACKEND")
                    Log.d("MapViewModel", "🔍 ════════════════════════════════════════")
                    Log.d("MapViewModel", "📊 Total zonas usuario: ${validacion.totalZonasUsuario}")
                    Log.d("MapViewModel", "📊 Todas seguras: ${validacion.todasSeguras}")
                    Log.d("MapViewModel", "📊 Mejor ruta segura: ${validacion.mejorRutaSegura}")

                    validacion.rutasValidadas.forEachIndexed { index, ruta ->
                        Log.d("MapViewModel", "")
                        Log.d("MapViewModel", "🚗 Ruta ${index + 1}: ${ruta.tipo}")
                        Log.d("MapViewModel", "   esSegura: ${ruta.esSegura}")
                        Log.d("MapViewModel", "   nivelRiesgo: ${ruta.nivelRiesgo}")
                        Log.d("MapViewModel", "   zonasDetectadas: ${ruta.zonasDetectadas.size}")

                        ruta.zonasDetectadas.forEach { zona ->
                            Log.d("MapViewModel", "     • ${zona.nombre} (nivel ${zona.nivelPeligro})")
                        }
                    }
                    Log.d("MapViewModel", "🔍 ════════════════════════════════════════")

                    // 3. Combinar rutas con información de seguridad
                    val routesConSeguridad = routes.mapIndexed { index, route ->
                        val validacionRuta = validacion.rutasValidadas[index]

                        val routeConSeguridad = route.copy(
                            isRecommended = route.type == validacion.tipoMlRecomendado,
                            esSegura = validacionRuta.esSegura,
                            nivelRiesgo = validacionRuta.nivelRiesgo,
                            zonasDetectadas = validacionRuta.zonasDetectadas,
                            mensajeSeguridad = validacionRuta.mensaje
                        )

                        // 🔥 LOG DE CADA RUTA
                        routeConSeguridad.logInfo("MapViewModel")

                        routeConSeguridad
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

    fun regenerarRutasEvitandoZonasPeligrosas(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        token: String,
        ubicacionId: Int,
        transporteTexto: String
    ) {
        viewModelScope.launch {
            try {
                _isRegeneratingRoutes.value = true
                Log.d("MapViewModel", "🔄 Regenerando rutas evitando zonas peligrosas...")

                // 1. Obtener zonas peligrosas del usuario
                val zonasUsuario = try {
                    RetrofitClient.rutasApiService.obtenerMisZonasPeligrosas(
                        token = "Bearer $token",
                        activasSolo = true
                    )
                } catch (e: Exception) {
                    Log.e("MapViewModel", "Error obteniendo zonas: ${e.message}")
                    emptyList()
                }

                if (zonasUsuario.isEmpty()) {
                    Log.w("MapViewModel", "⚠️ Usuario no tiene zonas peligrosas marcadas")
                    _isRegeneratingRoutes.value = false
                    return@launch
                }

                Log.d("MapViewModel", "📍 Zonas peligrosas a evitar: ${zonasUsuario.size}")

                // 2. ✅ CONVERTIR TODAS LAS ZONAS A UN SOLO OBJETO GeoJSON
                val todosLosPoligonos = zonasUsuario.mapNotNull { zona ->
                    try {
                        val primerPunto = zona.poligono.firstOrNull()
                        if (primerPunto == null) {
                            Log.w("MapViewModel", "⚠️ Zona sin puntos en polígono: ${zona.nombre}")
                            return@mapNotNull null
                        }

                        val lat = primerPunto.lat
                        val lon = primerPunto.lon
                        val radio = zona.radioMetros ?: 200

                        Log.d("MapViewModel", "📍 Procesando zona '${zona.nombre}': lat=$lat, lon=$lon, radio=${radio}m")

                        convertirCirculoAPoligono(lat, lon, radio)
                    } catch (e: Exception) {
                        Log.e("MapViewModel", "Error procesando zona ${zona.nombre}: ${e.message}")
                        null
                    }
                }

                if (todosLosPoligonos.isEmpty()) {
                    Log.w("MapViewModel", "⚠️ No se pudieron procesar las zonas peligrosas")
                    _isRegeneratingRoutes.value = false
                    return@launch
                }

                // 3. ✅ CREAR EL OBJETO GeoJSON CORRECTO
                val avoidPolygons = if (todosLosPoligonos.size == 1) {
                    // Si es un solo polígono, usar tipo "Polygon"
                    mapOf(
                        "type" to "Polygon",
                        "coordinates" to todosLosPoligonos  // Ya es una lista de anillos
                    )
                } else {
                    // Si son múltiples polígonos, usar tipo "MultiPolygon"
                    mapOf(
                        "type" to "MultiPolygon",
                        "coordinates" to todosLosPoligonos.map { listOf(it) }  // Cada polígono envuelto en otra lista
                    )
                }

                Log.d("MapViewModel", "✅ GeoJSON generado correctamente")
                Log.d("MapViewModel", "📦 JSON enviado: ${com.google.gson.Gson().toJson(avoidPolygons)}")

                // 4. Calcular rutas con ORS evitando polígonos
                val routes = listOf("fastest", "shortest", "recommended").map { preference ->
                    async {
                        try {
                            val request = DirectionsRequest(
                                coordinates = listOf(
                                    listOf(start.second, start.first),
                                    listOf(end.second, end.first)
                                ),
                                preference = preference,
                                options = DirectionsOptions(
                                    avoid_polygons = avoidPolygons
                                )
                            )

                            // Log del request completo
                            Log.d("MapViewModel", "📤 Request $preference: ${com.google.gson.Gson().toJson(request)}")

                            val response = RetrofitInstance.api.getRoute(currentMode, request)
                            val route = response.routes.firstOrNull()

                            Log.d("MapViewModel", "📊 Respuesta ORS para $preference:")
                            Log.d("MapViewModel", "   - Distancia: ${route?.summary?.distance}")
                            Log.d("MapViewModel", "   - Duración: ${route?.summary?.duration}")
                            Log.d("MapViewModel", "   - Geometry: ${route?.geometry?.take(50)}...")

                            RouteAlternative(
                                type = preference,
                                displayName = getPreferenceDisplayName(preference),
                                response = response.copy(profile = currentMode),
                                distance = (route?.summary?.distance ?: 0.0) * 1000,
                                duration = route?.summary?.duration ?: 0.0,
                                isRecommended = false
                            )
                        } catch (e: retrofit2.HttpException) {
                            val errorBody = e.response()?.errorBody()?.string()
                            Log.e("MapViewModel", "❌ HTTP ${e.code()} - Error body: $errorBody")
                            Log.e("MapViewModel", "Error calculando ruta $preference (evitando zonas)", e)
                            null
                        } catch (e: Exception) {
                            Log.e("MapViewModel", "Error calculando ruta $preference (evitando zonas)", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                if (routes.isEmpty()) {
                    Log.e("MapViewModel", "❌ No se pudieron calcular rutas alternativas")
                    _isRegeneratingRoutes.value = false
                    return@launch
                }

                // 5. Validar las nuevas rutas contra zonas peligrosas
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

                    Log.d("MapViewModel", "🔐 Validación de rutas regeneradas:")
                    Log.d("MapViewModel", "  - Todas seguras: ${validacion.todasSeguras}")
                    Log.d("MapViewModel", "  - ML recomienda: ${validacion.tipoMlRecomendado}")

                    // 6. Combinar rutas con información de seguridad
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
                    _rutasGeneradasEvitandoZonas.value = true

                    Log.d("MapViewModel", "✅ ${routes.size} rutas regeneradas evitando zonas peligrosas")

                } catch (e: Exception) {
                    Log.e("MapViewModel", "⚠️ Error validando seguridad de rutas regeneradas", e)
                    _alternativeRoutes.value = routes
                    _rutasGeneradasEvitandoZonas.value = true
                }

                _isRegeneratingRoutes.value = false

            } catch (e: Exception) {
                Log.e("MapViewModel", "Error general regenerando rutas seguras", e)
                _isRegeneratingRoutes.value = false
            }
        }
    }

    // ✅ Función auxiliar corregida - ahora retorna List<List<Double>>
    private fun convertirCirculoAPoligono(lat: Double, lon: Double, radioMetros: Int): List<List<Double>> {
        val puntos = mutableListOf<List<Double>>()
        val numPuntos = 16

        val radioTierraKm = 6371.0
        val radioTierraMetros = radioTierraKm * 1000.0

        for (i in 0 until numPuntos) {
            val angulo = 2 * Math.PI * i / numPuntos

            val deltaLat = (radioMetros / radioTierraMetros) * (180.0 / Math.PI)
            val deltaLon = (radioMetros / radioTierraMetros) * (180.0 / Math.PI) /
                    Math.cos(Math.toRadians(lat))

            val newLat = lat + (deltaLat * Math.cos(angulo))
            val newLon = lon + (deltaLon * Math.sin(angulo))

            // ORS espera formato [lon, lat]
            puntos.add(listOf(newLon, newLat))
        }

        // Cerrar el polígono
        puntos.add(puntos.first())

        Log.d("MapViewModel", "🔷 Polígono generado con ${puntos.size} puntos para radio ${radioMetros}m")

        return puntos
    }

    fun eliminarZonaPeligrosa(
        zonaId: Int,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("MapViewModel", "🗑️ Eliminando zona ID: $zonaId")

                val response = RetrofitClient.rutasApiService.eliminarZonaPeligrosa(
                    token = "Bearer $token",
                    zonaId = zonaId
                )

                if (response.isSuccessful) {
                    Log.d("MapViewModel", "✅ Zona eliminada correctamente")

                    // Actualizar lista local de zonas
                    _zonasPeligrosas.value = _zonasPeligrosas.value.filter {
                        it.id != zonaId
                    }

                    onSuccess()
                } else {
                    val errorMsg = "Error ${response.code()}: ${response.message()}"
                    Log.e("MapViewModel", "❌ $errorMsg")
                    onError(errorMsg)
                }

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Error desconocido"
                Log.e("MapViewModel", "❌ Error eliminando zona: $errorMsg", e)
                onError(errorMsg)
            }
        }
    }

    // 🆕 Reset del estado de regeneración
    fun resetRegeneracionZonas() {
        _rutasGeneradasEvitandoZonas.value = false
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


    override fun onCleared() {
        super.onCleared()
        _zonasPeligrosas.value = emptyList()
    }
}