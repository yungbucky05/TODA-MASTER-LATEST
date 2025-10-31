package com.example.toda.data

import com.google.gson.annotations.SerializedName

data class RouteResponse(
    @SerializedName("features")
    val features: List<Feature>
)

data class Feature(
    @SerializedName("geometry")
    val geometry: Geometry,
    @SerializedName("properties")
    val properties: RouteProperties
)

data class Geometry(
    @SerializedName("coordinates")
    val coordinates: List<List<Double>>,
    @SerializedName("type")
    val type: String
)

data class RouteProperties(
    @SerializedName("segments")
    val segments: List<Segment>,
    @SerializedName("summary")
    val summary: Summary
)

data class Segment(
    @SerializedName("distance")
    val distance: Double,
    @SerializedName("duration")
    val duration: Double
)

data class Summary(
    @SerializedName("distance")
    val distance: Double,
    @SerializedName("duration")
    val duration: Double
)
