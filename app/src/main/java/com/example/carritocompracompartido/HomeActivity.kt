package com.example.carritocompracompartido

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.util.Util

enum class ProviderType {
    BASIC,
    GOOGLE
}

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private inner class ListsAdapter(private val lists: List<Map<String, Any>>) : RecyclerView.Adapter<ListsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val listNameTextView: TextView = itemView.findViewById(R.id.listNameTextView)
            val productCountTextView: TextView = itemView.findViewById(R.id.productCountTextView)
            val userImageViews: List<ImageView> = listOf(
                itemView.findViewById(R.id.userImageView1),
                itemView.findViewById(R.id.userImageView2),
                itemView.findViewById(R.id.userImageView3),
                itemView.findViewById(R.id.addUserImageView)
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val list = lists[position]
            val listName = list["Nombre"] as String
            val products = list["PorComprar"] as List<String>
            val users = list["Usuarios"] as List<String>

            holder.listNameTextView.text = listName
            holder.productCountTextView.text = "${products.size} productos"

            for (i in holder.userImageViews.indices) {
                if (i < users.size && i < holder.userImageViews.size - 1) {
                    // Aquí podrías cargar imágenes de usuarios reales si las tienes
                    // usando Glide, Picasso, etc. De momento usamos un placeholder
                    holder.userImageViews[i].visibility = View.VISIBLE
                } else if (i == holder.userImageViews.size - 1) {
                    holder.userImageViews[i].visibility = View.VISIBLE
                } else {
                    holder.userImageViews[i].visibility = View.GONE
                }
            }
        }

        override fun getItemCount(): Int {
            return lists.size
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.setLanguage(this)
        Utils.setTheme(this)
        setContentView(R.layout.activity_home)

        // Setup
        setup()
    }

    private fun setup() {
        val lists: MutableList<Map<String, Any>> = mutableListOf()

        db.collection("Usuarios")
            .whereEqualTo("Email", FirebaseAuth.getInstance().currentUser?.email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val tasks = mutableListOf<Task<DocumentSnapshot>>()

                for (document in querySnapshot) {
                    val userLists = document.get("Listas") as? List<String> ?: emptyList()
                    for (listId in userLists) {
                        val task = db.collection("Listas").document(listId).get()
                            .addOnSuccessListener { listDocument ->
                                listDocument.data?.let { listData ->
                                    lists.add(listData)
                                    val listName = listData["Nombre"] as? String
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error getting list document", e)
                            }
                        tasks.add(task)
                    }
                }

                Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
                    .addOnSuccessListener { snapshots ->
                        showListInfo(lists)
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting all list documents: ", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting user documents: ", exception)
            }
    }

    private fun showListInfo(lists: List<Map<String, Any>>) {
        val listInfoStringBuilder = StringBuilder()
        for (list in lists) {
            val listName = list["Nombre"] as String
            val products = (list["Comprados"] as? List<*>)?.size ?: 0
            val users = list["Usuarios"] as List<*>

            listInfoStringBuilder.append("Nombre de la lista: $listName\n")
            listInfoStringBuilder.append("Número de productos comprados: $products\n")
            listInfoStringBuilder.append("Usuarios en la lista:\n")
            for (user in users) {
                listInfoStringBuilder.append("$user\n")
            }
            listInfoStringBuilder.append("\n")
        }
        val listInfoTextView = findViewById<TextView>(R.id.listInfoTextView)
        listInfoTextView.text = listInfoStringBuilder.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
                // return true
                true
            }
            R.id.perfil -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                finish()
                // return true
                true
            }
            R.id.cerrar_sesion -> {
                // Borrar datos de sesión
                Utils.limpiarPreferencias(this)

                // Cerrar sesión
                FirebaseAuth.getInstance().signOut()

                // Navegar hacia atrás
                onBackPressed()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showNewListDialog(view: View) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_list, null)
        val listNameEditText = dialogView.findViewById<EditText>(R.id.listNameEditText)
        val emailsEditText = dialogView.findViewById<EditText>(R.id.emailsEditText)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Crear Nueva Lista")
            .setPositiveButton("Crear") { dialog, _ ->
                val listName = listNameEditText.text.toString().trim()
                val emails = emailsEditText.text.toString().trim()

                if (listName.isNotEmpty()) {
                    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return@setPositiveButton

                    // Crear el nuevo documento en la colección "Listas"
                    val users = if (emails.isNotEmpty()) {
                        emails.split(",").map { it.trim() } + currentUserEmail
                    } else {
                        listOf(currentUserEmail)
                    }

                    val newList = hashMapOf(
                        "Nombre" to listName,
                        "Comprados" to emptyList<String>(),
                        "PorComprar" to emptyList<String>(),
                        "Usuarios" to users
                    )

                    db.collection("Listas")
                        .add(newList)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")

                            val updateUserLists = users.map { email ->
                                db.collection("Usuarios")
                                    .whereEqualTo("Email", email)
                                    .get()
                                    .continueWithTask { querySnapshot ->
                                        if (!querySnapshot.result.isEmpty) {
                                            val userDoc = querySnapshot.result.documents[0]
                                            db.collection("Usuarios").document(userDoc.id)
                                                .update("Listas", FieldValue.arrayUnion(documentReference.id))
                                        } else {
                                            val newUser = hashMapOf(
                                                "Email" to email,
                                                "Foto" to "",
                                                "Listas" to listOf(documentReference.id),
                                                "Token" to ""
                                            )
                                            db.collection("Usuarios").add(newUser)
                                        }
                                    }
                            }

                            Tasks.whenAll(updateUserLists)
                                .addOnSuccessListener {
                                    // Iniciar una nueva instancia de HomeActivity después de que todos los usuarios se hayan actualizado
                                    val intent = Intent(this, HomeActivity::class.java)
                                    startActivity(intent)
                                    // Finalizar la actividad actual
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error updating user documents", e)
                                    // Manejar el error, por ejemplo, mostrando un mensaje al usuario
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                            // Manejar el error, por ejemplo, mostrando un mensaje al usuario
                        }
                } else {
                    Toast.makeText(this, "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }
}
