package com.flan1e.schedule.notification

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.flan1e.schedule.R
import com.flan1e.schedule.ScheduleApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Плитка быстрых настроек: включает и выключает приложение одним нажатием. */
class ScheduleTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settingsJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val container = (application as ScheduleApplication).container
        settingsJob?.cancel()
        settingsJob = scope.launch {
            container.settingsRepository.settings.collect { settings ->
                updateTile(settings.enabled)
            }
        }
    }

    override fun onStopListening() {
        settingsJob?.cancel()
        settingsJob = null
        super.onStopListening()
    }

    override fun onClick() {
        if (isLocked) {
            unlockAndRun { toggle() }
        } else {
            toggle()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun toggle() {
        val container = (application as ScheduleApplication).container
        scope.launch {
            val settings = container.settingsRepository.snapshot()
            container.settingsRepository.setEnabled(!settings.enabled)
            container.serviceController.apply(container.settingsRepository.snapshot())
        }
    }

    private fun updateTile(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (enabled) R.string.tile_subtitle_on else R.string.tile_subtitle_off,
            )
        }
        tile.updateTile()
    }
}
