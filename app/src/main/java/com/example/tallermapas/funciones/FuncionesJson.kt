package com.example.tallermapas.funciones

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FuncionesJson {

    companion object {

        private val TAG = "FuncionesJson"

        // Función para escribir datos en un archivo JSON
        fun escribirEnJson(context: Context, nombreArchivo: String, latitud: Double, longitud: Double) {
            val file = File(context.filesDir, nombreArchivo)
            val jsonUbicacion = JSONObject()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDateAndTime = sdf.format(Date())

            try {
                // Si el archivo ya existe, leemos el contenido actual
                val jsonArray = if (file.exists()) {
                    val jsonString = file.readText()
                    val json = JSONObject(jsonString)
                    json.getJSONArray("ubicaciones")
                } else {
                    JSONArray()
                }

                // Crear un nuevo objeto con la ubicación actual
                jsonUbicacion.put("latitud", latitud)
                jsonUbicacion.put("longitud", longitud)
                jsonUbicacion.put("fecha_hora", currentDateAndTime)

                // Añadir la nueva ubicación al array
                jsonArray.put(jsonUbicacion)

                // Crear el objeto JSON que contiene todas las ubicaciones
                val jsonFinal = JSONObject()
                jsonFinal.put("ubicaciones", jsonArray)

                // Guardar el JSON en el archivo
                FileWriter(file).use { it.write(jsonFinal.toString()) }
                Log.d(TAG, "Ubicación guardada correctamente: $latitud, $longitud")
            } catch (e: IOException) {
                Log.e(TAG, "Error escribiendo en el archivo JSON: ${e.message}")
            }
        }

        // Función para leer desde un archivo JSON
        fun leerDesdeJson(context: Context, nombreArchivo: String): List<Pair<Double, Double>> {
            val listaUbicaciones = mutableListOf<Pair<Double, Double>>()
            val file = File(context.filesDir, nombreArchivo)

            if (!file.exists()) {
                Log.e(TAG, "El archivo $nombreArchivo no existe.")
                return listaUbicaciones
            }

            try {
                // Leer el contenido del archivo
                val jsonString = file.readText()
                val json = JSONObject(jsonString)
                val jsonArray = json.getJSONArray("ubicaciones")

                // Procesar cada ubicación
                for (i in 0 until jsonArray.length()) {
                    val ubicacionJson = jsonArray.getJSONObject(i)
                    val latitud = ubicacionJson.getDouble("latitud")
                    val longitud = ubicacionJson.getDouble("longitud")
                    listaUbicaciones.add(Pair(latitud, longitud))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo el archivo JSON: ${e.message}")
            }

            return listaUbicaciones
        }
    }
}
