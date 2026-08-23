#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-/home/m3kky/android-sdk}"
ADB="$SDK_ROOT/platform-tools/adb"
SERIAL="emulator-5554"
IME="at.xikey.ime/.XiKeyInputMethodService"
RUNNER="at.xikey.ime.test/androidx.test.runner.AndroidJUnitRunner"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
ARTIFACT_DIR="${XIKEY_ARTIFACT_DIR:-$ROOT_DIR/build/emulator-artifacts/$TIMESTAMP}"
KEEP_IME=false
RESET=false
HEADLESS=false

usage() {
  printf '%s\n' \
    "Usage: scripts/run-emulator-tests.sh [--reset] [--headless] [--keep-ime]" \
    "" \
    "Builds, installs, selects XiKey, runs all instrumentation tests, and captures:" \
    "  instrumentation.txt, screenshot.png, window.xml, logcat.txt, device-state.txt" \
    "" \
    "Safety: only emulator-5554 is addressed; attached physical devices are ignored."
}

while test $# -gt 0; do
  case "$1" in
    --reset) RESET=true ;;
    --headless) HEADLESS=true ;;
    --keep-ime) KEEP_IME=true ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown option: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

mkdir -p "$ARTIFACT_DIR"
mode=""
$HEADLESS && mode="--headless"
if $RESET; then
  "$ROOT_DIR/scripts/xikey-emulator.sh" reset "$mode"
else
  "$ROOT_DIR/scripts/xikey-emulator.sh" start "$mode"
fi

previous_ime="$("$ADB" -s "$SERIAL" shell settings get secure default_input_method 2>/dev/null | tr -d '\r')"
collect_artifacts() {
  status=$?
  "$ADB" -s "$SERIAL" exec-out screencap -p >"$ARTIFACT_DIR/screenshot.png" 2>/dev/null || true
  "$ADB" -s "$SERIAL" shell uiautomator dump /sdcard/xikey-window.xml >/dev/null 2>&1 || true
  "$ADB" -s "$SERIAL" pull /sdcard/xikey-window.xml "$ARTIFACT_DIR/window.xml" >/dev/null 2>&1 || true
  "$ADB" -s "$SERIAL" logcat -d -v threadtime >"$ARTIFACT_DIR/logcat.txt" 2>/dev/null || true
  {
    printf 'serial=%s\n' "$SERIAL"
    printf 'avd=xikey_api35\n'
    printf 'previous_ime=%s\n' "$previous_ime"
    printf 'test_ime=%s\n' "$IME"
    printf 'final_default_ime=%s\n' "$("$ADB" -s "$SERIAL" shell settings get secure default_input_method 2>/dev/null | tr -d '\r')"
    "$ADB" -s "$SERIAL" shell wm size 2>/dev/null || true
    "$ADB" -s "$SERIAL" shell wm density 2>/dev/null || true
  } >"$ARTIFACT_DIR/device-state.txt"
  if ! $KEEP_IME && test -n "$previous_ime" && test "$previous_ime" != "null"; then
    "$ADB" -s "$SERIAL" shell ime enable "$previous_ime" >/dev/null 2>&1 || true
    "$ADB" -s "$SERIAL" shell ime set "$previous_ime" >/dev/null 2>&1 || true
  fi
  printf 'Artifacts: %s\n' "$ARTIFACT_DIR"
  exit "$status"
}
trap collect_artifacts EXIT

cd "$ROOT_DIR"
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
"$ADB" -s "$SERIAL" install -r -t app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$SERIAL" install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$ADB" -s "$SERIAL" shell ime enable "$IME"
"$ADB" -s "$SERIAL" shell ime set "$IME"
"$ADB" -s "$SERIAL" logcat -c
"$ADB" -s "$SERIAL" shell am instrument -w -r "$RUNNER" | tee "$ARTIFACT_DIR/instrumentation.txt"
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_STATUS_CODE: -2' "$ARTIFACT_DIR/instrumentation.txt"; then
  printf 'Instrumentation reported test failures.\n' >&2
  exit 1
fi
grep -Eq '^OK \([0-9]+ tests?\)$' "$ARTIFACT_DIR/instrumentation.txt" || {
  printf 'Instrumentation did not report a successful test total.\n' >&2
  exit 1
}

# End-to-end smoke outside UiAutomation: focus the DONE field, press XiKey's
# bottom-right action key, then read the action recorded by the harness.
"$ADB" -s "$SERIAL" shell am start -W -n at.xikey.ime/.ImeTestHarnessActivity >/dev/null
SIZE_LINE=$("$ADB" -s "$SERIAL" shell wm size | tail -n 1 | tr -d '\r')
SIZE=${SIZE_LINE##*: }
WIDTH=${SIZE%x*}
HEIGHT=${SIZE#*x}
DENSITY_LINE=$("$ADB" -s "$SERIAL" shell wm density | tail -n 1 | tr -d '\r')
DENSITY=${DENSITY_LINE##*: }
FIELD_X=$((WIDTH / 2))
FIELD_Y=$((DENSITY * 132 / 160))
ACTION_X=$((WIDTH * 922 / 1000))
ACTION_Y=$((HEIGHT * 920 / 1000))
for _ in $(seq 1 20); do
  if "$ADB" -s "$SERIAL" shell uiautomator dump /sdcard/xikey-ready.xml >/dev/null 2>&1 &&
     "$ADB" -s "$SERIAL" shell grep -q DONE /sdcard/xikey-ready.xml; then
    break
  fi
  sleep 0.25
done
"$ADB" -s "$SERIAL" shell grep -q DONE /sdcard/xikey-ready.xml || {
  printf 'DONE field did not become ready.\n' >&2
  exit 1
}
"$ADB" -s "$SERIAL" shell ime enable "$IME" >/dev/null
"$ADB" -s "$SERIAL" shell ime set com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME >/dev/null
"$ADB" -s "$SERIAL" shell ime set "$IME" >/dev/null
"$ADB" -s "$SERIAL" shell input tap "$FIELD_X" "$FIELD_Y"
for _ in $(seq 1 20); do
  "$ADB" -s "$SERIAL" shell dumpsys input_method | grep -q 'mInputShown=true' && break
  sleep 0.25
done
"$ADB" -s "$SERIAL" shell dumpsys input_method | grep -q 'mInputShown=true' || {
  printf 'XiKey did not become visible for the DONE field.\n' >&2
  exit 1
}
sleep 1
"$ADB" -s "$SERIAL" shell input tap "$ACTION_X" "$ACTION_Y"
SMOKE_XML="$ARTIFACT_DIR/done-smoke.xml"
SMOKE_OK=false
for _ in $(seq 1 20); do
  if "$ADB" -s "$SERIAL" shell uiautomator dump /sdcard/xikey-done-smoke.xml >/dev/null 2>&1 &&
     "$ADB" -s "$SERIAL" pull /sdcard/xikey-done-smoke.xml "$SMOKE_XML" >/dev/null 2>&1 &&
     grep -q 'text="ACTION:DONE:6"' "$SMOKE_XML"; then
    SMOKE_OK=true
    break
  fi
  sleep 0.25
done
$SMOKE_OK || {
  printf 'XiKey DONE smoke did not record ACTION:DONE:6.\n' >&2
  exit 1
}
printf 'XiKey DONE smoke: ACTION:DONE:6\n'
