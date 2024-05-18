package com.example.carritocompracompartido

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductosAdapter(
    private val isComprados: Boolean,
    private val onItemClicked: (Map<String, String>) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ProductoViewHolder>() {

    private var productos: MutableList<Map<String, String>> = mutableListOf()

    fun setProductos(productos: List<Map<String, String>>) {
        this.productos = productos.toMutableList()
        notifyDataSetChanged()
    }

    fun addProducto(producto: Map<String, String>) {
        productos.add(producto)
        notifyItemInserted(productos.size - 1)
    }

    fun removeProducto(producto: Map<String, String>) {
        val index = productos.indexOf(producto)
        if (index != -1) {
            productos.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getProductos(): List<Map<String, String>> {
        return productos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.productoNombre.text = producto["nombre"] ?: ""

        if (isComprados) {
            holder.productoNombre.setBackgroundResource(R.drawable.product_background_comprados)
        } else {
            holder.productoNombre.setBackgroundResource(R.drawable.product_background_porcomprar)
        }

        holder.itemView.setOnClickListener {
            onItemClicked(producto)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productoNombre: TextView = itemView.findViewById(R.id.productoNombre)
    }
}

