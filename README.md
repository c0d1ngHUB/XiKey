# XiKey

**Eine datensparsame Android-Tastatur für Vorarlberger Deutsch und Englisch.**

XiKey ist eine eigenständige Android-IME (Input Method Editor). Microsoft SwiftKey kann nicht um ein eigenes Vorarlberger Sprachmodell erweitert werden; XiKey schafft deshalb eine offene, lokale Alternative.

> **Frühe Basis:** Diese Version enthält die Android-IME-Registrierung, die zwei vorgesehenen Sprachen und eine minimal testbare Tastaturansicht. Sie ist noch kein SwiftKey-Ersatz.

## Grundsätze

- **Nur zwei Sprachen:** Vorarlberger Deutsch (`de-AT-vorarlberg`) und Englisch (`en`).
- **Lokal zuerst:** Wortschatz, Personalisierung und Vorhersage bleiben auf dem Gerät.
- **Keine Netzwerkberechtigung:** Die App übermittelt keine Tastatureingaben.
- **Offene Entwicklung:** GPL-3.0-or-later.

## Aktueller Funktionsumfang

- Alphabetisches **QWERTZ**-Layout für Vorarlberger Deutsch inklusive `ä`, `ö`, `ü` und `ß`.
- **QWERTY**-Layout für Englisch.
- Direkter Sprachwechsel mit `VBG` / `EN`.
- Umschalttaste und Löschen direkt in der dritten Buchstabenreihe sowie Leertaste und Eingabe in der Steuerzeile.
- Umschaltbare **Zahlen-/Sonderzeichenebene**: Ziffern sowie `@ # € % & - + ( ) /` und häufige Satzzeichen.

Wortvorhersagen folgen im nächsten Meilenstein.

## Roadmap

1. Zahlen-/Sonderzeichenebene und dauerhafte Spracheinstellung.
2. Kuratierter Vorarlberger Wortschatz mit Varianten und Frequenzen.
3. Lokale Kandidaten-/Autokorrektur-Engine und lernbares persönliches Lexikon.
4. Opt-in Import/Export des persönlichen Wörterbuchs.
5. Accessibility, Privacy-Audit, Hardware-Tests und F-Droid/Play-Store-Paketierung.

## Entwicklung

Voraussetzungen: Android SDK Platform 35 und JDK 17+.

```bash
printf 'sdk.dir=/PFAD/ZUM/android-sdk\n' > local.properties
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Die Debug-APK liegt anschließend unter `app/build/outputs/apk/debug/app-debug.apk`.

## Datenschutz

Android zeigt bei allen Drittanbieter-Tastaturen eine Systemwarnung, weil eine IME Eingaben verarbeiten kann. XiKey soll daher ohne Internetberechtigung, ohne Analytics und mit vollständig einsehbarem Quellcode betrieben werden. Passwortfelder werden in einem späteren Meilenstein bewusst ohne Vorhersage und ohne Lernen behandelt.

## Mitwirken

Dialekt-Wörter und Varianten sind willkommen. Bitte keine privaten Texte, Chats oder Adressdaten als Beispielmaterial einreichen. Für neue Wörter werden Herkunft, Region und Lizenz/Einwilligung dokumentiert.

## Lizenz

[GPL-3.0-or-later](LICENSE).
