package com.example.app.models

data class PoisRequest(
    val request: String = "pois",
    val geometry: Geometry
)

data class Geometry(
    val geojson: GeoJson,
    val buffer: Int = 100
)

data class GeoJson(
    val type: String = "Point",
    val coordinates: List<Double>
)

data class PoisResponse(
    val type: String?,
    val features: List<Feature>?
)

data class Feature(
    val geometry: GeometryData?,
    val properties: Properties?
)

data class GeometryData(
    val type: String?,
    val coordinates: List<Double>?
)

data class Properties(
    val osm_id: Long?,
    val osm_tags: Map<String, String>?,
    val category_ids: Map<String, CategoryInfo>?
)

data class CategoryInfo(
    val category_name: String?,
    val category_group: String?
)

fun Feature.getDisplayName(): String {
    // 1. Intentar obtener el nombre de osm_tags
    val osmName = properties?.osm_tags?.get("name")
    if (!osmName.isNullOrBlank()) return osmName

    // 2. Si no tiene nombre, usar la categoría
    val category = properties?.category_ids?.values?.firstOrNull()
    if (category != null) {
        val categoryName = category.category_name ?: ""
        val categoryGroup = category.category_group ?: ""
        return "$categoryGroup - $categoryName".trim('-', ' ')
    }

    // 3. Fallback final
    return "POI #${properties?.osm_id ?: "desconocido"}"
}