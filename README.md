# XiKey

**Datensparsame Android-Tastatur mit lokaler Autovervollständigung für Vorarlberger Deutsch.**

XiKey ist eine eigenständige Android-IME (Input Method Editor) für Vorarlberger Deutsch und Englisch. Die App arbeitet vollständig lokal: Sie besitzt **keine Internetberechtigung**, keine Analytics und keinen Remote-Vorhersagepfad.

## Funktionen

- Deutsches **QWERTZ**-Layout für Vorarlberger Deutsch: `ä`, `ö`, `ü`, `ß` und `ẞ` per Shift.
- Englisches **QWERTY**-Layout; Wechsel direkt über `VBG` / `EN`.
- Zahlen-, Satzzeichen- und zweite Sonderzeichenebene.
- Lokale VoraLex-Autovervollständigung mit **3.867** exportierbaren Dialektformen aus den aktuell produktiven, `model_approved`-Quellen.
  - Beispiele: `Guata Morga`, `Guata Obed`, `g'hörig`, `Schtoa`.
  - Präfixsuche ist Groß-/Kleinschreibungs-unabhängig und erhält Dialekt-Apostrophe.
  - Ein Vorschlag ersetzt das aktuelle Wortfragment und fügt ein Leerzeichen an.
  - Das Asset wird deterministisch aus dem VoraLex-DB-Pfad exportiert: `exportable_forms()` liefert nur `model_approved`-Formen, dedupliziert nach Oberfläche und sortiert nach `normalized`/`surface`.
  - Der Sync ist als `scripts/sync_voralex_asset.py` mit `--check` abbildbar; im CI/Lokal kann damit Drift gegen den VoraLex-Export erkannt werden.
- Keine Vorschläge in Passwortfeldern; keine Texte oder Nutzungsdaten verlassen das Gerät.
- Startbildschirm mit direktem Link zu Androids Tastatur-Einstellungen und eingebautem Testfeld.

## Installieren und verwenden

### Fertige Debug-APK

Nach einem erfolgreichen Debug-Build liegt die lokal erzeugte APK hier (Build-Artefakte sind nicht im Checkout enthalten):

```text
app/build/outputs/apk/debug/app-debug.apk
```

Auf einem per USB verbundenen Android-Gerät installieren:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Dann:

1. **XiKey** öffnen.
2. **Tastatur aktivieren** drücken und XiKey in Android aktivieren.
3. Zur App zurückkehren, **XiKey als Tastatur wählen** drücken.
4. Das Testfeld antippen oder eine andere App mit Texteingabe öffnen.
5. Für einen Vorschlag `Gu` tippen und etwa **Guata Morga** auswählen.

Android zeigt für jede Drittanbieter-Tastatur einen Systemhinweis, weil IMEs Texteingaben verarbeiten können. XiKey benötigt trotzdem kein Netzwerk und überträgt keine Eingaben.

## Entwicklung

Voraussetzungen: Android SDK Platform 35 und JDK 17+; die [CI](.github/workflows/android.yml) verwendet JDK 21. Der eingecheckte Gradle-Wrapper lädt Gradle 8.11.1; beim ersten Build werden auch Android-/Maven-Abhängigkeiten geladen. Die App unterstützt Android 8.0/API 26 und höher.

```bash
printf 'sdk.dir=/PFAD/ZUM/android-sdk\n' > local.properties
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

### VoraLex-Asset reproduzieren

Die Wortliste ist bereits im Checkout enthalten; für den normalen Android-Build wird kein VoraLex-Checkout benötigt. Für die Driftprüfung Python 3.11+ und das VoraLex-Repository als benachbarten Checkout oder expliziten Pfad bereitstellen:

```bash
python3 scripts/sync_voralex_asset.py --voralex-root ../VoraLex --check
# Nach einer bewusst geprüften Wörterbuchänderung neu erzeugen:
python3 scripts/sync_voralex_asset.py --voralex-root ../VoraLex
```

Alternativ setzt `VORALEX_ROOT` den Pfad. Der Sync liest vier Datensätze (`vorarlberg_v1.json`, `vorarlberg_core_v1.json`, `vorarlberg_regional_expansion_v1.json`, `vorarlberg_voice_v1_curated.json`) in eine temporäre SQLite-Datenbank ein. `--check` vergleicht die exakten JSON-Bytes und schreibt das Asset nicht. Dieser Check ist derzeit kein Schritt der Android-CI; bei Wörterbuchänderungen separat ausführen.

### Automatisierte IME-Tests auf `xikey_api35`

Der Debug-Build enthält eine eigene `ImeTestHarnessActivity` mit Feldern für `DONE`, `SEARCH`, `SEND`, `GO`, `NEXT`, `PREVIOUS`, Mehrzeilentext, Passwort, Auto-Shift/Caps-Lock, VoraLex sowie Long-Press/Backspace. Die Activity wird ausschließlich im Debug-Source-Set gebaut und ist nicht Bestandteil der Release-App.

Die Skripte verwenden `ANDROID_SDK_ROOT` (Fallback: `/home/m3kky/android-sdk`) für `adb` und `emulator`. Dafür muss ein AVD namens `xikey_api35` mit API-35-Systemimage bereits existieren; die Skripte erstellen es nicht. Beispiel nach Installation der Android-Command-Line-Tools:

```bash
export ANDROID_SDK_ROOT=/PFAD/ZUM/android-sdk
sdkmanager "platforms;android-35" "platform-tools" "emulator" "system-images;android-35;google_apis;x86_64"
avdmanager create avd --name xikey_api35 --package "system-images;android-35;google_apis;x86_64" --device pixel_2
```

`--reset` löscht die Daten dieses AVD. Die GitHub-Instrumentierung verwendet separat einen API-30-Emulator; der lokale API-35-Runner ist ein eigener Abnahmepfad.

```bash
# Vollständiger Lauf: Build, Installation, XiKey-Auswahl, Tests und Beweisartefakte
scripts/run-emulator-tests.sh

# Reproduzierbarer Lauf ab einem leeren AVD, optional ohne Fenster
scripts/run-emulator-tests.sh --reset --headless

# Emulator-Lebenszyklus und Anzeigeprofile
scripts/xikey-emulator.sh status
scripts/xikey-emulator.sh profile phone       # 720×1600, 320 dpi
scripts/xikey-emulator.sh profile small
scripts/xikey-emulator.sh profile landscape
scripts/xikey-emulator.sh profile tablet
scripts/xikey-emulator.sh profile reset
scripts/xikey-emulator.sh snapshot-save clean-xikey
scripts/xikey-emulator.sh snapshot-load clean-xikey
scripts/xikey-emulator.sh screenshot
scripts/xikey-emulator.sh record 15
```

Alle ADB-Befehle der Skripte adressieren explizit `emulator-5554`. Ein gleichzeitig angeschlossenes physisches Gerät wird nicht verändert. Der Test-Runner stellt nach dem Lauf standardmäßig die zuvor ausgewählte IME wieder her; `--keep-ime` lässt XiKey ausgewählt.

Die Artefakte liegen unter `build/emulator-artifacts/<Zeitstempel>/`:

- `instrumentation.txt`
- `screenshot.png`
- `window.xml`
- `logcat.txt`
- `device-state.txt`

Ausgaben:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

Bei lokaler Signierung ist `XIKEY_STORE_FILE` ein Dateipfad zum Keystore (Gradle löst relative Pfade im `app/`-Projekt auf), kein base64-Inhalt. Die übrigen drei Variablen liefern Passwort und Alias. Signaturdateien und Zugangsdaten bleiben außerhalb Git.

Die Release-APK ist absichtlich **nicht signiert**, sofern keine Signatur-Secrets konfiguriert sind. In GitHub Actions wird der Release-Build automatisch mit hinterlegten Secrets signiert, sobald `XIKEY_STORE_FILE` (base64-kodierter Keystore), `XIKEY_STORE_PASSWORD`, `XIKEY_KEY_ALIAS` und `XIKEY_KEY_PASSWORD` als Repository-Secrets gesetzt sind. Ohne diese Secrets wird eine unsignierte Release-APK gebaut.

## Screenshots

> Screenshots folgen in einer kommenden Veröffentlichung. Bis dahin kann die Debug-APK direkt auf einem Gerät installiert und ausprobiert werden (siehe oben).

## Architektur

- **Kotlin + Android Framework:** Eine schlanke `InputMethodService`, ohne Backend oder Netzwerkanbindung.
- **VoraLex als APK-Asset:** `app/src/main/assets/voralex_words.json` enthält 3.867 exportierbare, kuratierte Formen aus den aktuell produktiven `model_approved`-VoraLex-Quellen.
- **Suggestion Engine:** Reine Kotlin-Präfixsuche; durch JVM-Tests abgesichert.
- **Lokale Persistenz:** Sprachpräferenz (`VBG`/`EN`) sowie ausschließlich lokal gelernte Token und Häufigkeiten werden via `SharedPreferences` gespeichert.

## Datenschutz und Sicherheit

- Manifest ohne `INTERNET`-Permission.
- Kein Telemetrie-, Cloud-, Analytics- oder Werbe-SDK.
- Vorschläge werden für Passwort- und Zahlen-PIN-Felder deaktiviert.
- Backup-Regeln schließen `xikey_preferences` von Cloud Backup und Geräteübertragung aus.

## Lizenz

[GPL-3.0-or-later](LICENSE).
