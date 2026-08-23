# XiKey UI/UX Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Behebt die zehn Findings des XiKey-UI/UX-Reviews in priorisierter Reihenfolge und liefert eine auf physischem Android-Gerät prüfbare Tastatur.

**Architecture:** Verhaltenslogik wird aus dem großen `XiKeyInputMethodService` in kleine, rein testbare Kotlin-Modelle ausgelagert. Die Serviceklasse bleibt Android-Adapter für `InputConnection`, Rendering und Popups; Vorschlagsranking, Editoraktion, Shift-Zustand, Backspace-Timing, Beschriftungen und Setupstatus erhalten klare Schnittstellen.

**Tech Stack:** Kotlin, Android InputMethodService/View API, JUnit 4, Gradle Android Plugin.

**Spec:** `/home/m3kky/projects/XiKey/ui-ux-review-20260823/report.md`

## Global Constraints

- Keine Internetberechtigung, Analytics oder Cloud-Abhängigkeit hinzufügen.
- VBG und EN bleiben lokale Layouts; VBG behält direkte `ü`, `ö`, `ä`, `ß`-Tasten.
- Änderungen erfolgen test-first; jeder Regressionstest muss vor der Implementierung am erwarteten Verhalten scheitern.
- Debug- und Release-Build sowie vollständige Unit-Tests müssen grün sein.
- Physischer Smoke-Test erfolgt auf 360×800 dp und prüft alle im Review genannten Flows.

---

### Task 1: Editoraktionen und Enter-Accessibility

**Files:**
- Create: `app/src/main/java/at/xikey/ime/ImeActionSpec.kt`
- Create: `app/src/test/java/at/xikey/ime/ImeActionSpecTest.kt`
- Modify: `app/src/main/java/at/xikey/ime/XiKeyInputMethodService.kt`

**Interfaces:**
- Produces: `ImeActionSpec.from(imeOptions: Int): ImeActionSpec`
- Produces fields: `icon: String`, `contentDescription: String`, `editorAction: Int?`, `hideKeyboardAfterAction: Boolean`

- [ ] Write tests proving DONE/Search/Send/Go/Next/Previous map to matching icon, description and action.
- [ ] Write a test proving `IME_FLAG_NO_ENTER_ACTION`, NONE and UNSPECIFIED map to newline with no editor action.
- [ ] Run `./gradlew testDebugUnitTest --tests at.xikey.ime.ImeActionSpecTest` and verify compilation/test failure because the type is missing.
- [ ] Implement the minimal immutable action model.
- [ ] Replace unconditional Enter key events with `performEditorAction` for actionable specs and raw Enter only for newline specs.
- [ ] Drive visible icon and `contentDescription` from the same spec.
- [ ] Run focused and full unit tests.

### Task 2: Source-aware VoraLex ranking

**Files:**
- Modify: `app/src/main/java/at/xikey/ime/SuggestionWordLists.kt`
- Modify: `app/src/main/java/at/xikey/ime/XiKeyInputMethodService.kt`
- Modify: `app/src/test/java/at/xikey/ime/SuggestionWordListsTest.kt`

**Interfaces:**
- Produces: `SuggestionWordLists.suggestionsFor(language: PredictionLanguage, prefix: String, limit: Int = 3): List<String>`

- [ ] Add failing tests proving VBG returns up to two dialect hits before German fallback, deduplicates normalized hits, fills unused dialect slots with German, and leaves EN unchanged.
- [ ] Run the focused test and verify the expected source-order failure.
- [ ] Keep separate dialect, German and English engines and implement deterministic merge/deduplication.
- [ ] Change the service to call the new list-level API.
- [ ] Run focused and full unit tests.

### Task 3: Shift, Backspace, Accessibility and Symbolstatus

**Files:**
- Modify: `app/src/main/java/at/xikey/ime/KeyboardShiftController.kt`
- Modify: `app/src/test/java/at/xikey/ime/KeyboardShiftControllerTest.kt`
- Modify: `app/src/main/java/at/xikey/ime/BackspaceRepeatController.kt`
- Modify: `app/src/test/java/at/xikey/ime/BackspaceRepeatControllerTest.kt`
- Create: `app/src/main/java/at/xikey/ime/KeyboardAccessibility.kt`
- Create: `app/src/test/java/at/xikey/ime/KeyboardAccessibilityTest.kt`
- Modify: `app/src/main/java/at/xikey/ime/XiKeyInputMethodService.kt`

**Interfaces:**
- Produces: `ShiftState { OFF, AUTO, ONESHOT, CAPS_LOCK }` and `KeyboardShiftController.state`.
- Produces: `KeyboardAccessibility.shift(state)`, `language(language)`, `symbolPage(page)`.
- Backspace timing: immediate deletion, 350-ms initial delay, 90-ms repeat, 50-ms repeat after 1500 ms.

- [ ] Add a failing regression test: two real taps from AUTO activate Caps-Lock.
- [ ] Replace coupled booleans with explicit shift state while preserving one-shot and Caps behavior.
- [ ] Add failing timing tests for initial delay, normal repeat and acceleration.
- [ ] Implement scheduled deletion accounting without losing overdue deletions.
- [ ] Add failing accessibility tests for all shift states, current language and symbol page 1/2 versus 2/2.
- [ ] Bind dynamic descriptions, selected/activated state and page labels in the service.
- [ ] Run focused and full unit tests.

### Task 4: Narrow VBG touch-target mitigation

**Files:**
- Modify: `app/src/main/java/at/xikey/ime/XiKeyInputMethodService.kt`
- Modify: `app/src/test/java/at/xikey/ime/KeyboardLayoutTest.kt`

**Interfaces:**
- `KeyboardSurfaceMetrics.keyHeightDp = 48`
- `KeyboardSurfaceMetrics.keyVisualGapDp = 2`
- Button Views occupy the complete weighted cell; visual gaps are created with inset backgrounds rather than layout margins.

- [ ] Add failing metric tests for 48-dp height and 2-dp visual gap.
- [ ] Remove horizontal layout margins from alphabet/symbol Button Views so their tappable cell is not reduced from 32 to 24 dp.
- [ ] Apply visual spacing through inset drawables while retaining direct VBG umlaut keys.
- [ ] Build and inspect 360-dp rendering for clipping and row stability.

### Task 5: Compact horizontal long-press chooser

**Files:**
- Create: `app/src/main/java/at/xikey/ime/LongPressVariants.kt`
- Create: `app/src/test/java/at/xikey/ime/LongPressVariantsTest.kt`
- Modify: `app/src/main/java/at/xikey/ime/XiKeyInputMethodService.kt`

**Interfaces:**
- Produces: `LongPressVariants.forKey(key: String, shifted: Boolean): List<String>`.
- Android rendering uses a focusable horizontal `PopupWindow` anchored immediately above the pressed key.

- [ ] Add failing tests for variant order, shifted labels, unknown keys and single variants.
- [ ] Move variant data/label transformation into the tested model.
- [ ] Replace vertical `PopupMenu` with compact horizontal `PopupWindow` buttons.
- [ ] Ensure selection commits exactly one variant and applies one-shot/Caps state correctly.
- [ ] Run focused/full tests and physical long-press smoke test.

### Task 6: Status-aware polished launcher

**Files:**
- Create: `app/src/main/java/at/xikey/ime/ImeSetupStatus.kt`
- Create: `app/src/test/java/at/xikey/ime/ImeSetupStatusTest.kt`
- Modify: `app/src/main/java/at/xikey/ime/MainActivity.kt`

**Interfaces:**
- Produces: `ImeSetupStatus(enabled: Boolean, selected: Boolean)` and pure status/button copy helpers.
- `MainActivity.onResume()` re-reads enabled IMEs and `Settings.Secure.DEFAULT_INPUT_METHOD`, then rebuilds status UI.

- [ ] Add failing tests for pending, enabled and selected setup copy/state.
- [ ] Implement status model and Android status lookup without new permissions.
- [ ] Rebuild launcher with one-line header, short privacy benefit, numbered status-aware steps and compact test area.
- [ ] Keep the now-working `Gu` → `Guata Morga` demo.
- [ ] Run full tests, lint, debug/release builds and physical regression suite.

### Final verification

- [ ] Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`.
- [ ] Scan `git diff --check`, manifest permission changes and staged secret patterns.
- [ ] Install debug APK on the physical device and repeat all ten review reproductions plus Search/Send/Go/Next/Previous and TalkBack checks.
- [ ] Commit in Conventional Commit units, push branch, open PR, verify CI and read back PR scope.
