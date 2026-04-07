# Sleep Timer - Android TV

## Project Overview

Minimal Android TV app that replicates classic TV sleep timer functionality. Triggered by the **red color button** on the remote, cycles through timer presets, shows a brief overlay, and puts the TV to sleep when the timer expires.

## Architecture

Single-file app — everything lives in `SleepTimerService` (an `AccessibilityService`). No activities, no fragments, no Compose, no external dependencies.

### Key Files

- `app/src/main/java/com/bk/sleeptimer/SleepTimerService.kt` — all app logic
- `app/src/main/java/com/bk/sleeptimer/BootReceiver.kt` — boot safety net (logs permission state)
- `app/src/main/res/xml/accessibility_service_config.xml` — accessibility service flags
- `app/src/main/AndroidManifest.xml` — service + receiver declarations
- `setup.sh` — ADB install + permission setup

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Deploy

```bash
./setup.sh
```

Requires ADB connection to the TV.

## Technical Decisions

- **No Compose** — overlay is a single `TextView` on `WindowManager`. Compose would add ~5-8MB for no benefit.
- **No foreground service** — `AccessibilityService` already runs at foreground priority.
- **No activities** — app is invisible, service-only. Enabled via ADB.
- **`CountDownTimer`** over `AlarmManager` — more accurate for minute-level countdown, sufficient for this use case.
- **`GLOBAL_ACTION_LOCK_SCREEN`** for sleep — available since API 28, triggers standby on Android TV without system-level permissions.
- **`View.GONE`/`VISIBLE`** toggle instead of `addView`/`removeView` — avoids flicker, zero render cost when hidden.

## Conventions

- Kotlin, no Java
- Gradle Kotlin DSL (`.kts`)
- Version catalog in `gradle/libs.versions.toml`
- `compileSdk 35`, `minSdk 28`, `targetSdk 35`
- Zero external dependencies
