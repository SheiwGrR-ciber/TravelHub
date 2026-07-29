package com.travelhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TravelHubColorScheme = lightColorScheme(
    primary = Terracota,
    onPrimary = CremaCalido,
    primaryContainer = MarronOscuro,
    onPrimaryContainer = CremaClaro,
    secondary = TealProfundo,
    onSecondary = CremaCalido,
    secondaryContainer = TealClaro,
    onSecondaryContainer = CremaClaro,
    tertiary = DoradoArena,
    onTertiary = TextoOscuro,
    tertiaryContainer = DoradoOscuro,
    onTertiaryContainer = CremaCalido,
    background = CremaCalido,
    onBackground = TextoOscuro,
    surface = Superficie,
    onSurface = TextoOscuro,
    surfaceVariant = CremaClaro,
    onSurfaceVariant = TextoClaro,
    error = Error,
    onError = CremaCalido
)

@Composable
fun TravelHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TravelHubColorScheme,
        typography = Typography,
        content = content
    )
}
