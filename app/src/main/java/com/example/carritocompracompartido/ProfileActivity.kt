package com.example.carritocompracompartido

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

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
        setupSpinner(R.id.spinner_language, R.array.languages, "locale")
        setupLogoutButton()
    }

    private fun setupSpinner(spinnerId: Int, optionsArrayId: Int, prefsOption: String) {
        val spinner: Spinner = findViewById(spinnerId)
        val adapter = ArrayAdapter.createFromResource(
            this,
            optionsArrayId,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set initial selection
        val prefsValue = Utils.obtenerPreferencia(this, prefsOption)
        val position = if (prefsOption == "locale") {
            when (prefsValue) {
                "es" -> 0
                "eu" -> 1
                "en" -> 2
                else -> 2
            }
        } else {
            adapter.getPosition(prefsValue)
        }
        spinner.setSelection(position)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (userIsInteracting) {
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    Utils.ponerPreferencia(this@ProfileActivity, prefsOption, selectedItem)
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({ recreate() }, 100)
                }
                userIsInteracting = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupLogoutButton() {
        val btnLogout: Button = findViewById(R.id.btn_logout)
        btnLogout.setOnClickListener {
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        userIsInteracting = true
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
}
