package com.flan1e.schedule.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.flan1e.schedule.domain.model.AppTheme

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF001452),
    secondary = Color(0xFF475D92),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E2FF),
    onSecondaryContainer = Color(0xFF001945),
    surface = Color(0xFFFFFBFE),
    background = Color(0xFFFFFBFE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF1D4ED8),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFB6C4FF),
    onSecondary = Color(0xFF162E60),
    secondaryContainer = Color(0xFF2F4478),
    onSecondaryContainer = Color(0xFFD9E2FF),
    surface = Color(0xFF141218),
    background = Color(0xFF141218),
)

@Composable
fun ScheduleTheme(
    theme: AppTheme,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
