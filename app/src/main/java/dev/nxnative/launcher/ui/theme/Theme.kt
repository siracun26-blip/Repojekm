package dev.nxnative.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NxDarkColorScheme = darkColorScheme(
    primary = NxPrimary,
    onPrimary = NxOnPrimary,
    secondary = NxSecondary,
    onSecondary = NxOnSecondary,
    background = NxBackground,
    onBackground = NxTextPrimary,
    surface = NxSurface,
    onSurface = NxTextPrimary,
    surfaceVariant = NxSurfaceVariant,
    onSurfaceVariant = NxTextMuted,
    error = NxDanger
)

@Composable
fun NXNativeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NxDarkColorScheme,
        typography = NxTypography,
        content = content
    )
}
