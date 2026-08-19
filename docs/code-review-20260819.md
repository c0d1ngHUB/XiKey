# XiKey Code-Review (v0.2.0, Commit 543de38)

**Datum:** 19. August 2026
**Scope:** Vollständige Quelltext-Inspektion aller 13 Kotlin-Dateien, 10 Test-Dateien, Build-Setup, CI, Manifest, Ressourcen und Assets. Read-only — keine Änderungen am Code vorgenommen.

---

## 🔴 Kritisch

### C1: Backspace hat kein haptisches Feedback und keinen Click-Sound

`XiKeyInputMethodService.kt:392-398`

```kotlin
btn.setOnClickListener { performKeyFeedback(btn); deleteOneCharacter() }
btn.setOnTouchListener { _, event ->
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> startBackspaceRepeat()
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopBackspaceRepeat()
    }
    true  // ← konsumiert alle Touch-Events
}
```

Der `onTouchListener` gibt `true` zurück und konsumiert damit alle Touch-Events. Der `setOnClickListener` feuert **nie** — er ist tote Code. `performKeyFeedback()` (Haptik + Sound) steht nur im Click-Listener und wird folglich beim Backspace **nicht aufgerufen**.

Jede andere Taste (Buchstaben, Symbole, Komma, Punkt, Enter) ruft `performKeyFeedback` auf. Nur Backspace nicht.

**Fix:** `performKeyFeedback(btn)` in `startBackspaceRepeat()` vor `deleteOneCharacter()` aufrufen.

### C2: `XiKeyInputMethodService` hat 0 Tests

Die größte und komplexeste Datei (567 Zeilen) ist komplett ungetestet. Die 10 Test-Dateien decken nur die Controller und Hilfsklassen ab. Die gesamte Rendering-Logik (`updateKeyboard`, `showAlphabeticRows`, `showSymbolRows`, `configureKeyButton`, `handleLongPress`, `isSensitiveInput`, `enterIcon`) hat keine Testabdeckung.

### C3: Suggestion-Engine ist O(n) pro Tastendruck

`DialectSuggestionEngine.kt:21-25`

```kotlin
return entries.asSequence()
    .filter { it.lookup.startsWith(query) }  // linear scan über alle Einträge
    .map(Entry::word)
    .take(limit)
    .toList()
```

Die Wortliste für Vorarlberger Deutsch kombiniert Dialekt (3.850) + Hochdeutsch (355.987) = **359.837 Einträge**. Bei jedem Tastendruck wird ein linearer Scan über alle 360K Einträge ausgeführt. Die Liste ist zwar sortiert (`sortedWith(compareBy { it.lookup })`), aber die Suche nutzt keine binäre Suche.

Auf einem Mobiltelefon (Redmi 15c) kann das spürbare Eingabelatenz verursachen, besonders bei kurzen Präfixen wie "a" oder "e", die zehntausende Treffer matchen.

**Fix:** `Collections.binarySearch` nutzen, um den ersten Treffer zu finden, dann `limit` Einträge ab dort nehmen.

---

## 🟡 Warnung

### W1: `roundedBackground()` allokiert bei jedem Tastendruck neu

`XiKeyInputMethodService.kt:550`

```kotlin
private fun roundedBackground(color: Int): GradientDrawable = GradientDrawable().apply { ... }
```

`updateKeyboard()` wird bei jedem Tastendruck aufgerufen und konfiguriert alle sichtbaren Buttons neu. Jeder Aufruf von `configureKeyButton`, `configureSymbolButton`, `configureShiftButton` usw. erzeugt ein neues `GradientDrawable`. Bei 30+ sichtbaren Tasten sind das 30+ Objektallokationen pro Tastendruck.

Der Kommentar in der Klasse sagt "no Views are created or destroyed after the initial layout" — aber `GradientDrawable` wird trotzdem bei jedem Update neu erzeugt.

**Fix:** `GradientDrawable` einmal pro `KeyKind` cachen und nur `setColor` beim Update aufrufen.

### W2: `onUpdateSelection` feuert `refreshSuggestions()` bei jeder Cursorbewegung

`XiKeyInputMethodService.kt:121-131`

```kotlin
override fun onUpdateSelection(...) {
    if (oldSelStart != newSelStart || oldSelEnd != newSelEnd) refreshSuggestions()
}
```

Jede Cursorbewegung (auch Pfeiltasten, Tap im Text) löst `refreshSuggestions()` → `currentComposingWord()` → `getTextBeforeCursor(64, 0)` (IPC zum anderen Prozess) → `suggestionsFor()` (O(n) Scan) aus. Das ist unnötig, wenn sich das aktuelle Wort nicht geändert hat.

**Fix:** Das composing word zwischenspeichern und nur bei Änderung refreshen.

### W3: Keine Auto-Großschreibung am Satzanfang

Die Tastatur erkennt nicht, ob der Cursor nach einem `.` oder am Textanfang steht, und schaltet Shift nicht automatisch ein. Gboard und alle Standard-Tastaturen tun dies. Der Benutzer muss Shift manuell antippen.

### W4: `styles.xml` definiert `XiKeyButton`-Styles, die nie verwendet werden

`styles.xml` definiert `XiKeyButton`, `XiKeyButton.Normal` und `XiKeyButton.Accent`. Die Buttons werden aber programmatisch mit `Button(this).apply { ... }` erzeugt und konfiguriert — die Styles werden **nirgendwo** referenziert. Die `textSize` wird direkt im Code gesetzt (`18f` bzw. `14f`), die `minWidth`/`minHeight` auch. Die Styles sind tote Code.

### W5: `handleLongPress` committet immer nur die erste Variante

`XiKeyInputMethodService.kt:496-507`

```kotlin
val variants = longPressMap[key] ?: return false
if (variants.isEmpty()) return false
val variant = variants[0]  // ← immer die erste Variante
```

Bei langem Drücken von "a" wird immer "ä" committet. Die anderen Varianten (à, á, â, æ) sind für den Benutzer **nicht erreichbar**. Der Kommentar sagt "future: show popup chooser" — aber so wie es ist, sind 4 von 5 Varianten unbrauchbar.

### W6: `deleteSurroundingText(1, 0)` unterstützt keine Surrogate/Combining Characters

`XiKeyInputMethodService.kt:427`

```kotlin
private fun deleteOneCharacter() {
    currentInputConnection?.deleteSurroundingText(1, 0)
    ...
}
```

`deleteSurroundingText(1, 0)` löscht genau 1 Java-`char` (16-bit). Bei Emoji (Surrogate-Paare) oder kombinierten Diakritika würde dies nur einen Teil des Zeichens löschen und ein kaputtes Zeichen zurücklassen. Für eine Vorarlberger Tastatur mit NFC-normalisierten Umlauten ist das in der Praxis sicher, aber für Emoji oder internationale Texte fehleranfällig.

### W7: Setup-Activity ist Light-Theme, Keyboard ist Dark-Theme

`styles.xml:3`: `Theme.Material.Light.NoActionBar`
`colors.xml`: Keyboard-Hintergrund `#FF14171B` (sehr dunkel)

Die Setup-Activity ist hell, die Tastatur dunkel. Kein `nightMode`-Switch. Visuell inkonsistent — der Hindsight-Kontext bestätigt dies als bekannte Lücke.

### W8: `enterIcon()` fehlt `IME_ACTION_PREVIOUS`

`XiKeyInputMethodService.kt:473-480`

```kotlin
when (currentImeOptions and EditorInfo.IME_MASK_ACTION) {
    EditorInfo.IME_ACTION_GO -> "→"
    EditorInfo.IME_ACTION_SEARCH -> "🔍"
    EditorInfo.IME_ACTION_SEND -> "➤"
    EditorInfo.IME_ACTION_NEXT -> "⇥"
    EditorInfo.IME_ACTION_DONE -> "✓"
    else -> "↵"
}
```

`IME_ACTION_PREVIOUS` fällt auf `↵` durch. Funktioniert, aber visuell inkonsistent — `NEXT` hat `⇥`, `PREVIOUS` sollte `⇤` bekommen.

---

## 🟢 Gut

| # | Befund |
|---|--------|
| G1 | **Keine INTERNET-Permission** im Manifest — lokale Tastatur, keine Daten verlassen das Gerät |
| G2 | **`allowBackup="false"`** — kein Backup sensibler Tastatureingaben |
| G3 | **Password-Felder erkannt** — `isSensitiveInput` deaktiviert Vorschläge für `TYPE_TEXT_VARIATION_PASSWORD`, `VISIBLE_PASSWORD`, `WEB_PASSWORD` |
| G4 | **Controller-Architektur** — Shift, Page, Language, Backspace sind saubere, testbare State Machines mit injizierbaren Time-Providern |
| G5 | **Shift/Caps-Lock** — Double-Tap-Fenster 300ms, korrekt getestet mit gefälschter Clock |
| G6 | **View-Recycling** — Row-Pools werden einmal erzeugt, nur Labels/Listener werden aktualisiert |
| G7 | **NFC-Normalisierung** in der Suggestion-Engine — korrekt für deutsche Umlaut-Codepoints |
| G8 | **ComposingWord** — erkennt Dialekt-Apostrophe (`g'hörig`) und stoppt an Leerzeichen/Satzzeichen |
| G9 | **SharedPreferences** speichern nur das Sprach-Tag, keine Texteingaben oder Wörterbücher |
| G10 | **CI-Pipeline** — 3 Jobs (verify, instrumented, release), alle grün auf main |
| G11 | **Signing** — Keystore via GitHub Secrets, base64-decoded in CI, `.jks` in `.gitignore` |
| G12 | **ProGuard** — `isMinifyEnabled=true`, korrekte Default-Rules, keine Reflection im Code |
| G13 | **Adaptive Icon** — `ic_launcher.xml` mit `ic_launcher_foreground.xml` (Vector-Drawable "X") |
| G14 | **GPL-3.0-Lizenz** — LICENSE-Datei vorhanden, README verweist korrekt |
| G15 | **Test-Coverage der Controller** — 10 Unit-Tests, alle Controller und Hilfsklassen abgedeckt |

---

## Statistik

| Metrik | Wert |
|--------|------|
| Kotlin-Dateien (main) | 13 |
| Kotlin-Dateien (test) | 10 |
| Zeilen (main) | ~1.100 |
| Zeilen (test) | ~450 |
| Test-Verhältnis | ~41% (main:test) |
| Ungetestete Dateien | 1 (`XiKeyInputMethodService`, 567 Zeilen) |
| Wortlisten-Größe | 3.850 (VoraLex) + 355.987 (Deutsch) + 102.485 (Englisch) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## Priorisierte Empfehlungen

1. **C1** — Haptik/Sound bei Backspace (1-Zeilen-Fix)
2. **C3** — Binary Search in Suggestion-Engine (Performance)
3. **C2** — Mindestens Integration-Tests für `XiKeyInputMethodService`
4. **W1** — `GradientDrawable` cachen
5. **W2** — Suggestion-Refresh nur bei Wortänderung
6. **W5** — Long-Press Popup-Chooser für Akzentvarianten
7. **W3** — Auto-Großschreibung am Satzanfang
8. **W7** — Dark-Theme für Setup-Activity
9. **W8** — `IME_ACTION_PREVIOUS` Icon

---

*Read-only Review — keine Änderungen am System vorgenommen.*