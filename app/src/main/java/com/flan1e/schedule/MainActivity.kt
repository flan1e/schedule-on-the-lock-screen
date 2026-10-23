package com.flan1e.schedule

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flan1e.schedule.domain.model.AppTheme
import com.flan1e.schedule.ui.ScheduleApp
import com.flan1e.schedule.ui.theme.ScheduleTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ScheduleApplication).container
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = null)

            ScheduleTheme(theme = settings?.theme ?: AppTheme.SYSTEM) {
                ScheduleApp(container = container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Приложение на переднем плане: можно безопасно синхронизировать службу.
        (application as ScheduleApplication).container.serviceController.sync()
    }
}
