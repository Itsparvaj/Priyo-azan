package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.FontScaleOption
import com.example.model.ThemeMode

private val PriyoLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = OnGoldLight,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = OnGoldContainerLight,
    tertiary = TealTertiaryLight,
    tertiaryContainer = TealContainerLight,
    background = IvoryBackgroundLight,
    onBackground = InkOnSurfaceLight,
    surface = CreamSurfaceLight,
    onSurface = InkOnSurfaceLight,
    surfaceVariant = SageSurfaceVariantLight,
    onSurfaceVariant = MutedOnSurfaceVariantLight,
    outline = OutlineSageLight
)

private val PriyoDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = OnGoldDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = OnGoldContainerDark,
    tertiary = TealTertiaryDark,
    tertiaryContainer = TealContainerDark,
    background = MidnightBackgroundDark,
    onBackground = IvoryOnSurfaceDark,
    surface = DeepForestSurfaceDark,
    onSurface = IvoryOnSurfaceDark,
    surfaceVariant = ElevatedForestVariantDark,
    onSurfaceVariant = MutedOnSurfaceVariantDark,
    outline = OutlineForestDark
)

val PriyoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val LocalAppStrings = staticCompositionLocalOf { AppStrings.forLanguage(AppLanguage.ENGLISH) }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun PriyoAzanTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.ENGLISH,
    fontScaleOption: FontScaleOption = FontScaleOption.NORMAL,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (isDark) PriyoDarkColorScheme else PriyoLightColorScheme
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val strings = AppStrings.forLanguage(language)
    val typography = buildPriyoTypography(language, fontScaleOption.scale)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides strings,
        LocalAppLanguage provides language
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = PriyoShapes,
            content = content
        )
    }
}
