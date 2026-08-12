# XiKey

**Datensparsame Android-Tastatur mit lokaler Autovervollständigung für Vorarlberger Deutsch.**

XiKey ist eine eigenständige Android-IME (Input Method Editor) für Vorarlberger Deutsch und Englisch. Die App arbeitet vollständig lokal: Sie besitzt **keine Internetberechtigung**, keine Analytics und keinen Remote-Vorhersagepfad.

## Funktionen

- Deutsches **QWERTZ**-Layout für Vorarlberger Deutsch: `ä`, `ö`, `ü`, `ß` und `ẞ` per Shift.
- Englisches **QWERTY**-Layout; Wechsel direkt über `VBG` / `EN`.
- Zahlen-, Satzzeichen- und zweite Sonderzeichenebene.
- Lokale VoraLex-Autovervollständigung mit **3.850** kuratierten Dialektformen.
  - Beispiele: `Guata Morga`, `Guata Obed`, `g'hörig`, `Schtoa`.
  - Präfixsuche ist Groß-/Kleinschreibungs-unabhängig und erhält Dialekt-Apostrophe.
  - Ein Vorschlag ersetzt das aktuelle Wortfragment und fügt ein Leerzeichen an.
- Keine Vorschläge in Passwortfeldern; keine Texte oder Nutzungsdaten verlassen das Gerät.
- Startbildschirm mit direktem Link zu Androids Tastatur-Einstellungen und eingebautem Testfeld.

## Installieren und verwenden

### Fertige Debug-APK

Die geprüfte APK liegt hier:

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

Voraussetzungen: Android SDK Platform 35 und JDK 17+.

```bash
printf 'sdk.dir=/PFAD/ZUM/android-sdk\n' > local.properties
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

Ausgaben:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

Die Release-APK ist absichtlich **nicht signiert** und muss vor einer Distribution mit einem eigenen Keystore signiert werden.

## Architektur

- **Kotlin + Android Framework:** Eine schlanke `InputMethodService`, ohne Backend oder Netzwerkanbindung.
- **VoraLex als APK-Asset:** `app/src/main/assets/voralex_words.json` enthält die 3.850 produktiven, kuratierten Formen aus dem lokalen VoraLex-Datensatz.
- **Suggestion Engine:** Reine Kotlin-Präfixsuche; durch JVM-Tests abgesichert.
- **Lokal gespeicherte Einstellung:** Nur das gewählte Tastaturlayout (`VBG`/`EN`) wird via `SharedPreferences` gespeichert.

## Datenschutz und Sicherheit

- Manifest ohne `INTERNET`-Permission.
- Kein Telemetrie-, Cloud-, Analytics- oder Werbe-SDK.
- Vorschläge werden für Passwortvarianten deaktiviert.
- `allowBackup="false"`; weder Texte noch persönliches Wörterbuch werden exportiert.

## Lizenz

[GPL-3.0-or-later](LICENSE).
