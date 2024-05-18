package com.example.carritocompracompartido

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object Utils {
    fun setLanguage(context: Context) {
        val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file),
            AppCompatActivity.MODE_PRIVATE
        )
        val languageCode = prefs.getString("locale", "en") ?: "en"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
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

}
