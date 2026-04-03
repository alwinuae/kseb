package com.kseb.billcalculator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green10,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = GreenGrey80,
    onSecondary = GreenGrey30,
    tertiary = Orange80,
    onTertiary = Orange40,
    background = Green10,
    surface = Green10,
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = GreenGrey50,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = Orange40,
    onTertiary = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun KSEBBillCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
