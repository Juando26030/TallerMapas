package com.example.tallermapas.adaptador

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tallermapas.R

data class Contact(val id: String, val name: String, val imageUri: String?)

class ContactsAdapter(private val contacts: List<Contact>) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contactImage: ImageView = view.findViewById(R.id.contactImage)
        val contactId: TextView = view.findViewById(R.id.contactId)
        val contactName: TextView = view.findViewById(R.id.contactName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.contactId.text = "ID: ${contact.id}"
        holder.contactName.text = contact.name

        // Cargar imagen con Glide si la URI no es nula
        if (contact.imageUri != null) {
            Glide.with(holder.itemView.context)
                .load(contact.imageUri)
                .placeholder(R.drawable.ic_launcher_foreground) // Placeholder mientras carga
                .into(holder.contactImage)
        } else {
            holder.contactImage.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    override fun getItemCount(): Int = contacts.size
}
