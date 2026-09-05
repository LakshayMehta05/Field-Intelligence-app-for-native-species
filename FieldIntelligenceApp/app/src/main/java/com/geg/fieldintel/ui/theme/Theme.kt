package com.geg.fieldintel.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ForestGreen = Color(0xFF1B5E20)
val LeafGreen = Color(0xFF4CAF50)
val Bark = Color(0xFF3E2723)
val Canopy = Color(0xFFA5D6A7)
val AlertAmber = Color(0xFFFFA000)
val ConservationRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    secondary = LeafGreen,
    tertiary = Bark,
    background = Color(0xFFF4FBF4),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Canopy,
    secondary = LeafGreen,
    tertiary = Bark,
    background = Color(0xFF0E1B0F),
    surface = Color(0xFF16241A)
)

@Composable
fun FieldIntelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
