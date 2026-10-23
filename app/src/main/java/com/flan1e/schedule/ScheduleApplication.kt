package com.flan1e.schedule

import android.app.Application
import com.flan1e.schedule.di.AppContainer
import com.flan1e.schedule.notification.NotificationChannels

class ScheduleApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensure(this)
    }
}
