package com.example.helpdesk

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class ThemePalette(val key: String, val label: String, val primary: Color, val accent: Color)

val PALETTES = listOf(
    ThemePalette("INDIGO", "Indigo", Color(0xFF4F46E5), Color(0xFF7C3AED)),
    ThemePalette("OCEAN", "Ocean", Color(0xFF0284C7), Color(0xFF14B8A6)),
    ThemePalette("FOREST", "Forest", Color(0xFF15803D), Color(0xFF65A30D)),
    ThemePalette("SUNSET", "Sunset", Color(0xFFEA580C), Color(0xFFDB2777)),
    ThemePalette("ROSE", "Rose", Color(0xFFE11D48), Color(0xFF9333EA))
)

object ThemeManager {
    private const val PREFS = "helpdesk_theme"
    private var loaded = false

    var paletteKey by mutableStateOf("INDIGO")
    var darkMode by mutableStateOf(false)

    private val palette: ThemePalette
        get() = PALETTES.firstOrNull { it.key == paletteKey } ?: PALETTES.first()
    val primary: Color get() = palette.primary
    val accent: Color get() = palette.accent

    fun ensureLoaded(context: Context) {
        if (loaded) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        paletteKey = prefs.getString("palette", "INDIGO") ?: "INDIGO"
        darkMode = prefs.getBoolean("dark", false)
        loaded = true
    }

    fun setPalette(context: Context, key: String) {
        paletteKey = key
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("palette", key).apply()
    }

    fun setDark(context: Context, value: Boolean) {
        darkMode = value
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("dark", value).apply()
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val primary = ThemeManager.primary
    val accent = ThemeManager.accent
    val dark = ThemeManager.darkMode
    val onSurface = if (dark) Color(0xFFE2E8F0) else Color(0xFF0F172A)

    val scheme = if (dark) {
        darkColorScheme(
            primary = primary, onPrimary = Color.White, secondary = accent,
            primaryContainer = primary.copy(alpha = 0.25f), onPrimaryContainer = onSurface,
            background = Color(0xFF0F172A), onBackground = onSurface,
            surface = Color(0xFF1E293B), onSurface = onSurface
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = Color.White, secondary = accent,
            primaryContainer = primary.copy(alpha = 0.12f), onPrimaryContainer = onSurface,
            background = Color(0xFFF8FAFC), onBackground = onSurface,
            surface = Color.White, onSurface = onSurface
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

// Drop-in replacement for setContent { } that applies the saved theme
fun ComponentActivity.setAppContent(content: @Composable () -> Unit) {
    ThemeManager.ensureLoaded(this)
    setContent { AppTheme { content() } }
}

@Composable
fun ThemeSettingsButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { open = true }, modifier = modifier.fillMaxWidth()) {
        Text("🎨 Theme")
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dark mode", modifier = Modifier.weight(1f))
                        Switch(
                            checked = ThemeManager.darkMode,
                            onCheckedChange = { ThemeManager.setDark(context, it) }
                        )
                    }
                    Text("Accent colour", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    Row {
                        PALETTES.forEach { p ->
                            val selected = p.key == ThemeManager.paletteKey
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(34.dp)
                                    .background(Brush.linearGradient(listOf(p.primary, p.accent)), CircleShape)
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { ThemeManager.setPalette(context, p.key) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Text("✓", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { open = false }) { Text("Done") } }
        )
    }
}