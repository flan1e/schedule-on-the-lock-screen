package com.flan1e.schedule.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.flan1e.schedule.domain.model.AppLanguage

/**
 * Переключение языка через per-app locales.
 * На Android 13+ AppCompat делегирует вызов системному LocaleManager,
 * на более старых — применяет локаль к ресурсам приложения.
 */
object LocaleController {

    fun apply(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.RUSSIAN -> LocaleListCompat.forLanguageTags("ru")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
