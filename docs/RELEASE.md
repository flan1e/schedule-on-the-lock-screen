# Выпуск новой версии

Инструкция для владельца репозитория. Пользователям она не нужна.

## 1. Подготовка

- JDK 17 и Android SDK 36 (`platforms;android-36`, `build-tools;36.0.0`).
- Файл `keystore.properties` в корне проекта и ключ `keystore/release.jks`.
  Оба в `.gitignore` и **не должны** попадать в репозиторий. Сделайте резервную
  копию ключа и пароля: без них нельзя выпустить обновление, которое установится
  поверх предыдущей версии.
- Если файла нет, создайте ключ:

  ```bash
  keytool -genkeypair -v -keystore keystore/release.jks -alias schedule \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Schedule, O=flan1e, C=RU"
  ```

  и заполните `keystore.properties` по образцу `keystore.properties.example`.

## 2. Версия

В `app/build.gradle.kts`:

```kotlin
versionCode = 2          // +1 для каждого релиза
versionName = "1.1.0"    // отображается пользователю
```

## 3. Проверка и сборка

```bash
./gradlew testDebugUnitTest lintDebug assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`.

Проверьте, что файл подписан:

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Установите его на устройство (при необходимости удалив debug-версию — подписи
разные) и проверьте: запуск, добавление пары, уведомление, экран блокировки.

## 4. Публикация

1. Закоммитьте изменения и отправьте ветку в GitHub.
2. Переименуйте APK в `schedule-<versionName>.apk` (например, `schedule-1.1.0.apk`).
3. Создайте релиз: GitHub → Releases → «Draft a new release».
   - Tag: `v1.1.0` (создать новый).
   - Title: `1.1.0`.
   - Описание: список изменений.
   - Прикрепите `schedule-1.1.0.apk`.
4. Опубликуйте релиз. Ссылка `releases/latest` в README всегда ведёт на него.

## 5. Если ключ утерян

Обновление с новым ключом не установится поверх старого: пользователям придётся
удалить приложение и потерять данные. Поэтому ключ и пароль нужно хранить
в надёжном месте (менеджер паролей, зашифрованный архив).
