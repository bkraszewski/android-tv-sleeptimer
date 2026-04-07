#!/bin/bash
set -e

PKG="com.bk.sleeptimer"
SERVICE="$PKG/.SleepTimerService"
APK="app/build/outputs/apk/debug/app-debug.apk"

# Find Android TV device among connected devices
# Prefer network-connected (IP) devices, then fall back to model name match
TV_SERIAL=""
for serial in $(adb devices | awk 'NR>1 && /device$/{print $1}'); do
    if echo "$serial" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+'; then
        TV_SERIAL="$serial"
        break
    fi
    model=$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    if echo "$model" | grep -qi "atv\|tv"; then
        TV_SERIAL="$serial"
    fi
done

if [ -z "$TV_SERIAL" ]; then
    echo "Error: No Android TV device found."
    echo "Connected devices:"
    adb devices -l
    exit 1
fi

ADB="adb -s $TV_SERIAL"
echo "Using Android TV device: $TV_SERIAL"

if [ ! -f "$APK" ]; then
    echo "APK not found. Building..."
    ./gradlew assembleDebug
fi

echo "Installing APK..."
$ADB install -r "$APK"

echo "Granting overlay permission..."
$ADB shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow

echo "Enabling accessibility service..."
$ADB shell settings put secure enabled_accessibility_services "$SERVICE"
$ADB shell settings put secure accessibility_enabled 1

echo ""
echo "Done! Sleep timer is active on $TV_SERIAL."
echo "Press the RED button on your remote to use it."
