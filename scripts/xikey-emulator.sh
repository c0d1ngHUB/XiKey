#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-/home/m3kky/android-sdk}"
ADB="$SDK_ROOT/platform-tools/adb"
EMULATOR="$SDK_ROOT/emulator/emulator"
AVD="xikey_api35"
SERIAL="emulator-5554"
STATE_DIR="$ROOT_DIR/build/emulator-state"
PID_FILE="$STATE_DIR/emulator.pid"
LOG_FILE="$STATE_DIR/emulator.log"

usage() {
  printf '%s\n' \
    "Usage: scripts/xikey-emulator.sh <command> [args]" \
    "" \
    "Commands:" \
    "  start [--headless]       Start xikey_api35 and wait for Android" \
    "  reset [--headless]       Wipe xikey_api35, start, and wait" \
    "  stop                     Stop emulator-5554" \
    "  status                   Show boot, display, rotation, and IME state" \
    "  profile <name>           Apply phone|small|landscape|tablet|reset" \
    "  snapshot-save <name>     Save an emulator snapshot" \
    "  snapshot-load <name>     Restart from an emulator snapshot" \
    "  screenshot [path]        Save a PNG (default: build/emulator-state/)" \
    "  record [seconds] [path]  Record MP4, maximum 180 seconds" \
    "" \
    "Safety: every ADB mutation targets emulator-5554; physical devices are ignored."
}

require_tools() {
  test -x "$ADB" || { printf 'Missing adb: %s\n' "$ADB" >&2; exit 2; }
  test -x "$EMULATOR" || { printf 'Missing emulator: %s\n' "$EMULATOR" >&2; exit 2; }
  "$EMULATOR" -list-avds | grep -Fxq "$AVD" || { printf 'Missing AVD: %s\n' "$AVD" >&2; exit 2; }
}

connected() {
  "$ADB" -s "$SERIAL" get-state >/dev/null 2>&1
}

wait_for_boot() {
  "$ADB" -s "$SERIAL" wait-for-device
  local attempt value
  for attempt in $(seq 1 180); do
    value="$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if test "$value" = "1"; then
      "$ADB" -s "$SERIAL" shell input keyevent 82 >/dev/null 2>&1 || true
      printf 'Ready: %s (%s)\n' "$AVD" "$SERIAL"
      return 0
    fi
    sleep 1
  done
  printf 'Timed out waiting for %s\n' "$SERIAL" >&2
  return 1
}

start_emulator() {
  local wipe="$1"
  local mode="${2:-}"
  if connected; then
    printf 'Already running: %s\n' "$SERIAL"
    return 0
  fi
  mkdir -p "$STATE_DIR"
  local args=(-avd "$AVD" -port 5554 -no-audio -no-boot-anim -gpu swiftshader_indirect)
  test "$wipe" = "true" && args+=(-wipe-data -no-snapshot-load)
  test "$mode" = "--headless" && args+=(-no-window)
  "$EMULATOR" "${args[@]}" >"$LOG_FILE" 2>&1 &
  printf '%s\n' "$!" >"$PID_FILE"
  wait_for_boot
}

stop_emulator() {
  if connected; then
    "$ADB" -s "$SERIAL" emu kill >/dev/null
    for _ in $(seq 1 30); do
      connected || break
      sleep 1
    done
  fi
  rm -f "$PID_FILE"
  printf 'Stopped: %s\n' "$SERIAL"
}

apply_profile() {
  local profile="$1"
  case "$profile" in
    phone)
      "$ADB" -s "$SERIAL" shell wm size 720x1600
      "$ADB" -s "$SERIAL" shell wm density 320
      "$ADB" -s "$SERIAL" shell settings put system user_rotation 0
      ;;
    small)
      "$ADB" -s "$SERIAL" shell wm size 480x800
      "$ADB" -s "$SERIAL" shell wm density 240
      "$ADB" -s "$SERIAL" shell settings put system user_rotation 0
      ;;
    landscape)
      "$ADB" -s "$SERIAL" shell wm size 1600x720
      "$ADB" -s "$SERIAL" shell wm density 320
      "$ADB" -s "$SERIAL" shell settings put system accelerometer_rotation 0
      "$ADB" -s "$SERIAL" shell settings put system user_rotation 1
      ;;
    tablet)
      "$ADB" -s "$SERIAL" shell wm size 1600x2560
      "$ADB" -s "$SERIAL" shell wm density 320
      "$ADB" -s "$SERIAL" shell settings put system user_rotation 0
      ;;
    reset)
      "$ADB" -s "$SERIAL" shell wm size reset
      "$ADB" -s "$SERIAL" shell wm density reset
      "$ADB" -s "$SERIAL" shell settings put system accelerometer_rotation 1
      "$ADB" -s "$SERIAL" shell settings put system user_rotation 0
      ;;
    *) printf 'Unknown profile: %s\n' "$profile" >&2; exit 2 ;;
  esac
  "$ADB" -s "$SERIAL" shell settings put system accelerometer_rotation 0
  printf 'Applied profile: %s\n' "$profile"
}

require_tools
command="${1:-help}"
case "$command" in
  help|-h|--help) usage ;;
  start) start_emulator false "${2:-}" ;;
  reset) stop_emulator; start_emulator true "${2:-}" ;;
  stop) stop_emulator ;;
  status)
    connected || { printf 'Stopped: %s\n' "$SERIAL"; exit 1; }
    printf 'serial=%s\navd=%s\nboot_completed=%s\n' "$SERIAL" "$AVD" "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')"
    "$ADB" -s "$SERIAL" shell wm size
    "$ADB" -s "$SERIAL" shell wm density
    printf 'rotation=%s\ndefault_ime=%s\n' \
      "$("$ADB" -s "$SERIAL" shell settings get system user_rotation | tr -d '\r')" \
      "$("$ADB" -s "$SERIAL" shell settings get secure default_input_method | tr -d '\r')"
    ;;
  profile) connected; apply_profile "${2:?profile name required}" ;;
  snapshot-save) connected; "$ADB" -s "$SERIAL" emu avd snapshot save "${2:?snapshot name required}" ;;
  snapshot-load)
    stop_emulator
    mkdir -p "$STATE_DIR"
    "$EMULATOR" -avd "$AVD" -port 5554 -snapshot "${2:?snapshot name required}" -no-audio -gpu swiftshader_indirect >"$LOG_FILE" 2>&1 &
    printf '%s\n' "$!" >"$PID_FILE"
    wait_for_boot
    ;;
  screenshot)
    connected
    output="${2:-$STATE_DIR/xikey-$(date +%Y%m%d-%H%M%S).png}"
    mkdir -p "$(dirname "$output")"
    "$ADB" -s "$SERIAL" exec-out screencap -p >"$output"
    printf '%s\n' "$output"
    ;;
  record)
    connected
    seconds="${2:-15}"
    test "$seconds" -ge 1 && test "$seconds" -le 180 || { printf 'Duration must be 1..180 seconds\n' >&2; exit 2; }
    output="${3:-$STATE_DIR/xikey-$(date +%Y%m%d-%H%M%S).mp4}"
    mkdir -p "$(dirname "$output")"
    remote="/sdcard/xikey-screenrecord.mp4"
    "$ADB" -s "$SERIAL" shell screenrecord --time-limit "$seconds" "$remote"
    "$ADB" -s "$SERIAL" pull "$remote" "$output" >/dev/null
    "$ADB" -s "$SERIAL" shell rm -f "$remote"
    printf '%s\n' "$output"
    ;;
  *) usage >&2; exit 2 ;;
esac
