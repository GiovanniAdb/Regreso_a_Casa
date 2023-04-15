package com.example.regresoacasa
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : AppCompatActivity(), MapEventsReceiver {
    private lateinit var mapView: MapView
    private lateinit var mapController: IMapController
    private lateinit var locationManager: LocationManager
    private lateinit var requestQueue: RequestQueue
    private lateinit var homeAddressEditText: EditText
    private lateinit var buttonGetRoute: Button
    private lateinit var currentLocation: GeoPoint
    private lateinit var homeLocation: GeoPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar la configuración de osmdroid
        val ctx = applicationContext
        Configuration.getInstance()
            .load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map_view)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        mapController = mapView.controller

        // Obtener referencias a los componentes de la vista
        homeAddressEditText = findViewById(R.id.destiny)
        buttonGetRoute = findViewById(R.id.btn_get_directions)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        requestQueue = Volley.newRequestQueue(this)

        // Agregar el listener para el botón de obtener ruta
        buttonGetRoute.setOnClickListener {
            getRoute()
        }

        // Comprobar los permisos de ubicación
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
            return
        }

        // Obtener la última ubicación conocida del proveedor de ubicación GPS
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
            currentLocation = GeoPoint(it.latitude, it.longitude)
            mapController.setZoom(15.0)
            mapController.setCenter(currentLocation)
            val currentLocationMarker = Marker(mapView)
            currentLocationMarker.position = currentLocation
            currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(currentLocationMarker)
        }
    }

    private fun getRoute() {
        // Verificar que la dirección esté configurada
        if (TextUtils.isEmpty(homeAddressEditText.toString())) {
            Toast.makeText(this, "Por favor, configure la dirección de destino en la configuración", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar que tengamos la ubicación actual del usuario
        if (currentLocation == null) {
            Toast.makeText(this, "No se puede obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            return
        }

        // Construir la URL de la API de directions de OpenRouteService
        val API_KEY = "5b3ce3597851110001cf624868f24090014d479a85093aac8026fa62"
        val url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=$API_KEY&start=${currentLocation!!.latitude},${currentLocation!!.longitude}&end=$homeAddressEditText"

        // Crear la solicitud HTTP para obtener la ruta
        val request = object : StringRequest(Request.Method.GET, url, Response.Listener { response ->
            // Procesar la respuesta de la API de directions
            val route = JSONObject(response).getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")

            // Crear una lista de puntos para la ruta
            val points = ArrayList<GeoPoint>()
            for (i in 0 until route.length()) {
                val point = route.getJSONArray(i)
                val lat = point.getDouble(1)
                val lon = point.getDouble(0)
                points.add(GeoPoint(lat, lon))
            }

            // Agregar la ruta al mapa en forma de una Polyline
            val line = Polyline()
            line.setPoints(points)
            line.width = 8f
            line.color = Color.RED
            mapView.overlayManager.add(line)
            mapView.invalidate()
        }, Response.ErrorListener { error ->
            Toast.makeText(this, "Error al obtener la ruta: ${error.message}", Toast.LENGTH_SHORT).show()
        }) {
            // Agregar la cabecera de autorización a la solicitud HTTP
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = API_KEY
                return headers
            }
        }

        // Agregar la solicitud HTTP a la cola de solicitudes
        requestQueue.add(request)
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        TODO("Not yet implemented")
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        // TODO: handle long press event on map
        return true
    }
}