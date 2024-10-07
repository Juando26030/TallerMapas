package com.example.tallermapas.funciones

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.Locale

class FuncionesUbicacion {

    companion object {

        // Obtener la ubicación actual del usuario
        fun obtenerUbicacionActual(context: Context, callback: (Pair<Double, Double>?) -> Unit) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permisos de ubicación no concedidos", Toast.LENGTH_LONG).show()
                callback(null)
                return
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val latitud = location.latitude
                        val longitud = location.longitude
                        callback(Pair(latitud, longitud))
                    } else {
                        Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_LONG).show()
                        callback(null)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al obtener la ubicación", Toast.LENGTH_LONG).show()
                    callback(null)
                }
        }

        // Obtener la latitud y longitud desde una dirección en texto
        fun obtenerLatLngDesdeDireccion(context: Context, direccion: String, callback: (LatLng?) -> Unit) {
            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                val resultados = geocoder.getFromLocationName(direccion, 1)
                if (resultados != null && resultados.isNotEmpty()) {
                    val location = resultados[0]
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    Toast.makeText(context, "No se encontró la dirección.", Toast.LENGTH_LONG).show()
                    callback(null)
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Error al buscar la dirección.", Toast.LENGTH_LONG).show()
                callback(null)
            }
        }

        // Obtener la dirección desde latitud y longitud
        fun obtenerDireccionDesdeLatLng(context: Context, latLng: LatLng, callback: (String?) -> Unit) {
            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                val resultados = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (resultados != null && resultados.isNotEmpty()) {
                    callback(resultados[0].getAddressLine(0))
                } else {
                    Toast.makeText(context, "No se pudo encontrar una dirección.", Toast.LENGTH_LONG).show()
                    callback(null)
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Error al obtener la dirección.", Toast.LENGTH_LONG).show()
                callback(null)
            }
        }

        // Calcular la distancia entre dos puntos en metros
        fun calcularDistanciaEntrePuntos(latLng1: LatLng, latLng2: LatLng): Float {
            val location1 = Location("").apply {
                latitude = latLng1.latitude
                longitude = latLng1.longitude
            }
            val location2 = Location("").apply {
                latitude = latLng2.latitude
                longitude = latLng2.longitude
            }
            return location1.distanceTo(location2)/1000 // Retorna la distancia en km
        }
    }
}
