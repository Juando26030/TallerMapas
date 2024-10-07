package com.example.tallermapas.adaptadores

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class AutoCompleteAdapter(
    private val context: Context,
    private val placesClient: PlacesClient,
    private val autoCompleteTextView: AutoCompleteTextView,
    private var ubicacionActual: LatLng? = null // Añadir ubicación actual
) {

    private val autoCompleteAdapter = ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line)

    init {
        // Asignar el adaptador al AutoCompleteTextView
        autoCompleteTextView.setAdapter(autoCompleteAdapter)

        // Token para sesión de autocompletado
        val token = AutocompleteSessionToken.newInstance()

        // Escuchar cambios de texto para realizar la búsqueda
        autoCompleteTextView.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && s.length > 2) {
                    // Crear una solicitud de autocompletado
                    val requestBuilder = FindAutocompletePredictionsRequest.builder()
                        .setSessionToken(token)
                        .setQuery(s.toString())

                    // Si se tiene la ubicación actual, añadir LocationBias
                    ubicacionActual?.let {
                        val bounds = RectangularBounds.newInstance(
                            LatLng(it.latitude - 0.1, it.longitude - 0.1), // Limites geográficos aproximados
                            LatLng(it.latitude + 0.1, it.longitude + 0.1)
                        )
                        requestBuilder.setLocationBias(bounds) // Prioriza los resultados cercanos
                    }

                    val request = requestBuilder.build()

                    // Enviar la solicitud al Places API
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            val predictions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                            autoCompleteAdapter.clear()
                            autoCompleteAdapter.addAll(predictions)
                            autoCompleteAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Error al obtener sugerencias: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    // Método para actualizar la ubicación actual
    fun setUbicacionActual(ubicacion: LatLng) {
        this.ubicacionActual = ubicacion
    }

    fun setOnItemClickListener(callback: (String) -> Unit) {
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val prediction = autoCompleteAdapter.getItem(position)
            if (prediction != null) {
                callback(prediction)
            }
        }
    }
}

