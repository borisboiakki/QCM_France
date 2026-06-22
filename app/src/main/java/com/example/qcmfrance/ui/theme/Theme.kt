package com.example.qcmfrance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.qcmfrance.data.repository.TextSizeMode
import com.example.qcmfrance.data.repository.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary   = Blue80,
    secondary = Green80,
    error     = Red80
)

private val LightColorScheme = lightColorScheme(
    primary   = Blue40,
    secondary = Green40,
    error     = Red40
)

private fun Typography.scale(factor: Float): Typography = if (factor == 1f) this else Typography(
    displayLarge   = displayLarge.copy(  fontSize = (displayLarge.fontSize.value   * factor).sp, lineHeight = (displayLarge.lineHeight.value   * factor).sp),
    displayMedium  = displayMedium.copy( fontSize = (displayMedium.fontSize.value  * factor).sp, lineHeight = (displayMedium.lineHeight.value  * factor).sp),
    displaySmall   = displaySmall.copy(  fontSize = (displaySmall.fontSize.value   * factor).sp, lineHeight = (displaySmall.lineHeight.value   * factor).sp),
    headlineLarge  = headlineLarge.copy( fontSize = (headlineLarge.fontSize.value  * factor).sp, lineHeight = (headlineLarge.lineHeight.value  * factor).sp),
    headlineMedium = headlineMedium.copy(fontSize = (headlineMedium.fontSize.value * factor).sp, lineHeight = (headlineMedium.lineHeight.value * factor).sp),
    headlineSmall  = headlineSmall.copy( fontSize = (headlineSmall.fontSize.value  * factor).sp, lineHeight = (headlineSmall.lineHeight.value  * factor).sp),
    titleLarge     = titleLarge.copy(    fontSize = (titleLarge.fontSize.value     * factor).sp, lineHeight = (titleLarge.lineHeight.value     * factor).sp),
    titleMedium    = titleMedium.copy(   fontSize = (titleMedium.fontSize.value    * factor).sp, lineHeight = (titleMedium.lineHeight.value    * factor).sp),
    titleSmall     = titleSmall.copy(    fontSize = (titleSmall.fontSize.value     * factor).sp, lineHeight = (titleSmall.lineHeight.value     * factor).sp),
    bodyLarge      = bodyLarge.copy(     fontSize = (bodyLarge.fontSize.value      * factor).sp, lineHeight = (bodyLarge.lineHeight.value      * factor).sp),
    bodyMedium     = bodyMedium.copy(    fontSize = (bodyMedium.fontSize.value     * factor).sp, lineHeight = (bodyMedium.lineHeight.value     * factor).sp),
    bodySmall      = bodySmall.copy(     fontSize = (bodySmall.fontSize.value      * factor).sp, lineHeight = (bodySmall.lineHeight.value      * factor).sp),
    labelLarge     = labelLarge.copy(    fontSize = (labelLarge.fontSize.value     * factor).sp, lineHeight = (labelLarge.lineHeight.value     * factor).sp),
    labelMedium    = labelMedium.copy(   fontSize = (labelMedium.fontSize.value    * factor).sp, lineHeight = (labelMedium.lineHeight.value    * factor).sp),
    labelSmall     = labelSmall.copy(    fontSize = (labelSmall.fontSize.value     * factor).sp, lineHeight = (labelSmall.lineHeight.value     * factor).sp),
)

@Composable
fun QcmFranceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    textSizeMode: TextSizeMode = TextSizeMode.MEDIUM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val scale = when (textSizeMode) {
        TextSizeMode.SMALL  -> 0.85f
        TextSizeMode.MEDIUM -> 1.00f
        TextSizeMode.LARGE  -> 1.15f
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography.scale(scale),
        content     = content
    )
}
