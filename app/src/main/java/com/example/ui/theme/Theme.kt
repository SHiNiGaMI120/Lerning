package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LingoLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = IndigoTertiary,
    onTertiary = IndigoOnTertiary,
    tertiaryContainer = IndigoContainer,
    onTertiaryContainer = IndigoOnContainer,
    background = AlabasterBg,
    onBackground = SlateOnBg,
    surface = IvorySurface,
    onSurface = SlateOnBg,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedOnSurface,
    outline = WarmOutline,
    error = ErrorCrimson
)

private val LingoDarkColorScheme = darkColorScheme(
    primary = DarkMintPrimary,
    onPrimary = DarkMintOnPrimary,
    primaryContainer = DarkMintContainer,
    onPrimaryContainer = DarkMintOnContainer,
    secondary = DarkGoldSecondary,
    onSecondary = DarkGoldOnSecondary,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = DarkGoldOnContainer,
    tertiary = DarkPeriwinkleTertiary,
    onTertiary = DarkPeriwinkleOnTertiary,
    tertiaryContainer = DarkPeriwinkleContainer,
    onTertiaryContainer = DarkPeriwinkleOnContainer,
    background = ObsidianBg,
    onBackground = CloudOnBg,
    surface = MidnightSurface,
    onSurface = CloudOnBg,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = CloudMuted,
    outline = MidnightOutline,
    error = DarkErrorCrimson
)

val LingoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LingoWorldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LingoDarkColorScheme else LingoLightColorScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LingoTypography,
            shapes = LingoShapes,
            content = content
        )
    }
}
