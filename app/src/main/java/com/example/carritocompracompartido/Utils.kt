package com.example.carritocompracompartido

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object Utils {
    fun setLanguage(context: Context) {
        val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file),
            AppCompatActivity.MODE_PRIVATE
        )
        val languageCode = prefs.getString("locale", "en") ?: "en"
        updateLanguage(context, languageCode)
    }

    fun setTheme(context: Context) {
        val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file),
            AppCompatActivity.MODE_PRIVATE
        )
        val theme = prefs.getString("theme", "Light") // Assuming DefaultTheme as the default
        when (theme) {
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "Claro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Oscuro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "Argia" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Iluna" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "Sistema" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun ponerPreferencia(context: Context, key: String, value: String) {
        val prefs: SharedPreferences.Editor = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString(key, value)
        prefs.apply()
    }

    fun obtenerPreferencia(context: Context, key: String): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }

    fun limpiarPreferencias(context: Context) {
        val prefs: SharedPreferences.Editor = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()
    }

    private fun updateLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}
