package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.settings.AuraThemeOption

private val ObsidianSaffronColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = SaffronOnPrimary,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = SaffronOnContainer,
    secondary = CoralSecondary,
    onSecondary = CoralOnSecondary,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = CoralOnContainer,
    tertiary = IndigoTertiary,
    onTertiary = IndigoOnTertiary,
    tertiaryContainer = IndigoContainer,
    onTertiaryContainer = IndigoOnContainer,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val MidnightEmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = Color(0xFF0F3547),
    onSecondaryContainer = Color(0xFFCEEEFF),
    tertiary = MintTertiary,
    onTertiary = Color(0xFF002420),
    background = EmeraldBg,
    onBackground = TextPrimary,
    surface = EmeraldSurface,
    onSurface = TextPrimary,
    surfaceVariant = EmeraldSurfaceElevated,
    onSurfaceVariant = Color(0xFF9EC2BC),
    surfaceContainerLowest = EmeraldBg,
    surfaceContainerLow = EmeraldSurface,
    surfaceContainer = EmeraldSurfaceElevated,
    surfaceContainerHigh = EmeraldSurfaceHighest,
    outline = EmeraldOutline,
    outlineVariant = EmeraldOutlineSubtle,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val RoyalAmethystColorScheme = darkColorScheme(
    primary = AmethystPrimary,
    onPrimary = AmethystOnPrimary,
    primaryContainer = AmethystContainer,
    onPrimaryContainer = AmethystOnContainer,
    secondary = RoseGoldSecondary,
    onSecondary = RoseGoldOnSecondary,
    secondaryContainer = Color(0xFF4A1830),
    onSecondaryContainer = Color(0xFFFFD9E5),
    tertiary = LavenderTertiary,
    onTertiary = Color(0xFF002436),
    background = AmethystBg,
    onBackground = TextPrimary,
    surface = AmethystSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmethystSurfaceElevated,
    onSurfaceVariant = Color(0xFFB8A9D9),
    surfaceContainerLowest = AmethystBg,
    surfaceContainerLow = AmethystSurface,
    surfaceContainer = AmethystSurfaceElevated,
    surfaceContainerHigh = AmethystSurfaceHighest,
    outline = AmethystOutline,
    outlineVariant = AmethystOutlineSubtle,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val CrimsonVelvetColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = CrimsonOnPrimary,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = CrimsonOnContainer,
    secondary = GoldSecondary,
    onSecondary = GoldOnSecondary,
    secondaryContainer = Color(0xFF47300A),
    onSecondaryContainer = Color(0xFFFFE3B3),
    tertiary = PeachTertiary,
    onTertiary = Color(0xFF331100),
    background = CrimsonBg,
    onBackground = TextPrimary,
    surface = CrimsonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CrimsonSurfaceElevated,
    onSurfaceVariant = Color(0xFFD6ACB6),
    surfaceContainerLowest = CrimsonBg,
    surfaceContainerLow = CrimsonSurface,
    surfaceContainer = CrimsonSurfaceElevated,
    surfaceContainerHigh = CrimsonSurfaceHighest,
    outline = CrimsonOutline,
    outlineVariant = CrimsonOutlineSubtle,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val WarmAlabasterColorScheme = lightColorScheme(
    primary = WarmAmberPrimary,
    onPrimary = AlabasterSurface,
    primaryContainer = WarmAmberContainer,
    onPrimaryContainer = WarmAmberOnContainer,
    secondary = CoralSecondary,
    onSecondary = AlabasterSurface,
    secondaryContainer = Color(0xFFFFE5E1),
    onSecondaryContainer = Color(0xFF3F0B05),
    tertiary = Color(0xFF4A5CE6),
    onTertiary = AlabasterSurface,
    background = AlabasterBg,
    onBackground = WarmTextPrimary,
    surface = AlabasterSurface,
    onSurface = WarmTextPrimary,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = WarmTextSecondary,
    surfaceContainerLowest = AlabasterSurface,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = AlabasterSurfaceVariant,
    surfaceContainerHigh = AlabasterSurfaceHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineSubtle,
    error = LightErrorRed,
    onError = AlabasterSurface,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

fun themePreviewSwatches(option: AuraThemeOption): List<Color> {
    return when (option) {
        AuraThemeOption.OBSIDIAN_SAFFRON -> listOf(ObsidianBg, SaffronPrimary, IndigoTertiary)
        AuraThemeOption.MIDNIGHT_EMERALD -> listOf(EmeraldBg, EmeraldPrimary, CyanSecondary)
        AuraThemeOption.ROYAL_AMETHYST -> listOf(AmethystBg, AmethystPrimary, RoseGoldSecondary)
        AuraThemeOption.CRIMSON_VELVET -> listOf(CrimsonBg, CrimsonPrimary, GoldSecondary)
        AuraThemeOption.WARM_ALABASTER -> listOf(AlabasterBg, WarmAmberPrimary, Color(0xFF4A5CE6))
    }
}

fun colorSchemeFor(option: AuraThemeOption): ColorScheme {
    return when (option) {
        AuraThemeOption.OBSIDIAN_SAFFRON -> ObsidianSaffronColorScheme
        AuraThemeOption.MIDNIGHT_EMERALD -> MidnightEmeraldColorScheme
        AuraThemeOption.ROYAL_AMETHYST -> RoyalAmethystColorScheme
        AuraThemeOption.CRIMSON_VELVET -> CrimsonVelvetColorScheme
        AuraThemeOption.WARM_ALABASTER -> WarmAlabasterColorScheme
    }
}

val AuraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AuraMateTheme(
    themeOption: AuraThemeOption = AuraThemeOption.OBSIDIAN_SAFFRON,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeFor(themeOption)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraMateTypography,
        shapes = AuraShapes,
        content = content
    )
}
