package com.example.tallermapas.logica

import com.android.volley.Request
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.tallermapas.databinding.ActivityMapsBinding
import com.example.tallermapas.R
import com.example.tallermapas.funciones.FuncionesJson
import com.example.tallermapas.funciones.FuncionesPermisos
import com.example.tallermapas.funciones.FuncionesUbicacion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var lastLocation: Location? = null
    private val LOCATION_PERMISSION_CODE = 100

    // SensorManager para detectar el sensor de luz
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var isNightMode: Boolean = false // Bandera para controlar el modo nocturno

    // Variables para la ruta
    private var polyline: Polyline? = null
    private val routePoints = mutableListOf<LatLng>() // Lista que almacenará los puntos de la ruta

    //Inicializar Geocoder
    private lateinit var geocoder: Geocoder
    private var currentLocationMarker: MarkerOptions? = null // Marcador de mi ubicación
    private var destinationMarker: MarkerOptions? = null // Marcador del destino



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar SensorManager y el sensor de luz
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Obtener el fragmento del mapa y configurar el callback
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        geocoder = Geocoder(this)  // Inicializar el Geocoder

        // Evento para buscar la dirección cuando el usuario termina de editar el EditText
        binding.etDireccion.setOnEditorActionListener { v, actionId, event ->
            val direccion = binding.etDireccion.text.toString()
            if (direccion.isNotEmpty()) {
                buscarDireccion(direccion)
            }
            false
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Verificar y solicitar permisos de ubicación antes de obtener la ubicación actual
        FuncionesPermisos.checkAndRequestPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            LOCATION_PERMISSION_CODE
        ) {
            // Obtener la ubicación actual y agregar el marcador inicial
            FuncionesUbicacion.obtenerUbicacionActual(this) { ubicacion ->
                if (ubicacion != null) {
                    val latLng = LatLng(ubicacion.first, ubicacion.second)

                    // Hacer zoom a nivel 15
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    // Guardar el marcador de la ubicación actual en la variable para no perderlo
                    if (currentLocationMarker == null) {
                        // Si es la primera vez, creamos el marcador de la ubicación actual
                        currentLocationMarker = MarkerOptions().position(latLng).title("Ubicación actual")
                        mMap.addMarker(currentLocationMarker!!)
                    } else {
                        // Si ya existe, solo actualizamos su posición
                        currentLocationMarker?.position(latLng)
                    }

                    // Iniciar Polyline con la ubicación inicial
                    polyline = mMap.addPolyline(PolylineOptions().add(latLng))
                    routePoints.add(latLng)
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación inicial.", Toast.LENGTH_LONG).show()
                }
            }

            // Empezar a recibir actualizaciones de la ubicación para actualizar la ruta
            obtenerUbicacionYActualizar()
        }

        // Listener para LongClick en el mapa
        mMap.setOnMapLongClickListener { latLng ->
            buscarDireccionDeCoordenadas(latLng)

            // Calcular la ruta si hay una ubicación actual
            if (lastLocation != null) {
                val origen = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
                calcularYMostrarRuta(origen, latLng)  // Aquí se calcula la ruta desde mi ubicación hasta el punto tocado
            }
        }
    }



    // Función para obtener la dirección a partir de las coordenadas con LongClick
    private fun buscarDireccionDeCoordenadas(latLng: LatLng) {
        try {
            // Usamos el Geocoder para obtener la dirección de las coordenadas
            val direcciones = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (direcciones != null && direcciones.isNotEmpty()) {
                val direccion = direcciones[0].getAddressLine(0)

                // Agregar un marcador con la dirección y mover la cámara
                mMap.addMarker(MarkerOptions().position(latLng).title(direccion))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                Toast.makeText(this, "Marcador agregado en: $direccion", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No se encontró dirección para esta ubicación", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error al obtener la dirección", Toast.LENGTH_SHORT).show()
        }
    }

    // Registrar el listener del sensor de luz cuando la actividad se reanuda
    override fun onResume() {
        super.onResume()
        lightSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Desregistrar el listener cuando la actividad se pausa
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // Este método se llama cuando hay un cambio en el sensor de luz
    override fun onSensorChanged(event: SensorEvent) {
        val lux = event.values[0] // El valor del sensor de luz en lux

        try {
            // Si está oscuro (umbral de ejemplo: 30 lux), cambiar a modo oscuro
            if (lux < 30 && !isNightMode) {
                // Aplicar el estilo oscuro
                val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))
                if (success) {
                    Toast.makeText(this, "Modo nocturno", Toast.LENGTH_SHORT).show()
                    isNightMode = true // Actualizamos la bandera después de cambiar el estilo
                } else {
                    Toast.makeText(this, "Error al aplicar el estilo nocturno", Toast.LENGTH_SHORT).show()
                }
            }
            // Si hay luz suficiente, cambiar a modo claro
            else if (lux >= 30 && isNightMode) {
                // Aplicar el estilo claro
                val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_light))
                if (success) {
                    Toast.makeText(this, "Modo diurno", Toast.LENGTH_SHORT).show()
                    isNightMode = false // Actualizamos la bandera después de cambiar el estilo
                } else {
                    Toast.makeText(this, "Error al aplicar el estilo diurno", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MapsActivity", "Error al cambiar el estilo del mapa: ${e.message}")
        }
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No es necesario implementar esta función para el sensor de luz
    }

    // Método que maneja la solicitud de permisos y actualiza la ubicación
    private fun obtenerUbicacionYActualizar() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Intervalo de 5 segundos
        ).setMinUpdateIntervalMillis(3000L) // 3 segundos mínimo entre actualizaciones
            .setMinUpdateDistanceMeters(30f) // Desplazamiento mínimo de 30 metros
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                val latitud = location.latitude
                val longitud = location.longitude
                val ubicacionActual = LatLng(latitud, longitud)

                if (lastLocation == null || lastLocation!!.distanceTo(location) > 30) {
                    lastLocation = location

                    // 1. Guardar la nueva ubicación en el archivo JSON
                    FuncionesJson.escribirEnJson(this@MapsActivity, "ubicaciones.json", latitud, longitud)

                    // 2. Actualizar la Polyline con el nuevo punto
                    routePoints.add(ubicacionActual)
                    polyline?.points = routePoints

                    // 3. Actualizar el marcador en la nueva ubicación
                    if (currentLocationMarker != null) {
                        currentLocationMarker!!.position(ubicacionActual)
                    } else {
                        currentLocationMarker = MarkerOptions().position(ubicacionActual).title("Ubicación actual")
                        mMap.addMarker(currentLocationMarker!!)
                    }

                    // 4. Mover la cámara a la nueva ubicación
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual, 15f))
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }



    // Manejar el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYActualizar()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Función para buscar una dirección y agregar el marcador sin borrar el marcador actual
    private fun buscarDireccion(direccion: String) {
        try {
            val resultados = geocoder.getFromLocationName(direccion, 1)
            if (resultados != null && resultados.isNotEmpty()) {
                val location = resultados[0]
                val destino = LatLng(location.latitude, location.longitude)

                // Borrar el marcador del destino anterior si existe
                if (destinationMarker != null) {
                    // Solo removemos el marcador del destino anterior
                    mMap.clear() // Borrar solo el destino anterior

                    // Mantener el marcador de la ubicación actual
                    currentLocationMarker?.let { mMap.addMarker(it) }
                }

                // Crear el nuevo marcador de destino
                destinationMarker = MarkerOptions().position(destino).title(direccion)
                mMap.addMarker(destinationMarker!!)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destino, 15f))

                // Calcular y mostrar la ruta desde la ubicación actual
                if (lastLocation != null) {
                    val origen = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
                    calcularYMostrarRuta(origen, destino)
                }

            } else {
                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error al buscar la dirección", Toast.LENGTH_SHORT).show()
        }
    }


    // Función para obtener la dirección a partir de las coordenadas
    private fun obtenerDireccionDeCoordenadas(latLng: LatLng) {
        try {
            val direcciones = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (direcciones != null && direcciones.isNotEmpty()) {
                val direccion = direcciones[0].getAddressLine(0)

                // Agregar un marcador con la dirección
                mMap.addMarker(MarkerOptions().position(latLng).title(direccion))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "No se encontró dirección", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error al obtener dirección", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para calcular la ruta entre dos puntos y dibujarla en el mapa
    private fun calcularYMostrarRuta(origen: LatLng, destino: LatLng) {
        val apiKey = "AIzaSyDONA4QwbjORMb3NM9NJCRxmZCXJWfaSVM" // Asegúrate de reemplazarlo con tu propia API Key de Google Directions
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origen.latitude},${origen.longitude}&destination=${destino.latitude},${destino.longitude}&key=$apiKey"

        // Crear una solicitud HTTP usando Volley
        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    // Parsear la respuesta JSON
                    val jsonResponse = JSONObject(response)
                    val routes = jsonResponse.getJSONArray("routes")

                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val polylinePoints = route.getJSONObject("overview_polyline").getString("points")
                        val pointsList = decodePolyline(polylinePoints)

                        // Dibujar la ruta en el mapa
                        val polylineOptions = PolylineOptions()
                        polylineOptions.addAll(pointsList)
                        polylineOptions.width(10f)
                        polylineOptions.color(R.color.purple) // Puedes personalizar el color

                        // Añadir la Polyline en el mapa
                        mMap.addPolyline(polylineOptions)
                    } else {
                        Toast.makeText(this, "No se encontró ruta", Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al procesar la ruta", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Error en la solicitud: ${error.message}", Toast.LENGTH_LONG).show()
            })

        // Añadir la solicitud a la cola de Volley
        requestQueue.add(stringRequest)
    }

    // Función para decodificar el string de Polyline de Google en una lista de LatLng
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(
                lat / 1E5, lng / 1E5
            )
            poly.add(latLng)
        }

        return poly
    }

}