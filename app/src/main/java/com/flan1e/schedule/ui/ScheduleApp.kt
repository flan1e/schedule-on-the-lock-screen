package com.flan1e.schedule.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flan1e.schedule.di.AppContainer
import com.flan1e.schedule.util.NotificationPermission
import com.flan1e.schedule.ui.schedule.ScheduleScreen
import com.flan1e.schedule.ui.schedule.ScheduleViewModel
import com.flan1e.schedule.ui.settings.SettingsScreen
import com.flan1e.schedule.ui.settings.SettingsViewModel

private const val ROUTE_SCHEDULE = "schedule"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun ScheduleApp(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_SCHEDULE,
    ) {
        composable(ROUTE_SCHEDULE) {
            ScheduleRoute(
                container = container,
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsRoute(
                container = container,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun ScheduleRoute(
    container: AppContainer,
    onOpenSettings: () -> Unit,
) {
    val viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = NotificationPermission.isGranted(context)
    }

    // Один раз за установку просим разрешение на уведомления (Android 13+).
    LaunchedEffect(
        state.loaded,
        state.settings?.notificationPermissionRequested,
        permissionGranted,
    ) {
        val settings = state.settings ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !permissionGranted &&
            !settings.notificationPermissionRequested
        ) {
            viewModel.markPermissionRequested()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ScheduleScreen(
        state = state,
        permissionGranted = permissionGranted,
        onOpenSettings = onOpenSettings,
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onSaveLesson = viewModel::saveLesson,
        onDeleteLesson = viewModel::deleteLesson,
    )
}

@Composable
private fun SettingsRoute(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = NotificationPermission.isGranted(context)
    }

    val versionName = remember(context) {
        @Suppress("DEPRECATION")
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
                .versionName
        }.getOrNull().orEmpty()
    }

    SettingsScreen(
        state = state,
        permissionGranted = permissionGranted,
        versionName = versionName,
        onBack = onBack,
        onEvent = viewModel::handle,
    )
}

