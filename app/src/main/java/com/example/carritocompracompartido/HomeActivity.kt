package com.example.carritocompracompartido

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import com.google.firebase.auth.FirebaseAuth

enum class ProviderType {
    BASIC,
    GOOGLE
}

class HomeActivity : AppCompatActivity() {
    // Declarar elementos de la UI como atributos de la clase
    private lateinit var emailTextView: TextView
    private lateinit var providerTextView: TextView
    private lateinit var logOutButton: Button
    private lateinit var email: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Elementos del layout XML
        emailTextView = findViewById(R.id.emailTextView)
        providerTextView = findViewById(R.id.providerTextView)
        logOutButton = findViewById(R.id.logOutButton)

        // Setup
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")

        // Guardado de datos (sesión)
        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()   // Asegurarnos de que se guarden los datos en la app
    }

    private fun setup(email: String, provider: String) {
        title = "Inicio"
        emailTextView.text = email
        this.email = email
        providerTextView.text = provider

        logOutButton.setOnClickListener {
            // Borrado de datos (sesión)
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            /*R.id.perfil -> {
                // Crear un Intent para iniciar la actividad del perfil
                val profileIntent = Intent(this, ProfileActivity::class.java)
                startActivity(profileIntent)
                true
            }*/
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