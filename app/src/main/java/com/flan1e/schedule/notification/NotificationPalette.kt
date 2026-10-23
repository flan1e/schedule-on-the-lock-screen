package com.flan1e.schedule.notification

import android.content.Context
import android.content.res.Configuration

/**
 * Цвета текста для собственного layout уведомления.
 *
 * Фон уведомления следует системной теме, а не теме приложения, поэтому
 * `?android:attr/textColorPrimary` из темы приложения не подходит: в светлой
 * теме приложения на тёмном уведомлении получается чёрный текст.
 */
internal data class NotificationPalette(
    val primary: Int,
    val secondary: Int,
) {
    companion object {
        private const val PRIMARY_DARK = 0xFFFFFFFF.toInt()
        private const val SECONDARY_DARK = 0xB3FFFFFF.toInt()
        private const val PRIMARY_LIGHT = 0xDE000000.toInt()
        private const val SECONDARY_LIGHT = 0x99000000.toInt()

        fun of(context: Context): NotificationPalette {
            val uiMode = context.resources.configuration.uiMode
            val isNight = (uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            return if (isNight) {
                NotificationPalette(PRIMARY_DARK, SECONDARY_DARK)
            } else {
                NotificationPalette(PRIMARY_LIGHT, SECONDARY_LIGHT)
            }
        }
    }
}
