#!/bin/bash
set -e

TV_SERIAL=""
for serial in $(adb devices | awk 'NR>1 && /device$/{print $1}'); do
    model=$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    if echo "$model" | grep -qi "atv\|tv"; then
        TV_SERIAL="$serial"
        break
    fi
done

if [ -z "$TV_SERIAL" ]; then
    echo "Error: No Android TV device found."
    exit 1
fi

adb -s "$TV_SERIAL" shell am broadcast -a com.bk.sleeptimer.RED_BUTTON
echo "Sent RED button to $TV_SERIAL"
