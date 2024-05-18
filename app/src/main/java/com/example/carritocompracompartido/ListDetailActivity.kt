package com.example.carritocompracompartido

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var listId: String
    private lateinit var listNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_detail)

        listNameTextView = findViewById(R.id.listNameTextView)
        listId = intent.getStringExtra("listId") ?: return

        setup()
    }

    private fun setup() {
        db.collection("Listas").document(listId).get()
            .addOnSuccessListener { document ->
                val listName = document.getString("Nombre")
                val boughtProducts = document.get("Comprados") as? List<String> ?: emptyList()
                val toBuyProducts = document.get("PorComprar") as? List<String> ?: emptyList()

                listNameTextView.text = listName

                val boughtRecyclerView = findViewById<RecyclerView>(R.id.boughtProductsRecyclerView)
                val toBuyRecyclerView = findViewById<RecyclerView>(R.id.toBuyProductsRecyclerView)

                boughtRecyclerView.layoutManager = GridLayoutManager(this, 2)
                toBuyRecyclerView.layoutManager = GridLayoutManager(this, 2)

                boughtRecyclerView.adapter = ProductsAdapter(boughtProducts)
                toBuyRecyclerView.adapter = ProductsAdapter(toBuyProducts)
            }
    }

    private inner class ProductsAdapter(private val products: List<String>) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val product = products[position]
            holder.productNameTextView.text = product
        }

        override fun getItemCount(): Int {
            return products.size
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                finish() // Cierra esta actividad y vuelve a la anterior
                true
            }
            R.id.perfil -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
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
}
