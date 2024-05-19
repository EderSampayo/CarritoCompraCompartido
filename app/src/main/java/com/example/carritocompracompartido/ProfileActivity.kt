package com.example.carritocompracompartido

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileActivity : AppCompatActivity() {
    private var userIsInteracting = false
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var db: FirebaseFirestore
    private lateinit var profileImageView: ImageView
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.setLanguage(this)
        Utils.setTheme(this)
        setContentView(R.layout.activity_profile)
        db = FirebaseFirestore.getInstance()
        profileImageView = findViewById(R.id.profileImageView)
        setup()
        loadProfileImage()

        findViewById<Button>(R.id.btn_change_profile_image).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
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

    private fun loadProfileImage() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        currentUserEmail?.let {
            db.collection("Usuarios").whereEqualTo("Email", it).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val imageString = document.getString("Foto")
                        if (!imageString.isNullOrEmpty()) {
                            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profileImageView.setImageBitmap(bitmap)
                            menu?.let { Utils.loadProfileImage(this, it) }
                        }
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let {
                val imageStream: InputStream? = contentResolver.openInputStream(it)
                val selectedImage = BitmapFactory.decodeStream(imageStream)
                profileImageView.setImageBitmap(selectedImage)
                saveImageToFirestore(selectedImage)
            }
        }
    }

    private fun saveImageToFirestore(bitmap: Bitmap) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        val imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        db.collection("Usuarios").whereEqualTo("Email", currentUserEmail).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("Usuarios").document(document.id)
                        .update("Foto", imageString)
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.profile_image_updated), Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, getString(R.string.profile_image_update_failed), Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        this.menu = menu
        Utils.loadProfileImage(this, menu) // Cargar la imagen de perfil en el menú cuando se crea
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
                // Ya estamos en el perfil, no hacer nada
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
