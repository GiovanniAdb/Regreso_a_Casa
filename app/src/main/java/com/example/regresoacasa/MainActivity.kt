package com.example.regresoacasa

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.telecom.Call
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapController: IMapController
    private lateinit var locationOverlay: MyLocationNewOverlay

    private lateinit var routeManager: RouteManager
    private lateinit var startLocation: Location
    private lateinit var endLocation: GeoPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map_view)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        mapController = mapView.controller

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        mapView.overlays.add(locationOverlay)

        val homeLocation = Location("")
        homeLocation.latitude = YOUR_HOME_LATITUDE
        homeLocation.longitude = YOUR_HOME_LONGITUDE

        endLocation = GeoPoint(homeLocation.latitude, homeLocation.longitude)

        routeManager = RouteManager()

        startLocation = locationOverlay.myLocation

        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                val newLocation = locationOverlay.myLocation
                if (newLocation != null) {
                    startLocation = newLocation
                    drawRoute()
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnable)
    }

    private fun drawRoute() {
        routeManager.getRoute(startLocation, "${endLocation.latitude},${endLocation.longitude}", object : Callback<RouteResponse> {
            override fun onResponse(call: Call<RouteResponse>, response: Response<RouteResponse>) {
                if (response.isSuccessful) {
                    val routeResponse = response.body()
                    val routePoints = decodePolyline(routeResponse?.routes?.get(0)?.geometry)
                    val routeLine = Polyline(mapView)
                    routeLine.setPoints(routePoints)
                    routeLine.paint.color = Color.RED
                    mapView.overlays.add(routeLine)
                    mapController.setCenter(routePoints[0])
                    mapController.setZoom(15.0)
                }
            }

            override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error al obtener la ruta", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun decodePolyline(polyline: String?): List<GeoPoint> {
        val decodedPoints = PolylineDecoder().decode(polyline)
        return decodedPoints.map { GeoPoint(it.latitude, it.longitude) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationOverlay.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
