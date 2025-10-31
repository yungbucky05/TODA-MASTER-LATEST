package com.example.toda.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.example.toda.R
import com.example.toda.service.GeocodingService
import com.example.toda.service.RoutingService
import javax.inject.Inject

@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    pickupLocation: GeoPoint?,
    dropoffLocation: GeoPoint?,
    onMapClick: (GeoPoint) -> Unit,
    onPickupLocationDragged: (GeoPoint) -> Unit = {},
    onDropoffLocationDragged: (GeoPoint) -> Unit = {},
    onInvalidLocationSelected: (String) -> Unit = {},
    restrictToBarangay177: Boolean = false,
    enableZoom: Boolean = true,
    enableDrag: Boolean = true,
    validateBarangay177: Boolean = true,
    routingService: RoutingService? = null
) {
    val context = LocalContext.current
    val geocodingService = remember { GeocodingService() }
    val injectedRoutingService = routingService ?: remember { RoutingService() }

    // State to hold the current route points
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(false) }

    // Calculate route when both locations are available
    LaunchedEffect(pickupLocation, dropoffLocation) {
        if (pickupLocation != null && dropoffLocation != null) {
            isLoadingRoute = true
            try {
                val points = injectedRoutingService.getRouteWithFallback(pickupLocation, dropoffLocation)
                routePoints = points
            } catch (e: Exception) {
                // Fallback to straight line if routing fails
                routePoints = listOf(pickupLocation, dropoffLocation)
            } finally {
                isLoadingRoute = false
            }
        } else {
            routePoints = emptyList()
        }
    }

    // Initialize OSMDroid configuration
    DisposableEffect(context) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(enableZoom)

            // Set initial view
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(14.74800540601891, 121.0499004))

            // Enable map scrolling and dragging
            isTilesScaledToDpi = true
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false

            // Set minimum and maximum zoom levels
            minZoomLevel = 14.0
            maxZoomLevel = 20.0

            // Restrict map bounds if needed
            if (restrictToBarangay177) {
                val centerLat = 14.74800540601891
                val centerLon = 121.0499004
                val offset = 0.01

                val boundingBox = BoundingBox(
                    centerLat + offset,
                    centerLon + offset,
                    centerLat - offset,
                    centerLon - offset
                )
                setScrollableAreaLimitDouble(boundingBox)
            }
        }
    }

    AndroidView(
        factory = { mapView },
        update = { mapView ->
            mapView.overlayManager.clear()

            // Add map click listener with validation
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let { geoPoint ->
                        println("=== MAP CLICK DEBUG ===")
                        println("Clicked coordinates: ${geoPoint.latitude}, ${geoPoint.longitude}")
                        println("Validation enabled: $validateBarangay177")

                        if (validateBarangay177) {
                            val isValid = geocodingService.isWithinBarangay177(geoPoint.latitude, geoPoint.longitude)
                            println("Location is valid: $isValid")

                            if (isValid) {
                                println("✅ Valid location - calling onMapClick")
                                onMapClick(geoPoint)
                            } else {
                                println("❌ Invalid location - calling onInvalidLocationSelected")
                                onInvalidLocationSelected("Only locations within Barangay 177 are allowed")
                                // DO NOT call onMapClick for invalid locations
                            }
                        } else {
                            println("Validation disabled - calling onMapClick")
                            onMapClick(geoPoint)
                        }
                        println("=======================")
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    return false
                }
            }

            val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
            mapView.overlayManager.add(0, mapEventsOverlay)

            // Add pickup marker
            pickupLocation?.let { location ->
                val pickupMarker = Marker(mapView).apply {
                    position = location
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Pickup Location"

                    // Try to use custom drawable, fallback to default
                    try {
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_pickup_pin)
                    } catch (e: Exception) {
                        // Use default OSM marker
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                    }

                    isDraggable = enableDrag

                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(marker: Marker?) {}

                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.position?.let { newPosition ->
                                if (validateBarangay177) {
                                    if (geocodingService.isWithinBarangay177(newPosition.latitude, newPosition.longitude)) {
                                        onPickupLocationDragged(newPosition)
                                    } else {
                                        // Reset marker to original position and show error
                                        marker.position = location
                                        onInvalidLocationSelected("Pickup location must be within Barangay 177")
                                        mapView.invalidate()
                                    }
                                } else {
                                    onPickupLocationDragged(newPosition)
                                }
                            }
                        }

                        override fun onMarkerDragStart(marker: Marker?) {}
                    })
                }
                mapView.overlayManager.add(pickupMarker)
            }

            // Add dropoff marker
            dropoffLocation?.let { location ->
                val dropoffMarker = Marker(mapView).apply {
                    position = location
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Destination"

                    // Try to use custom drawable, fallback to default
                    try {
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_destination_pin)
                    } catch (e: Exception) {
                        // Use default Android marker
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map)
                    }

                    isDraggable = enableDrag

                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(marker: Marker?) {}

                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.position?.let { newPosition ->
                                if (validateBarangay177) {
                                    if (geocodingService.isWithinBarangay177(newPosition.latitude, newPosition.longitude)) {
                                        onDropoffLocationDragged(newPosition)
                                    } else {
                                        // Reset marker to original position and show error
                                        marker.position = location
                                        onInvalidLocationSelected("Destination must be within Barangay 177")
                                        mapView.invalidate()
                                    }
                                } else {
                                    onDropoffLocationDragged(newPosition)
                                }
                            }
                        }

                        override fun onMarkerDragStart(marker: Marker?) {}
                    })
                }
                mapView.overlayManager.add(dropoffMarker)
            }

            // Draw route using calculated route points instead of straight line
            if (routePoints.isNotEmpty()) {
                val roadOverlay = Polyline(mapView).apply {
                    outlinePaint.color = if (isLoadingRoute) {
                        android.graphics.Color.GRAY // Show gray while loading
                    } else {
                        android.graphics.Color.BLUE // Show blue when route is ready
                    }
                    outlinePaint.strokeWidth = 8.0f

                    // Add all route points to create the polyline
                    routePoints.forEach { point ->
                        addPoint(point)
                    }
                }
                mapView.overlayManager.add(roadOverlay)
            }

            mapView.invalidate()
        },
        modifier = modifier
    )

    // Cleanup
    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }
}