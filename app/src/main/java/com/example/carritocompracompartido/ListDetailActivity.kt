package com.example.carritocompracompartido

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
            showQuantityDialog(producto) { quantity ->
                compradosAdapter.addProducto(mapOf("nombre" to producto["nombre"].toString(), "cantidad" to quantity))
                porComprarAdapter.removeProducto(producto)
            }
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

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_product))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                val productName = productNameEditText.text.toString().trim().lowercase()
                if (productName.isNotEmpty()) {
                    val allProducts = porComprarAdapter.getProductos() + compradosAdapter.getProductos()
                    val existingProduct = allProducts.find {
                        it["nombre"]?.toString()?.equals(productName, ignoreCase = true) == true
                    }
                    if (existingProduct != null) {
                        Toast.makeText(this, getString(R.string.product_exists), Toast.LENGTH_SHORT).show()
                    } else {
                        val newProduct = mapOf("nombre" to productName, "cantidad" to 1)
                        porComprarAdapter.addProducto(newProduct)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.positive_button))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.negative_button))
    }

    private fun showQuantityDialog(producto: Map<String, Any>, onQuantityEntered: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)

        var dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.enter_quantity))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                val quantity = quantityEditText.text.toString().toIntOrNull()
                if (quantity != null && quantity > 0) {
                    onQuantityEntered(quantity)
                } else {
                    Toast.makeText(this, getString(R.string.invalid_quantity), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.positive_button))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.negative_button))
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

                val newList = mapOf("nombre" to dragData, "cantidad" to 1)

                if (view.id == R.id.recyclerComprados) {
                    porComprarAdapter.removeProducto(newList)
                    showQuantityDialog(newList) { quantity ->
                        compradosAdapter.addProducto(mapOf("nombre" to newList["nombre"].toString(), "cantidad" to quantity))
                    }
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
                    val comprados = document["Comprados"] as? List<Map<String, Any>> ?: emptyList()
                    val porComprar = document["PorComprar"] as? List<Map<String, Any>> ?: emptyList()

                    compradosAdapter.setProductos(comprados)
                    porComprarAdapter.setProductos(porComprar)
                }
            }
            .addOnFailureListener { e ->
                // Manejo de error
            }
    }

    private fun updateFirestore() {
        listId?.let { listId ->
            val comprados = compradosAdapter.getProductos().map { mapOf("nombre" to it["nombre"], "cantidad" to it["cantidad"]) }
            val porComprar = porComprarAdapter.getProductos().map { mapOf("nombre" to it["nombre"], "cantidad" to it["cantidad"]) }

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
        Utils.loadProfileImage(this, menu)
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
