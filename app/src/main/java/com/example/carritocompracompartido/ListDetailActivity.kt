package com.example.carritocompracompartido

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerComprados: RecyclerView
    private lateinit var recyclerPorComprar: RecyclerView
    private lateinit var compradosAdapter: ProductosAdapter
    private lateinit var porComprarAdapter: ProductosAdapter

    private var listId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_detail)

        db = FirebaseFirestore.getInstance()

        listId = intent.getStringExtra("listId")
        val listTitle = intent.getStringExtra("listTitle")
        findViewById<TextView>(R.id.listTitle).text = listTitle

        recyclerComprados = findViewById(R.id.recyclerComprados)
        recyclerPorComprar = findViewById(R.id.recyclerPorComprar)

        recyclerComprados.layoutManager = GridLayoutManager(this, 2)
        recyclerPorComprar.layoutManager = GridLayoutManager(this, 2)

        compradosAdapter = ProductosAdapter(true) { producto ->
            porComprarAdapter.addProducto(producto)
            compradosAdapter.removeProducto(producto)
        }
        porComprarAdapter = ProductosAdapter(false) { producto ->
            compradosAdapter.addProducto(producto)
            porComprarAdapter.removeProducto(producto)
        }

        recyclerComprados.adapter = compradosAdapter
        recyclerPorComprar.adapter = porComprarAdapter

        listId?.let { loadListDetails(it) }

        setupDragAndDrop()
        setupAddProductFab()
    }

    private fun setupAddProductFab() {
        val addProductFab: FloatingActionButton = findViewById(R.id.addProductFab)
        addProductFab.setOnClickListener {
            showAddProductDialog()
        }
    }

    private fun showAddProductDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
        val productNameEditText = dialogView.findViewById<EditText>(R.id.productNameEditText)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_product))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                val productName = productNameEditText.text.toString().trim()
                if (productName.isNotEmpty()) {
                    val newProduct = mapOf("nombre" to productName)
                    porComprarAdapter.addProducto(newProduct)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun setupDragAndDrop() {
        recyclerComprados.setOnDragListener(dragListener)
        recyclerPorComprar.setOnDragListener(dragListener)
    }

    private val dragListener = View.OnDragListener { view, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> event.clipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ?: false
            DragEvent.ACTION_DRAG_ENTERED -> {
                view.setBackgroundColor(Color.LTGRAY)
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> true
            DragEvent.ACTION_DRAG_EXITED -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                true
            }
            DragEvent.ACTION_DROP -> {
                val item = event.clipData.getItemAt(0)
                val dragData = item.text.toString()
                view.setBackgroundColor(Color.TRANSPARENT)

                val newList = mapOf("nombre" to dragData)

                if (view.id == R.id.recyclerComprados) {
                    porComprarAdapter.removeProducto(newList)
                    compradosAdapter.addProducto(newList)
                } else if (view.id == R.id.recyclerPorComprar) {
                    compradosAdapter.removeProducto(newList)
                    porComprarAdapter.addProducto(newList)
                }

                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                event.result
            }
            else -> false
        }
    }

    private fun loadListDetails(listId: String) {
        db.collection("Listas").document(listId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val comprados = document["Comprados"] as? List<String> ?: emptyList()
                    val porComprar = document["PorComprar"] as? List<String> ?: emptyList()

                    val compradosList = comprados.map { mapOf("nombre" to it) }
                    val porComprarList = porComprar.map { mapOf("nombre" to it) }

                    compradosAdapter.setProductos(compradosList)
                    porComprarAdapter.setProductos(porComprarList)
                }
            }
            .addOnFailureListener { e ->
                // Manejo de error
            }
    }

    private fun updateFirestore() {
        listId?.let { listId ->
            val comprados = compradosAdapter.getProductos().map { it["nombre"].toString() }
            val porComprar = porComprarAdapter.getProductos().map { it["nombre"].toString() }

            db.collection("Listas").document(listId)
                .update("Comprados", comprados, "PorComprar", porComprar)
                .addOnSuccessListener {
                    // Éxito en la actualización
                }
                .addOnFailureListener { e ->
                    // Manejo de error
                }
        }
    }

    override fun onPause() {
        super.onPause()
        updateFirestore()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateFirestore()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                true
            }
            R.id.perfil -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                finish()
                true
            }
            R.id.cerrar_sesion -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        // Borrar datos de sesión
        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()

        // Cerrar sesión
        FirebaseAuth.getInstance().signOut()

        // Navegar a la pantalla de autenticación
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
