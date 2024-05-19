package com.example.carritocompracompartido

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.util.Util
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

/** Auth con Email extraído de MoureDev
Vídeo: https://www.youtube.com/watch?v=dpURgJ4HkMk&t=25s
Autor: https://www.youtube.com/@mouredev
 **/
class AuthActivity : AppCompatActivity() {
    private val GOOGLE_SIGN_IN = 100
    private lateinit var auth: FirebaseAuth
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    // Declarar elementos de la UI como atributos de la clase
    private lateinit var signUpButton: Button
    private lateinit var loginButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var authLayout: View
    private lateinit var googleButton: Button
    private lateinit var tokenFCM: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.setLanguage(this)
        Utils.setTheme(this)
        setContentView(R.layout.activity_auth)

        // Elementos del layout XML
        signUpButton = findViewById(R.id.signUpButton)
        loginButton = findViewById(R.id.loginButton)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        authLayout = findViewById(R.id.authLayout)
        googleButton = findViewById(R.id.googleButton)

        if (checkGooglePlayServices()) {
            setup()
        } else {
            Toast.makeText(this, "Google Play Services required", Toast.LENGTH_LONG).show()
        }

        // Obtener permisos
        obtenerPermisos()
        // Obtener el token de FCM con llamada a session() como callback
        obtenerTokenFCM {
            session() // Llamar a session() solo después de que tokenFCM haya sido inicializado
        }

        // Setup
        setup()
    }

    override fun onStart() {
        super.onStart()

        authLayout.visibility = View.VISIBLE
    }

    private fun session() {
        val email = Utils.obtenerPreferencia(this, "email")
        val provider = Utils.obtenerPreferencia(this, "provider")

        if (email != null && provider != null) {
            actualizarTokenFirestore(email, this.tokenFCM)  // Por si acaso
            authLayout.visibility = View.INVISIBLE
            navigateToHome(email, ProviderType.valueOf(provider))
        }
    }

    private fun setup() {
        title = "Autenticación"

        signUpButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener {
                        if (it.isSuccessful) {
                            registrarDatosFirestore(emailEditText.text.toString(), this.tokenFCM)
                            Utils.ponerPreferencia(this, "email", emailEditText.text.toString())
                            Utils.ponerPreferencia(this, "provider", ProviderType.BASIC.toString())
                            navigateToHome(it.result?.user?.email ?: "", ProviderType.BASIC)    // Si no existe el email, se manda un string vacío, aunque nunca debería pasar por la comprobación de antes
                        } else {
                            showAlert("Error", "Se ha producido un error en la autenticación del usuario")
                        }
                    }
            }
        }

        loginButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(emailEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener {
                        if (it.isSuccessful) {
                            actualizarTokenFirestore(emailEditText.text.toString(), this.tokenFCM)
                            Utils.ponerPreferencia(this, "email", emailEditText.text.toString())
                            Utils.ponerPreferencia(this, "provider", ProviderType.BASIC.toString())
                            navigateToHome(it.result?.user?.email ?: "", ProviderType.BASIC)    // Si no existe el email, se manda un string vacío, aunque nunca debería pasar por la comprobación de antes
                        } else {
                            showAlert("Error", "Se ha producido un error en la autenticación del usuario")
                        }
                    }
            }
        }

        googleButton.setOnClickListener {
            // Configuracion

            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()  // Para que no se quede la sesión iniciada, por si tenemos más de una cuenta de Google

            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }
    }

    private fun showAlert(titulo: String, texto: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(titulo)
        builder.setMessage(texto)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun navigateToHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply{
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                showAlert("Error", "Se ha producido un error en la autenticación del usuario")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val email = account.email ?: ""
                val token = this.tokenFCM

                verificarUsuarioFirestore(email) { exists ->
                    if (exists) {
                        // Si el usuario ya existe, solo actualizamos el token (por si se ha desinstalado la app o algo relacionado)
                        actualizarTokenFirestore(email, token)
                    } else {
                        // Si el usuario no existe, lo registramos
                        registrarDatosFirestore(email, token)
                    }
                    navigateToHome(email, ProviderType.GOOGLE)
                }
            } else {
                showAlert("Error", "Firebase Authentication failed: ${task.exception?.message}")
            }
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
            } else {
                Toast.makeText(this, "This device is not supported.", Toast.LENGTH_LONG).show()
                finish()
            }
            false
        } else {
            true
        }
    }

    private fun obtenerTokenFCM(callback: () -> Unit) {
        // Obtener el token de FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                this.tokenFCM = task.result
                Log.d("FCM Token", this.tokenFCM ?: "No se ha podido obtener el token")

                // Llamar al callback una vez que se obtiene el token
                callback()
            } else {
                // Fallo al obtener el token
                Toast.makeText(this, "Error al obtener el token.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                // PEDIR EL PERMISO
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
            }
        }
    }

    // Para el caso de registro (con Email y Google)
    private fun registrarDatosFirestore(email: String, tokenFCM: String) {
        val db = FirebaseFirestore.getInstance()
        val usuario = hashMapOf(
            "Email" to email,
            "Token" to tokenFCM
        )

        db.collection("Usuarios").document(email)
            .set(usuario)
            .addOnSuccessListener {
                Log.d("Firestore", "Usuario registrado correctamente!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al registrar usuario", e)
            }
    }

    // Para el caso de login (con Email y Google)
    private fun actualizarTokenFirestore(email: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val usuarioRef = db.collection("Usuarios").document(email)

        usuarioRef.update("Token", token)
            .addOnSuccessListener {
                Log.d("Firestore", "Token actualizado correctamente!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al actualizar token", e)
            }
    }

    // Para controlar si el usuario ya está registrado o no (con Google)
    private fun verificarUsuarioFirestore(email: String, callback: (exists: Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val usuarioRef = db.collection("Usuarios").document(email)

        usuarioRef.get().addOnSuccessListener { document ->
            callback(document.exists())
        }.addOnFailureListener { e ->
            Log.w("Firestore", "Error al verificar usuario", e)
            callback(false)  // En caso de error, asumimos que el usuario no existe
        }
    }

    enum class ProviderType {
        BASIC,
        GOOGLE
    }
}
