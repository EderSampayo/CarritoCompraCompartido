package com.example.carritocompracompartido

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class ProfileActivity : AppCompatActivity() {
    private var userIsInteracting = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.setLanguage(this)
        Utils.setTheme(this)
        setContentView(R.layout.activity_profile)
        setup()
    }

    private fun setup() {
        setupSpinner(R.id.spinner_theme, R.array.themes, "theme")
        setupSpinner(R.id.spinner_language, R.array.languages, "language")
    }
    //setupSpinner(R.id.spinner_theme, R.array.themes, preferences)

    private fun setupSpinner(spinner_id: Int, options_array_id: Int, prefsOption: String) {
        val spinner_theme: Spinner = findViewById(spinner_id)
        ArrayAdapter.createFromResource(
            this,
            options_array_id,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_theme.adapter = adapter
        }

        spinner_theme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Código que se ejecuta cuando se selecciona un ítem
                val selectedItem = parent.getItemAtPosition(position).toString()
                if (userIsInteracting) {
                    val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString(prefsOption, selectedItem)
                    editor.apply()  // or editor.commit() if you need an immediate return status
                    //Toast.makeText(this@ProfileActivity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({recreateActivity()}, 100)
                }
                userIsInteracting = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to execute when nothing is selected
                // This method is required but can be left empty if there's no specific action needed
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        userIsInteracting = true
    }

    private fun recreateActivity() {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.perfil -> {
                // Crear un Intent para iniciar la actividad del perfil
                val profileIntent = Intent(this, ProfileActivity::class.java)
                startActivity(profileIntent)
                true
            }
            R.id.cerrar_sesion -> {
                // Borrado de datos (sesión)
                val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
                prefs.clear()
                prefs.apply()

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
