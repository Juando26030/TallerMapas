package com.example.tallermapas.funciones

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

class FuncionesPermisos {

    companion object {
        // Método general para solicitar cualquier permiso
        fun checkAndRequestPermission(
            activity: Activity,
            permission: String,
            requestCode: Int,
            onPermissionGranted: () -> Unit
        ) {
            // Verificar si el permiso ya está concedido
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                // Si el permiso está concedido, ejecutar la lógica que necesita el permiso
                onPermissionGranted()
            } else {
                // Si el permiso no está concedido, verificar si se debe mostrar una explicación
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // Mostrar un mensaje explicando por qué se necesita el permiso
                    Toast.makeText(activity, "Este permiso es necesario para el funcionamiento completo de la app.", Toast.LENGTH_LONG).show()
                }
                // Solicitar el permiso
                ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
            }
        }
    }
}
