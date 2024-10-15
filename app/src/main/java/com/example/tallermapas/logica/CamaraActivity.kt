package com.example.tallermapas.logica

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tallermapas.R
import com.example.tallermapas.funciones.FuncionesPermisos
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class CamaraActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var photoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        imageView = findViewById(R.id.selected_image)
        btnCamera = findViewById(R.id.btn_camera)
        btnGallery = findViewById(R.id.btn_gallery)

        btnCamera.setOnClickListener {
            FuncionesPermisos.checkAndRequestPermission(
                this,
                Manifest.permission.CAMERA,
                CAMERA_PERMISSION_CODE
            ) {
                openCamera()
            }
        }

        btnGallery.setOnClickListener {
            FuncionesPermisos.checkAndRequestPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                STORAGE_PERMISSION_CODE
            ) {
                openGallery()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Cargar la imagen completa desde el archivo temporal
            imageView.setImageURI(photoUri)
        }
    }


    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let {
                imageView.setImageURI(it)
            }
        }
    }

    private fun openCamera() {
        // Crear un archivo temporal para almacenar la foto
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            this,
            "com.example.tallermapas.fileprovider",  // Cambiado a la cadena literal
            photoFile
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraLauncher.launch(cameraIntent)
    }


    // Crear un archivo temporal para la imagen
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        galleryLauncher.launch(galleryIntent)
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
        private const val STORAGE_PERMISSION_CODE = 102
    }
}
