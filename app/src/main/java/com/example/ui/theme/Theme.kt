package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Enum representing the requested pastel themes
enum class PhonelyticsTheme(val displayName: String) {
    LAVENDER_FROST("Lavender Frost"),
    CYBER_MINT("Cyber Mint"),
    OCEAN_BLUE("Ocean Blue"),
    SAKURA_PINK("Sakura Pink"),
    SUNSET_PEACH("Sunset Peach"),
    ARCTIC_WHITE("Arctic White"),
    NEON_PURPLE("Neon Purple")
}

@Composable
fun PhonelyticsTheme(
    selectedTheme: PhonelyticsTheme = PhonelyticsTheme.LAVENDER_FROST,
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val primaryColor: Color
    val secondaryColor: Color
    val tertiaryColor: Color
    val backgroundColor: Color
    val surfaceColor: Color

    if (darkTheme) {
        // Base dark selections with Immersive premium UI spacing values
        when (selectedTheme) {
            PhonelyticsTheme.LAVENDER_FROST -> {
                primaryColor = LavenderPrimary
                secondaryColor = LavenderSecondary
                tertiaryColor = LavenderTertiary
                backgroundColor = if (amoledMode) AmoledBackground else Color(0xFF030712)
                surfaceColor = Color(0xFF0B1220)
            }
            PhonelyticsTheme.CYBER_MINT -> {
                primaryColor = CyberMintPrimary
                secondaryColor = CyberMintSecondary
                tertiaryColor = CyberMintTertiary
                backgroundColor = if (amoledMode) AmoledBackground else Color(0xFF030712)
                surfaceColor = Color(0xFF0B1220)
            }
            PhonelyticsTheme.OCEAN_BLUE -> {
                primaryColor = OceanBluePrimary
                secondaryColor = OceanBlueSecondary
                tertiaryColor = OceanBlueTertiary
                backgroundColor = if (amoledMode) AmoledBackground else Color(0xFF030712)
                surfaceColor = Color(0xFF0B1220)
            }
            PhonelyticsTheme.SAKURA_PINK -> {
                primaryColor = SakuraPrimary
                secondaryColor = SakuraSecondary
                tertiaryColor = SakuraTertiary
                backgroundColor = if (amoledMode) AmoledBackground else Color(0xFF030712)
                surfaceColor = Color(0xFF0B1220)
            }
            PhonelyticsTheme.SUNSET_PEACH -> {
                primaryColor = PeachPrimary
                secondaryColor = PeachSecondary
                tertiaryColor = PeachTertiary
                backgroundColor = if (amoledMode) AmoledBackground else Color(0xFF030712)
                surfaceColor = Color(0xFF0B1220)
            }
            PhonelyticsTheme.ARCTIC_WHITE -> {
                primaryColor = ArcticPrimary
                secondaryColor = ArcticSecondary
                tertiaryColor = ArcticTertiary
                backgroundColor = if (amoledMode) AmoledBackground else Color(0xFF030712)
                surfaceColor = Color(0xFF0B1220)
            }
            PhonelyticsTheme.NEON_PURPLE -> {
                primaryColor = NeonPurplePrimary
                secondaryColor = NeonPurpleSecondary
                tertiaryColor = NeonPurpleTertiary
                backgroundColor = if (amoledMode) AmoledBackground else Color(0xFF030712)
                surfaceColor = Color(0xFF0B1220)
            }
        }
    } else {
        // Light pastel mappings
        when (selectedTheme) {
            PhonelyticsTheme.LAVENDER_FROST -> {
                primaryColor = Color(0xFF6B58C9) // slightly darker for light mode contrast
                secondaryColor = LavenderSecondary
                tertiaryColor = LavenderTertiary
                backgroundColor = LavenderLightBackground
                surfaceColor = Color.White
            }
            PhonelyticsTheme.CYBER_MINT -> {
                primaryColor = Color(0xFF009C5A)
                secondaryColor = CyberMintSecondary
                tertiaryColor = CyberMintTertiary
                backgroundColor = CyberMintLightBackground
                surfaceColor = Color.White
            }
            PhonelyticsTheme.OCEAN_BLUE -> {
                primaryColor = Color(0xFF0583CA)
                secondaryColor = OceanBlueSecondary
                tertiaryColor = OceanBlueTertiary
                backgroundColor = OceanBlueLightBackground
                surfaceColor = Color.White
            }
            PhonelyticsTheme.SAKURA_PINK -> {
                primaryColor = Color(0xFFCD486D)
                secondaryColor = SakuraSecondary
                tertiaryColor = SakuraTertiary
                backgroundColor = SakuraLightBackground
                surfaceColor = Color.White
            }
            PhonelyticsTheme.SUNSET_PEACH -> {
                primaryColor = Color(0xFFE25B2D)
                secondaryColor = PeachSecondary
                tertiaryColor = PeachTertiary
                backgroundColor = PeachLightBackground
                surfaceColor = Color.White
            }
            PhonelyticsTheme.ARCTIC_WHITE -> {
                primaryColor = Color(0xFF008AA8)
                secondaryColor = ArcticSecondary
                tertiaryColor = ArcticTertiary
                backgroundColor = ArcticLightBackground
                surfaceColor = Color.White
            }
            PhonelyticsTheme.NEON_PURPLE -> {
                primaryColor = Color(0xFF9000C7)
                secondaryColor = NeonPurpleSecondary
                tertiaryColor = NeonPurpleTertiary
                backgroundColor = NeonPurpleLightBackground
                surfaceColor = Color.White
            }
        }
    }

    val colorScheme = darkColorScheme(
        primary = primaryColor,
        secondary = secondaryColor,
        tertiary = tertiaryColor,
        background = backgroundColor,
        surface = surfaceColor,
        onPrimary = if (darkTheme) Color.Black else Color.White,
        onSecondary = if (darkTheme) Color.Black else Color.Black,
        onBackground = if (darkTheme) Color(0xFFEDF2F4) else Color(0xFF1E2022),
        onSurface = if (darkTheme) Color(0xFFEDF2F4) else Color(0xFF1E2022)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
