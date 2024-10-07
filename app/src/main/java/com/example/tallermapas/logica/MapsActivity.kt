package com.example.tallermapas.logica

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tallermapas.R
import com.example.tallermapas.adaptadores.AutoCompleteAdapter
import com.example.tallermapas.databinding.ActivityMapsBinding
import com.example.tallermapas.funciones.FuncionesPermisos
import com.example.tallermapas.funciones.FuncionesUbicacion
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.GeoPoint


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var lastLocation: Location? = null
    private var ubicacionActual: LatLng? = null
    private val locationFileName = "ubicaciones.json"

    // Variables para Places API
    private lateinit var placesClient: PlacesClient
    private lateinit var autoCompleteAdapter: AutoCompleteAdapter
    private var currentMarker: Marker? = null // Variable para almacenar el marcador actual
    private var polyline: Polyline? = null // Variable para la polilínea (ruta)

    // SensorManager para detectar el sensor de luz
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var isNightMode: Boolean = false // Bandera para controlar el modo nocturno

    // Código de solicitud de permisos para la ubicación
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Inicializar ViewBinding
            binding = ActivityMapsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Verificar permisos de ubicación utilizando FuncionesPermisos
            FuncionesPermisos.checkAndRequestPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE
            ) {
                // Si el permiso es otorgado, proceder a obtener la ubicación y cargar el mapa
                obtenerUbicacionYActualizar()
            }

            // Inicializar el SDK de Places utilizando la clave desde el AndroidManifest.xml
            val apiKey = getApiKeyFromManifest()
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }
            placesClient = Places.createClient(this)

            // Inicializar SensorManager y el sensor de luz
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

            // Obtener el fragmento del mapa y configurar el callback
            val mapFragment = supportFragmentManager.findFragmentById(R.id.mapa) as SupportMapFragment
            mapFragment.getMapAsync(this)

            // Configurar AutoCompleteTextView para autocompletar direcciones
            val destinoAutoCompleteTextView: AutoCompleteTextView = findViewById(R.id.destino)

            // Inicializar el adaptador de autocompletado personalizado
            autoCompleteAdapter = AutoCompleteAdapter(this, placesClient, destinoAutoCompleteTextView)



        } catch (e: Exception) {
            Log.e("MapsActivity", "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error al iniciar la actividad: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            mMap = googleMap

            // Obtener la ubicación actual y agregar el marcador inicial
            FuncionesUbicacion.obtenerUbicacionActual(this) { ubicacion ->
                if (ubicacion != null) {
                    ubicacionActual = LatLng(ubicacion.first, ubicacion.second)

                    // Actualiza la ubicación en el AutoCompleteAdapter
                    autoCompleteAdapter.setUbicacionActual(ubicacionActual!!)

                    // Hacer zoom a nivel 15 primero
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual!!, 15f))

                    // Luego agregar el marcador
                    mMap.addMarker(MarkerOptions().position(ubicacionActual!!).title("Ubicación Actual"))

                    // Mostrar mensaje de confirmación
                    Toast.makeText(this, "Marcador de ubicación inicial agregado.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación inicial.", Toast.LENGTH_LONG).show()
                }
            }

            // Evento de LongClick en el mapa para crear un marcador con la dirección
            mMap.setOnMapLongClickListener { latLng ->
                FuncionesUbicacion.obtenerDireccionDesdeLatLng(this, latLng) { direccion ->
                    if (direccion != null) {
                        // Agregar el marcador con la dirección obtenida
                        mMap.addMarker(MarkerOptions().position(latLng).title(direccion))

                        // Mostrar la distancia al nuevo marcador si se tiene la ubicación actual
                        if (ubicacionActual != null) {
                            val distancia = FuncionesUbicacion.calcularDistanciaEntrePuntos(ubicacionActual!!, latLng)
                            Toast.makeText(this, "Distancia al marcador: $distancia metros", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }


            autoCompleteAdapter.setOnItemClickListener { prediction ->
                FuncionesUbicacion.obtenerLatLngDesdeDireccion(this, prediction) { latLng ->
                    if (latLng != null) {
                        // Eliminar el marcador anterior si existe
                        currentMarker?.remove()

                        // Agregar el nuevo marcador
                        currentMarker = mMap.addMarker(MarkerOptions().position(latLng).title(prediction))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                        // Dibujar la ruta entre la ubicación actual y el destino
                        if (ubicacionActual != null) {
                            dibujarRuta(ubicacionActual!!, latLng)
                            val distancia = FuncionesUbicacion.calcularDistanciaEntrePuntos(ubicacionActual!!, latLng)
                            actualizarDistancia(distancia)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("MapsActivity", "Error en onMapReady: ${e.message}")
            Toast.makeText(this, "Error al preparar el mapa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun dibujarRuta(origen: LatLng, destino: LatLng) {
        // Eliminar la polilínea anterior si existe
        polyline?.remove()

        // Crear una nueva polilínea (ruta)
        polyline = mMap.addPolyline(
            PolylineOptions()
                .add(origen, destino)
                .width(10f)
                .color(ContextCompat.getColor(this, R.color.purple)) // Color de la ruta
        )
    }

    private fun actualizarDistancia(distancia: Float) {
        val textViewDistancia: TextView = findViewById(R.id.distancia)
        // Redondeamos la distancia a un solo decimal
        val distanciaFormateada = String.format("%.1f", distancia)
        textViewDistancia.text = "Distancia al destino: ${distanciaFormateada} km"
    }
    // Método para obtener la API key del manifiesto
    private fun getApiKeyFromManifest(): String {
        try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            return bundle.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("MapsActivity", "Error al obtener la API Key: ${e.message}")
        }
        return ""
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se necesita implementar nada aquí en este caso
    }

    // Método para solicitar la ubicación actual y actualizar en el mapa
    private fun obtenerUbicacionYActualizar() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 10000).build()

        // Verificar permisos utilizando FuncionesPermisos
        FuncionesPermisos.checkAndRequestPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            LOCATION_PERMISSION_REQUEST_CODE
        ) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    lastLocation = locationResult.lastLocation
                    if (lastLocation != null) {
                        ubicacionActual = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual!!, 15f))
                        mMap.addMarker(MarkerOptions().position(ubicacionActual!!).title("Ubicación Actual"))
                    }
                }
            }, null)
        }
    }
}
