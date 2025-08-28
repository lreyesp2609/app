package com.example.app.models

data class DirectionsResponse(
    val routes: List<Route>
)

data class Route(
    val summary: Summary,
    val geometry: String,
    val segments: List<Segment>
)

data class Summary(
    val distance: Double, // en metros
    val duration: Double  // en segundos
)

data class Segment(
    val distance: Double,
    val duration: Double,
    val steps: List<Step>
)

data class Step(
    val instruction: String,
    val distance: Double,
    val duration: Double
)

data class DirectionsRequest(
    val coordinates: List<List<Double>>,
    val language: String = "es",
    val units: String = "km",
    val instructions: Boolean = true
)