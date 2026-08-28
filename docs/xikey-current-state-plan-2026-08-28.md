# XiKey Bestandsaufnahme und technischer Plan

Stand: 2026-08-28
Arbeitsgrundlage: Workspace `/home/m3kky/projects/XiKey`, Branch `main`, HEAD `6ad35ad`.

## 1. Git- und Änderungszustand

`git status --short` zeigt ausschließlich bereits vorhandene, nicht von dieser Bestandsaufnahme veränderte Arbeit:

- geändert: `app/src/main/java/at/xikey/ime/XiKeyInputMethodService.kt`
- neu/untracked: `InputTypeClassifier.kt`, `LocalSuggestionModel.kt`, `SharedPreferencesLearningStore.kt`, `SuggestionInsertionPlanner.kt`
- neu/untracked: die drei zugehörigen Tests (`InputTypeClassifierTest`, `LocalPredictionModelTest`, `SuggestionInsertionPlannerTest`)
- neu/untracked: `ui-ux-review-20260823/` mit Reviewbericht, Screenshots und UI-XMLs

Die Änderung im Service ersetzt `SuggestionWordLists` durch `LocalPredictionModel`, bindet den SharedPreferences-Store an und verwendet `CursorContext`/`SuggestionInsertionPlanner` bei Vorschlagsannahme und Aktualisierung. Diese Dateien waren bei Beginn bereits vorhanden; es wurde nichts zurückgesetzt oder überschrieben.

## 2. Bestehende APIs und Datenfluss

### Tokenabschluss

- `XiKeyInputMethodService.commitAndRefresh(text)` ist der Android-Adapter für normale Tasten-/Satzzeichen-Eingabe.
- Bei `text == " "`, nicht-sensiblem Feld und einem nichtleeren `CursorContext.composingWord` wird vor dem Commit `suggestions.learn(language, previousWord, composingWord)` aufgerufen.
- Danach folgt `InputConnection.commitText(text, 1)`, Auto-Shift-Kontextaktualisierung und `refreshSuggestions()`.
- `CursorContext.fromText(textBeforeCursor())` ermittelt den Token direkt vor dem Cursor, den vorherigen Token und die Wortgrenze. `ComposingWord.beforeCursor` akzeptiert Buchstaben sowie `'`/`’`; `WordBoundaries` behandelt Whitespace und ausgewählte Satzzeichen als Grenzen und erhält Apostrophe/Bindestriche im vorherigen Wort.

### Vorschlagsannahme

- `updateSuggestionBar()` bindet jeden sichtbaren Button an `acceptSuggestion(suggestion)`.
- `acceptSuggestion` liest den Cursor-Kontext, ruft `SuggestionInsertionPlanner.plan` auf, löscht `context.composingWord.length` Zeichen mit `deleteSurroundingText`, committed `suggestion + " "` und lernt bei erlaubtem Feld die komplette Phrase über `learnPhrase`.
- Anschließend werden Vorschläge gelöscht und das Keyboard neu gerendert.
- Bei leerem Composing-Token, aber gültigem Wortgrenzen-Kontext, ist `deleteCount == 0`; der nächste statische oder gelernte Wortvorschlag wird angehängt.

### Modell und Ranking

`LocalPredictionModel.suggestionsFor(language, context, limit)` hat zwei Pfade:

1. Präfix vorhanden: sprachgefilterte gelernte Wörter, sortiert nach Count, Last-Seen und Key, danach statische `SuggestionWordLists`-Treffer; NFC/lowercase-normalisierte Schlüssel deduplizieren.
2. Kein Präfix, Wortgrenze nach vorherigem Wort: gelernte Bigramme vor dem statischen `PhraseIndex`; sonst leer.

Das Modell ist Android- und netzwerkfrei. Die statischen Quellen bleiben getrennt: Dialekt und Standarddeutsch werden in `SuggestionWordLists` für VBG zusammengeführt, Englisch verwendet ausschließlich die englische Liste.

## 3. Sprachkontext, Normalisierung und statische Daten

- Unterstützte Sprachen sind ausschließlich `PredictionLanguage.VORARLBERG_GERMAN` (`de-AT-vorarlberg`) und `ENGLISH` (`en`).
- `KeyboardLanguageController` verwaltet den aktuellen Wert; `KeyboardLanguagePreference` serialisiert ihn für die bestehende Sprachpräferenz.
- Der Service speichert das Tag in derselben `SharedPreferences`-Datei und erstellt das Modell mit `voralex_words.json`, `german_words.json` und `english_words.json`.
- `voralex_words.json` wird aus dem aktuellen VoraLex-DB-Pfad exportiert; der Export enthält nur `model_approved`-Formen, dedupliziert Oberflächen deterministisch und enthält aktuell 3.867 Einträge.
- `LocalPredictionModel.normalized` und `DialectSuggestionEngine.normalized` wenden `Normalizer.Form.NFC`, `trim()` und `lowercase(Locale.ROOT)` an. Anzeigeformen bleiben unverändert; bei erneut gelerntem Schlüssel wird die zuletzt gelernte Display-Schreibweise verwendet.
- NFC/lowercase gilt für Lookup, Deduplizierung, Lernschlüssel und Übergangsschlüssel, nicht für die sichtbare Ausgabe.
- Das produktive VoraLex-Asset enthält nachweislich `Guata Morga`, `Guata Obed` und weitere Dialektformen. Die eingebauten Phrase-Konstanten werden für Wort-für-Wort-Folgevorhersage genutzt; sie erweitern derzeit nicht den Präfixindex von `baseSuggestions`.

## 4. Sensible Felder und Datenschutz

- `onStartInput` setzt `suggestionsAllowed = !isSensitiveInput(attribute)`.
- `InputTypeClassifier` maskiert die Variation mit `0x00000ff0` und sperrt Passwort, sichtbares Passwort und Web-Passwort (`0x80`, `0x90`, `0xe0`).
- Bei gesperrten Feldern werden Vorschläge nicht berechnet und weder `learn` noch `learnPhrase` aufgerufen.
- `SharedPreferencesLearningStore` speichert ausschließlich aggregierte Wörter/Übergänge, Counts und Sequenznummern unter `local_prediction_learning_v1`; er speichert keinen Editor-Text und hat keine Android-unabhängige Abhängigkeit im Modell selbst.
- Das Manifest besitzt keine `INTERNET`-Permission; die IME nutzt nur die erforderliche `BIND_INPUT_METHOD`-Service-Deklaration. `allowBackup="false"` ist gesetzt.

## 5. Teststruktur und ausgeführte Prüfung

- JVM-Unit-Tests liegen unter `app/src/test/java/at/xikey/ime` und decken Sprachpräferenz/-controller, Layout, Shift, Backspace-Timing, Accessibility, IME-Aktionen, VoraLex-Engine/-Merge, Setupstatus/-anleitung, Cursor-/Lernmodell, Insertionsplanung und Sensitive-Input-Klassifikation ab.
- Android-/IME-Harness und instrumentierte Tests liegen getrennt im Debug-/Android-Testpfad; der README beschreibt `scripts/run-emulator-tests.sh` für den Emulator.
- Ausgeführt: `./gradlew testDebugUnitTest --offline` — `BUILD SUCCESSFUL`.
- Zusätzlich geprüft: `git diff --check` — ohne Ausgabe/Fehler.
- Diese Bestandsaufnahme hat keine funktionale Produktionscodeänderung vorgenommen.

## 6. Konflikte bzw. offene Punkte gegenüber dem verbindlichen Design

1. **Android-unabhängige Persistenz:** Das Zielbild verlangt eine klare Persistenzschnittstelle. `LearningStore` erfüllt dies bereits; `SharedPreferencesLearningStore` ist der Android-Adapter. Der aktuelle Adapter schreibt synchron per `apply()` auf dem Aufrufpfad. Für spätere Ausbaustufen sollte die Schnittstelle unverändert bleiben und nur ein asynchroner Adapter bzw. eine begrenzte Snapshot-Größe ergänzt werden.
2. **VBG-Onboarding:** Das Review verlangt, dass `Gu` zuverlässig `Guata Morga` anbietet. Das Asset enthält den Treffer und die aktuelle `SuggestionWordLists`-Priorisierung reserviert zwei Dialektplätze. Die neu eingeführten Lern-/Phrasepfade dürfen diese Reihenfolge nicht verdrängen. Ein Regressionstest gegen die echte Asset-Menge bzw. ein expliziter statischer Smoke-Test ist nötig.
3. **Phraseindex vs. Präfixindex:** Die eingebauten Phrase-Konstanten sind nur im Folgewortpfad sichtbar. Falls das Design alle beworbenen Phrasen schon nach `Gu` liefern will, müssen die Konstanten in die statische Dialektquelle des Präfixpfads, ohne die Sprachtrennung aufzugeben.
4. **Kontextgrenzen:** `WordBoundaries.boundaryChars` ist eine explizite, begrenzte Menge. Unicode-/weitere Satzzeichen und Cursorpositionen innerhalb von Text sind nicht vollständig modelliert. Änderungen sollten über reine JVM-Tests am `CursorContext` abgesichert werden.
5. **Lernen bei Vorschlagsannahme:** Eine Mehrwortannahme lernt jedes Token und die Übergänge. Das ist designkonform für lokale Bigramme, muss aber weiterhin an `suggestionsAllowed` und die vorhandene Sprach-ID gebunden bleiben.
6. **Uncommittete Änderungsgrenze:** Die neuen Dateien sind untracked und der Service ist modifiziert; nachgelagerte Implementierungsaufgaben dürfen diese Arbeit nicht verwerfen. Besonders `XiKeyInputMethodService.kt` ist ein Integrations-Hotspot, weil UI-, InputConnection-, Shift-, Vorschlags- und Accessibility-Änderungen dort zusammenlaufen.

## 7. Umsetzbarer Plan und Persistenzschnittstelle

1. Vorhandene Lernmodell-Änderungen als fachliche Basis beibehalten; zuerst JVM-Regressionstests für Cursorgrenzen, VBG-`Gu`-Demo, Sprachisolierung, Passwort-Gating und Reload ergänzen.
2. `LearningStore` als einzige Modellgrenze stabilisieren:
   - `load(): LearningSnapshot` liefert bei fehlender/korrupt gespeicherter Datenquelle ein leeres Snapshot.
   - `save(snapshot: LearningSnapshot)` erhält nur bereits validierte, begrenzte Aggregate.
   - Modell darf Android weder importieren noch `SharedPreferences` kennen.
   - Android bindet genau einen `SharedPreferencesLearningStore` an; Tests verwenden `InMemoryLearningStore` oder `NoOpLearningStore`.
3. Validierung/Normalisierung beim Restore zentralisieren: Sprache nur aus `PredictionLanguage`, Schlüssel NFC/lowercase, Counts/Sequenz nicht negativ, Größenlimits pro Sprache/Typ erzwingen; beschädigte Einträge ignorieren statt Vorhersage zu blockieren.
4. Service-Integration unverändert schmal halten: Cursor lesen, Plan ausführen, Modell lernen/abfragen, View-Zustand aktualisieren. Keine Persistenz- oder Rankinglogik in `XiKeyInputMethodService` verschieben.
5. Mit `testDebugUnitTest`, anschließend Debug-/Release-Build und dem bestehenden Emulator-/physischen IME-Smoke-Test verifizieren. Dabei insbesondere `Gu`-Vorschlag, Annahme mit genau einem Leerzeichen, Folgewort nach Boundary, Passwortfeld ohne Lernen und Sprachwechsel prüfen.

### Empfohlene abstrakte Schnittstelle

```kotlin
interface LearningStore {
    fun load(): LearningSnapshot
    fun save(snapshot: LearningSnapshot)
}

class LocalPredictionModel(
    dialectWords: Collection<String>,
    germanWords: Collection<String>,
    englishWords: Collection<String>,
    store: LearningStore = NoOpLearningStore,
)
```

`LearningSnapshot` bleibt ein reines Datenobjekt aus `LearnedWord`, `LearnedTransition` und `sequence`. Damit kann Android später durch eine andere lokale Datei-/DB-Implementierung ersetzt werden, ohne Ranking, Datenschutz-Gating oder Service-UI zu verändern.
