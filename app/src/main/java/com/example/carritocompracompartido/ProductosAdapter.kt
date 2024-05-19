package com.example.carritocompracompartido

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ProductosAdapter(
    private val isComprados: Boolean,
    private val onItemClicked: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ProductoViewHolder>() {

    private var productos: MutableList<Map<String, Any>> = mutableListOf()

    fun setProductos(productos: List<Map<String, Any>>) {
        this.productos = productos.toMutableList()
        notifyDataSetChanged()
    }

    fun addProducto(producto: Map<String, Any>) {
        productos.add(producto)
        notifyItemInserted(productos.size - 1)
    }

    fun removeProducto(producto: Map<String, Any>) {
        val index = productos.indexOfFirst { it["nombre"]?.toString()?.equals(producto["nombre"]?.toString(), ignoreCase = true) == true }
        if (index != -1) {
            productos.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getProductos(): List<Map<String, Any>> = productos

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        val nombre = producto["nombre"] as String
        val cantidad = (producto["cantidad"] as? Long)?.toInt() ?: producto["cantidad"] as Int

        holder.productoNombre.text = "$nombre ($cantidad)"

        if (isComprados) {
            holder.productoNombre.setBackgroundResource(R.drawable.product_background_comprados)
        } else {
            holder.productoNombre.setBackgroundResource(R.drawable.product_background_porcomprar)
        }

        holder.itemView.setOnClickListener {
            onItemClicked(producto)
        }

        holder.itemView.setOnLongClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, producto)
            true
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }

    private fun showDeleteConfirmationDialog(context: android.content.Context, producto: Map<String, Any>) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.delete_product)
            .setMessage(R.string.confirm_delete_product)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                removeProducto(producto)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.positive_button))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.negative_button))
    }

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productoNombre: TextView = itemView.findViewById(R.id.productoNombre)
    }
}
