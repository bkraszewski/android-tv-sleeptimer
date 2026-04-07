# Sleep Timer for Android TV

A minimal, fast sleep timer app for Android TV that replicates the classic sleep timer found in traditional TVs.

## How It Works

Press the **red color button** on your TV remote to control the timer:

| Press | Action |
|---|---|
| 1st | Set timer to **30 min** |
| 2nd | Change to **45 min** |
| 3rd | Change to **60 min** |
| 4th | Change to **1:30** |
| 5th | Change to **2:00** |
| 6th | **Cancel** timer |

A small overlay appears in the bottom-right corner showing the selected time, then auto-hides after 3 seconds.

### Mid-Timer Interaction

If a timer is already running and you press the red button:
1. **First press** — shows the remaining time (e.g. "13")
2. **Next press** — extends to the next preset above the remaining time
3. Continue pressing to cycle through higher presets or cancel

When the timer reaches zero, the TV goes to sleep automatically.

## Requirements

- Android TV running **Android 9 (API 28)** or higher
- ADB access to the TV (USB or network)
- TV remote with a **red color/function button** (keycode `KEYCODE_PROG_RED`)

Tested on: **Sony Bravia Android TV 9** (BRAVIA_ATV3_4K_EU)

## Installation

### 1. Build

```bash
./gradlew assembleDebug
```

### 2. Deploy

Connect to your TV via ADB, then run:

```bash
./setup.sh
```

This installs the APK, grants overlay permission, and enables the accessibility service.

### Manual Setup (if setup.sh doesn't work)

```bash
# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Grant overlay permission
adb shell appops set com.bk.sleeptimer SYSTEM_ALERT_WINDOW allow

# Enable accessibility service
adb shell settings put secure enabled_accessibility_services com.bk.sleeptimer/.SleepTimerService
adb shell settings put secure accessibility_enabled 1
```

> **Note:** If you have other accessibility services enabled, the `settings put` command will overwrite them. To preserve existing services, read the current value first and append with a colon separator.

## Technical Design

### Why AccessibilityService?

The only way to intercept remote button presses globally (across all apps) without root access. The service runs at foreground priority — Android keeps it alive and restarts it if killed.

### Why not Compose?

The entire UI is a single `TextView` overlay. Adding Jetpack Compose would increase the APK from ~2MB to ~10MB and slow down cold start — all for rendering one number on screen.

### How does it put the TV to sleep?

Uses `AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` (API 28+), which triggers standby mode on Android TV. No system-level permissions needed since the accessibility service already has this capability.

### Does it survive TV restarts?

Yes. The accessibility service setting persists in `secure` settings and Android auto-restarts enabled accessibility services on boot. A `BOOT_COMPLETED` receiver also logs permission state as a diagnostic safety net.

### Battery/resource impact?

Near zero. The service only does work when:
- The red button is pressed (instant response)
- Every 60 seconds to update the remaining minutes counter
- Timer expiry (one `performGlobalAction` call)

No wake locks, no polling, no background threads.

## Testing on Android TV Emulator

1. In Android Studio: **Device Manager → Create Virtual Device → TV** → pick any Android TV profile
2. Select a system image with **API 28+**
3. Build and deploy:

```bash
./gradlew assembleDebug
./setup.sh
```

The emulator's default remote doesn't have color buttons, so send the red button keycode manually:

```bash
adb shell input keyevent KEYCODE_PROG_RED
```

Run this each time you want to simulate a red button press.

## Troubleshooting

**Red button doesn't work:**
- Your remote may use a different keycode. Connect via ADB and run `adb shell getevent -l` while pressing the button to discover the actual keycode.
- Another accessibility service with key filtering may be active (only one is allowed at a time).

**Overlay doesn't appear:**
- Re-grant overlay permission: `adb shell appops set com.bk.sleeptimer SYSTEM_ALERT_WINDOW allow`

**TV doesn't go to sleep:**
- `GLOBAL_ACTION_LOCK_SCREEN` behavior can vary by manufacturer. If it doesn't trigger standby on your TV, open an issue.

**Service not running after reboot:**
- Check logs: `adb logcat -s SleepTimer`
- Re-run `./setup.sh`

## License

MIT
