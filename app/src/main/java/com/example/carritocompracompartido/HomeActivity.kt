package com.example.carritocompracompartido

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.QuerySnapshot
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
                                    // Añade el ID del documento a los datos de la lista
                                    val dataWithId = listData.toMutableMap()
                                    dataWithId["id"] = listDocument.id
                                    lists.add(dataWithId)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error getting list document", e)
                            }
                        tasks.add(task)
                    }
                }

                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        showListInfo(lists)
                    }
                    .addOnFailureListener { exception ->
                        Log.w("Firestore", "Error getting all list documents: ", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting user documents: ", exception)
            }

        findViewById<FloatingActionButton>(R.id.addListFab).setOnClickListener {
            showNewListDialog()
        }
    }

    private fun showListInfo(lists: List<Map<String, Any>>) {
        val recyclerView = findViewById<RecyclerView>(R.id.listsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ListsAdapter(lists)
    }

    private inner class ListsAdapter(private val lists: List<Map<String, Any>>) : RecyclerView.Adapter<ListsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val listNameTextView: TextView = itemView.findViewById(R.id.listNameTextView)
            val productCountTextView: TextView = itemView.findViewById(R.id.productCountTextView)
            val userImageLayout: LinearLayout = itemView.findViewById(R.id.userImageLayout)
            val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val list = lists[position]
            val listName = list["Nombre"] as? String ?: getString(R.string.no_name)
            val products = list["PorComprar"] as? List<String> ?: emptyList()
            val users = list["Usuarios"] as? List<String> ?: emptyList()

            holder.listNameTextView.text = listName
            holder.productCountTextView.text = "${products.size} ${if (products.size == 1) getString(R.string.product) else getString(R.string.products)}"

            holder.userImageLayout.removeAllViews() // Limpiar las vistas de usuario antes de añadir nuevas

            val maxUsersToShow = 3
            for (i in users.indices) {
                if (i < maxUsersToShow) {
                    val imageView = ImageView(holder.userImageLayout.context)
                    imageView.layoutParams = LinearLayout.LayoutParams(32.dpToPx(), 32.dpToPx()).apply {
                        marginEnd = 4.dpToPx()
                    }

                    // Obtener la foto del usuario de Firestore
                    db.collection("Usuarios").document(users[i]).get()
                        .addOnSuccessListener { document ->
                            val imageString = document.getString("Foto")
                            if (!imageString.isNullOrEmpty()) {
                                val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                imageView.setImageBitmap(bitmap)
                            } else {
                                imageView.setImageResource(R.drawable.ic_user_placeholder)
                            }
                        }
                        .addOnFailureListener {
                            imageView.setImageResource(R.drawable.ic_user_placeholder)
                        }

                    holder.userImageLayout.addView(imageView)
                } else if (i == maxUsersToShow) {
                    val imageView = ImageView(holder.userImageLayout.context)
                    imageView.layoutParams = LinearLayout.LayoutParams(32.dpToPx(), 32.dpToPx()).apply {
                        marginEnd = 4.dpToPx()
                    }
                    imageView.setImageResource(R.drawable.ic_add) // Usar un ícono con un símbolo "+"
                    holder.userImageLayout.addView(imageView)
                    break
                }
            }

            holder.editButton.setOnClickListener {
                showEditDialog(list["id"] as String, listName, users)
            }

            holder.itemView.setOnClickListener {
                val listId = list["id"] as? String
                if (listId != null) {
                    val intent = Intent(holder.itemView.context, ListDetailActivity::class.java)
                    intent.putExtra("listId", listId)
                    intent.putExtra("listTitle", listName)
                    holder.itemView.context.startActivity(intent)
                } else {
                    Log.w("Firestore", "List ID is null or invalid")
                }
            }
        }

        override fun getItemCount(): Int {
            return lists.size
        }
    }

    private fun showEditDialog(listId: String, currentName: String, currentUsers: List<String>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_list, null)
        val listNameEditText = dialogView.findViewById<EditText>(R.id.list_name_edit_text)
        val emailsEditText = dialogView.findViewById<EditText>(R.id.emails_edit_text)
        val currentUsersLayout = dialogView.findViewById<LinearLayout>(R.id.current_users_layout)
        val leaveListButton = dialogView.findViewById<Button>(R.id.leave_list_button)

        listNameEditText.setText(currentName)

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        // Mostrar usuarios actuales
        for (user in currentUsers) {
            val userTextView = TextView(this)
            userTextView.text = user
            currentUsersLayout.addView(userTextView)
        }

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(R.string.edit_list_title)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val listName = listNameEditText.text.toString().trim()
                val newEmails = emailsEditText.text.toString().trim()

                if (listName.isNotEmpty()) {
                    val newUsers = if (newEmails.isNotEmpty()) {
                        newEmails.split(",").map { it.trim() }
                    } else {
                        emptyList()
                    }

                    // Verificar si los nuevos usuarios existen
                    val validEmails = mutableListOf<String>()
                    val invalidEmails = mutableListOf<String>()
                    val tasks = mutableListOf<Task<QuerySnapshot>>()

                    for (email in newUsers) {
                        val task = db.collection("Usuarios").whereEqualTo("Email", email).get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    validEmails.add(email)
                                } else {
                                    invalidEmails.add(email)
                                }
                            }
                        tasks.add(task)
                    }

                    Tasks.whenAllComplete(tasks)
                        .addOnSuccessListener {
                            if (invalidEmails.isNotEmpty()) {
                                val message = if (invalidEmails.size == 1) {
                                    getString(R.string.email_not_exist, invalidEmails[0])
                                } else {
                                    getString(R.string.emails_not_exist, invalidEmails.joinToString(", "))
                                }
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            }

                            db.collection("Listas").document(listId)
                                .update("Nombre", listName, "Usuarios", currentUsers + validEmails)
                                .addOnSuccessListener {
                                    // Actualizar las listas de los nuevos usuarios
                                    val updateUserLists = validEmails.map { email ->
                                        db.collection("Usuarios").whereEqualTo("Email", email).get()
                                            .continueWithTask { querySnapshot ->
                                                if (!querySnapshot.result.isEmpty) {
                                                    val userDoc = querySnapshot.result.documents[0]
                                                    db.collection("Usuarios").document(userDoc.id)
                                                        .update("Listas", FieldValue.arrayUnion(listId))
                                                } else {
                                                    Tasks.forResult(null)
                                                }
                                            }
                                    }

                                    Tasks.whenAllComplete(updateUserLists)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, R.string.list_updated, Toast.LENGTH_SHORT).show()
                                            finish()
                                            startActivity(intent)
                                            // Obtener tokens de los usuarios y enviar notificación
                                            obtenerYEnviarTokens(validEmails, getString(R.string.list_updated), "¡La lista $listName ha sido actualizada!")
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, R.string.error_updating_list, Toast.LENGTH_SHORT).show()
                                            Log.w("Firestore", "Error updating user documents", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, R.string.error_updating_list, Toast.LENGTH_SHORT).show()
                                    Log.w("Firestore", "Error updating document", e)
                                }
                        }
                } else {
                    Toast.makeText(this, R.string.list_name_empty, Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()

        leaveListButton.setOnClickListener {
            val updatedUsers = currentUsers.toMutableList()
            currentUserEmail?.let {
                updatedUsers.remove(it)
            }

            if (updatedUsers.isEmpty()) {
                db.collection("Listas").document(listId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.list_deleted, Toast.LENGTH_SHORT).show()
                        finish()
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, R.string.error_deleting_list, Toast.LENGTH_SHORT).show()
                        Log.w("Firestore", "Error deleting document", e)
                    }
            } else {
                db.collection("Listas").document(listId)
                    .update("Usuarios", updatedUsers)
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.list_updated, Toast.LENGTH_SHORT).show()
                        finish()
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, R.string.error_updating_list, Toast.LENGTH_SHORT).show()
                        Log.w("Firestore", "Error updating document", e)
                    }
            }

            // Eliminar la lista del array de listas del usuario
            currentUserEmail?.let {
                db.collection("Usuarios").whereEqualTo("Email", it).get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            db.collection("Usuarios").document(document.id)
                                .update("Listas", FieldValue.arrayRemove(listId))
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error removing list from user document", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("Firestore", "Error getting user documents", e)
                    }
            }

            dialog.dismiss()
        }
    }

    private fun showNewListDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_list, null)
        val listNameEditText = dialogView.findViewById<EditText>(R.id.listNameEditText)
        val emailsEditText = dialogView.findViewById<EditText>(R.id.emailsEditText)
        val addListButton = dialogView.findViewById<Button>(R.id.addListButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(R.string.add_list)

        val dialog = builder.create()

        addListButton.setOnClickListener {
            val listName = listNameEditText.text.toString().trim()
            val emails = emailsEditText.text.toString().trim()

            if (listName.isNotEmpty()) {
                val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return@setOnClickListener

                // Crear el nuevo documento en la colección "Listas"
                val users = if (emails.isNotEmpty()) {
                    emails.split(",").map { it.trim() } + currentUserEmail
                } else {
                    listOf(currentUserEmail)
                }

                // Verificar si los usuarios existen
                val validEmails = mutableListOf<String>()
                val invalidEmails = mutableListOf<String>()
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (email in users) {
                    val task = db.collection("Usuarios").whereEqualTo("Email", email).get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                validEmails.add(email)
                            } else {
                                invalidEmails.add(email)
                            }
                        }
                    tasks.add(task)
                }

                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        if (invalidEmails.isNotEmpty()) {
                            val message = if (invalidEmails.size == 1) {
                                getString(R.string.email_not_exist, invalidEmails[0])
                            } else {
                                getString(R.string.emails_not_exist, invalidEmails.joinToString(", "))
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }

                        val newList = hashMapOf(
                            "Nombre" to listName,
                            "Comprados" to emptyList<String>(),
                            "PorComprar" to emptyList<String>(),
                            "Usuarios" to validEmails
                        )

                        db.collection("Listas")
                            .add(newList)
                            .addOnSuccessListener { documentReference ->
                                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")

                                val updateUserLists = validEmails.map { email ->
                                    db.collection("Usuarios").whereEqualTo("Email", email).get()
                                        .continueWithTask { querySnapshot ->
                                            if (!querySnapshot.result.isEmpty) {
                                                val userDoc = querySnapshot.result.documents[0]
                                                db.collection("Usuarios").document(userDoc.id)
                                                    .update("Listas", FieldValue.arrayUnion(documentReference.id))
                                            } else {
                                                Tasks.forResult(null)
                                            }
                                        }
                                }

                                Tasks.whenAllComplete(updateUserLists)
                                    .addOnSuccessListener {
                                        // Iniciar una nueva instancia de HomeActivity después de que todos los usuarios se hayan actualizado
                                        val intent = Intent(this, HomeActivity::class.java)
                                        startActivity(intent)
                                        // Finalizar la actividad actual
                                        finish()

                                        // Obtener tokens de los usuarios y enviar notificación
                                        obtenerYEnviarTokens(validEmails, "¡Se te ha añadido a una lista!", "Has sido añadido a la lista: $listName")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("Firestore", "Error updating user documents", e)
                                        // Manejar el error, por ejemplo, mostrando un mensaje al usuario
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error adding document", e)
                                // Manejar el error, por ejemplo, mostrando un mensaje al usuario
                            }
                    }
            } else {
                Toast.makeText(this, R.string.list_name_empty, Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // Ajustar el tamaño del diálogo
        dialog.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.85).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            R.id.perfil -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            R.id.cerrar_sesion -> {
                val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
                prefs.clear()
                prefs.apply()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Función para enviar notificaciones
    private fun enviarNotificacion(tokens: List<String>, title: String, body: String) {
        Log.d("Notificación", "Enviando notificación")
        val url = "http://34.121.128.202:81/notificarUsuariosCarrito.php"
        val client = OkHttpClient()
        val json = JSONObject()
        json.put("tokens", JSONArray(tokens)) // Asegurarse de que tokens sea un JSONArray
        json.put("title", title)
        json.put("body", body)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, json.toString())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Notificación", "Error al enviar notificación", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("Notificación", "Notificación enviada correctamente")
                    Log.d("Notificación", "Respuesta del servidor: $responseBody")
                } else {
                    val responseBody = response.body?.string()
                    Log.e("Notificación", "Error al enviar notificación: ${response.code}")
                    Log.e("Notificación", "Respuesta del servidor: $responseBody")
                }
            }
        })
    }

    // Función para obtener tokens y enviar notificación
    private fun obtenerYEnviarTokens(emails: List<String>, title: String, body: String) {
        val tokenTasks = emails.map { email ->
            db.collection("Usuarios").whereEqualTo("Email", email).get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(tokenTasks).addOnSuccessListener { snapshots ->
            val tokens = snapshots.flatMap { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.getString("Token")
                }
            }
            enviarNotificacion(tokens, title, body)
        }.addOnFailureListener { e ->
            Log.e("Notificación", "Error al obtener tokens", e)
        }
    }
}

// Extensión para convertir dp a px
fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}
