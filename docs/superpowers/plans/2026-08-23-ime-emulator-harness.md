# XiKey IME Emulator Harness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a debug-only IME test activity and deterministic `xikey_api35` automation that exercises XiKey editor actions without touching the attached Redmi.

**Architecture:** A debug source-set activity exposes one real `EditText` per IME behavior and records editor-action callbacks in an observable status view. Espresso verifies field configuration; UIAutomator verifies XiKey across the app/IME process boundary. Shell entry points own emulator lifecycle, installation, IME selection, evidence capture, and restoration.

**Tech Stack:** Kotlin, Android Framework, Espresso 3.6.1, UIAutomator 2.3.0, Bash, Gradle, Android Emulator/ADB.

**Spec:** User request in Telegram on 2026-08-23 to implement both the `xikey_api35` automated workflow and a UIAutomator/Espresso harness.

## Global Constraints

- AVD is exactly `xikey_api35`.
- All automated ADB mutations target exactly `emulator-5554`; never the attached Redmi.
- Harness is debug-only and absent from release artifacts.
- Fields cover DONE, SEARCH, SEND, GO, NEXT, PREVIOUS, multiline, password, auto-shift/Caps-Lock, VoraLex suggestions, long-press, and backspace.
- Test runs capture screenshot, UI hierarchy, logcat, and instrumentation output.

---

### Task 1: Debug IME test activity

**Files:**
- Create: `app/src/debug/AndroidManifest.xml`
- Create: `app/src/debug/java/at/xikey/ime/ImeTestHarnessActivity.kt`
- Create: `app/src/androidTest/java/at/xikey/ime/ImeTestHarnessActivityTest.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: exported debug activity `at.xikey.ime/.ImeTestHarnessActivity`, stable view IDs, field content descriptions, and status text `ACTION:<LABEL>:<ACTION_ID>`.

- [ ] Write instrumentation tests asserting all field configurations and editor-action logging.
- [ ] Run `./gradlew :app:compileDebugAndroidTestKotlin` and verify RED because the activity and IDs do not exist.
- [ ] Implement the minimal debug activity and manifest.
- [ ] Run focused instrumentation tests and verify GREEN.

### Task 2: Cross-process XiKey UIAutomator test

**Files:**
- Create: `app/src/androidTest/java/at/xikey/ime/XiKeyEditorActionUiTest.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: debug activity IDs/content descriptions and XiKey action-key accessibility text.
- Produces: a device test that focuses DONE, invokes XiKey's action key, and verifies the callback without a newline.

- [ ] Write the UIAutomator test.
- [ ] Run it before XiKey is configured and verify RED for the expected missing XiKey action key.
- [ ] Configure XiKey on the emulator through the automation script.
- [ ] Run the test and verify GREEN.

### Task 3: Emulator lifecycle and test runner

**Files:**
- Create: `scripts/xikey-emulator.sh`
- Create: `scripts/run-emulator-tests.sh`
- Modify: `README.md`

**Interfaces:**
- Produces: `start`, `reset`, `stop`, `snapshot-save`, `snapshot-load`, and `status` lifecycle commands; one-command build/install/test/evidence runner.

- [ ] Implement strict serial/AVD guards and help output.
- [ ] Verify lifecycle status and help without mutating the Redmi.
- [ ] Reset/start `xikey_api35`, wait for boot, install APKs, enable/select XiKey, and run instrumentation.
- [ ] Capture PNG, XML, logcat, and test output under `build/emulator-artifacts/<timestamp>/`.
- [ ] Document exact commands and safety boundary.

### Task 4: Full verification

**Files:** none.

- [ ] Run unit tests, lint, debug/release builds, and connected instrumentation tests.
- [ ] Verify the release manifest does not contain `ImeTestHarnessActivity`.
- [ ] Verify `adb devices` still lists the Redmi and all automation commands targeted `emulator-5554`.
- [ ] Inspect git diff and commit with Conventional Commits.
