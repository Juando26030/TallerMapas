package com.example.tallermapas.logica

import android.Manifest
import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tallermapas.R
import com.example.tallermapas.adaptador.Contact
import com.example.tallermapas.adaptador.ContactsAdapter
import com.example.tallermapas.funciones.FuncionesPermisos

class ContactsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        recyclerView = findViewById(R.id.recyclerViewContacts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Pedir permiso de contactos
        FuncionesPermisos.checkAndRequestPermission(
            this,
            Manifest.permission.READ_CONTACTS,
            100
        ) {
            loadContacts() // Cargar contactos si el permiso se concede
        }
    }

    // Manejar resultado de la solicitud del permiso
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            loadContacts() // Intentar cargar contactos si el permiso se otorga
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContacts() {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (cursor == null) {
            Toast.makeText(this, "No se pudieron obtener los contactos", Toast.LENGTH_SHORT).show()
            return
        }

        cursor.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            if (idIndex >= 0 && nameIndex >= 0) {
                while (it.moveToNext()) {
                    val id = it.getString(idIndex) ?: "N/A"
                    val name = it.getString(nameIndex) ?: "Sin nombre"
                    val imageUri = it.getString(photoIndex)

                    Log.d("ContactsActivity", "ID: $id, Name: $name, Photo: $imageUri")

                    contacts.add(Contact(id, name, imageUri))
                }
            } else {
                Log.e("ContactsActivity", "No se encontraron columnas válidas.")
                Toast.makeText(this, "Error al cargar los contactos.", Toast.LENGTH_SHORT).show()
            }
        }

        if (contacts.isNotEmpty()) {
            recyclerView.adapter = ContactsAdapter(contacts)
        } else {
            Toast.makeText(this, "No hay contactos disponibles", Toast.LENGTH_SHORT).show()
        }
    }
}
