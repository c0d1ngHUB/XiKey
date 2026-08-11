# Tastenlayout: Vergleich und Empfehlung für XiKey

**Stand:** 11. August 2026  
**Ziel:** Ein vertrautes, deutschsprachiges Android-Layout als Basis für Vorarlberger Deutsch, ohne die lokale und datensparsame Ausrichtung von XiKey zu verändern.

## Untersuchte Referenzen

- FlorisBoard legt für Deutsch QWERTZ mit `ü` in Zeile 1, `ö` und `ä` in Zeile 2 sowie `ß` am Ende der dritten Buchstabenzeile fest.[1]
- AnySoftKeyboard verwendet ebenfalls diese deutsche QWERTZ-Reihenfolge und ordnet der dritten Reihe zusätzlich Shift links sowie Löschen rechts zu.[5]
- Für Symbole hat FlorisBoard eine eigene westliche Ebene mit `@`, `#`, Währung, `%`, `&`, `-`, `+`, Klammern und `/`; die zweite Symbolzeile enthält unter anderem `*`, Anführungszeichen, Apostroph, `:`, `;`, `!` und `?`.[2]
- FlorisBoard trennt eine kompakte numerische Eingabeebene (3×4-Ziffernblock plus `-`, Leerzeichen, Löschen, Komma, Punkt und Eingabe) von der allgemeinen Symbolbelegung.[3]
- AnySoftKeyboard bietet zusätzlich Zahlen und häufige Symbole als sichtbare Hinweise bzw. Langdruck-Ziele auf den Buchstabentasten an.[6]
- Android empfiehlt für bedienbare Elemente mindestens 48×48 dp Touch-Zielgröße; bei eigenen UI-Elementen muss die App diese Größe selbst sicherstellen.[4]

## Bewertung des aktuellen XiKey-Layouts

**Stimmig:** Die Buchstabenebene folgt bereits der deutschen QWERTZ-Anordnung mit Umlauten und orientiert sich damit an beiden untersuchten deutschen Referenzlayouts.[1][5]

**Noch offen:**

1. `ß` fehlt auf der Buchstabenebene, obwohl es im deutschen Vergleichslayout einen festen, leicht erreichbaren Platz besitzt.[1][5]
2. Die neue Zeichenebene besitzt alle Ziffern in einer Zeile. Dadurch werden die Tasten besonders auf schmalen Geräten kleiner als nötig.
3. Die Steuerzeile zeigt in der Zeichenebene derzeit zweimal `ABC` (einmal als erste Taste und einmal als Seitenschalter). Das ist redundant.
4. Komma und Punkt sollten auf der Buchstabenebene direkt erreichbar bleiben; sie sind beim Schreiben deutlich häufiger als der Wechsel zur Zeichenebene.
5. Langdruck und sichtbare Symbolhinweise fehlen. Sie sind sinnvoll, aber erst nach einem belastbaren Grundlayout umzusetzen.

## Empfohlenes Zielbild (nächster UI-Schritt)

### Buchstabenebene

- Deutsche QWERTZ-Reihen beibehalten.[1][5]
- Dritte Reihe: `⇧  y x c v b n m ß  ⌫`.[5]
- Untere Steuerzeile: `?123`, Sprachwechsel, `,`, breite Leertaste, `.`, Eingabe.
- `ä`, `ö`, `ü`, `ß` bleiben direkte Tasten; dialektspezifische Varianten kommen später über Langdruck oder Wortvorschläge, nicht als zusätzliche Grundtasten.[1][5]

### Zeichenebene

- Erste Reihe: `1 2 3 4 5 6 7 8 9 0`.[3]
- Zweite Reihe: `@ # € % & - + ( ) /`.[2]
- Dritte Reihe: `* " ' : ; ! ?` zentriert zwischen einem `ABC`-Rückweg und Löschen.[2]
- Untere Steuerzeile: Sprachwechsel, `,`, breite Leertaste, `.`, Eingabe. Nur **ein** `ABC`-Schalter.

Damit entspricht der Zeicheninhalt weitgehend der westlichen Referenzbelegung von FlorisBoard, ohne XiKey mit einer zweiten, schwer merkbaren Symbolseite zu überladen.[2]

### Spätere Erweiterungen

1. **Langdruck:** Zahlen auf der oberen Buchstabenreihe und wenige häufige Satzzeichen als Hinweise/Langdruckziele, analog zu AnySoftKeyboard.[6]
2. **Spezielle Zahlenfelder:** Für Telefonnummern, Beträge und PINs eine kompakte 3×4-Nummernbelegung statt der allgemeinen Symbolseite, analog zur getrennten numerischen Ebene von FlorisBoard.[3]
3. **Messbare Usability-Prüfung:** Auf kleinen und großen Geräten prüfen, dass alle System- und Buchstabentasten mindestens ein 48×48-dp-Touch-Ziel erhalten.[4]

## Umsetzungsreihenfolge

1. Steuerzeile bereinigen, Komma/Punkt zurück auf die Buchstabenebene legen und `ß` ergänzen.
2. Zeichenebene wie oben anordnen und visuell auf einem realen Gerät testen.
3. Erst danach Langdruck/Hints als getrennten, testbaren Schritt ergänzen.

## Sources

[1] https://raw.githubusercontent.com/florisboard/florisboard/main/app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/characters/german.json — FlorisBoard German layout
[2] https://raw.githubusercontent.com/florisboard/florisboard/main/app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/symbols/western.json — FlorisBoard western symbols
[3] https://raw.githubusercontent.com/florisboard/florisboard/main/app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numeric/western_arabic.json — FlorisBoard numeric layout
[4] https://developer.android.com/guide/topics/ui/accessibility/apps — Android accessibility guidance
[5] https://github.com/AnySoftKeyboard/AnySoftKeyboard/blob/main/addons/languages/german/pack/src/main/res/xml/de_qwertz.xml — AnySoftKeyboard German QWERTZ layout
[6] https://github.com/AnySoftKeyboard/AnySoftKeyboard/blob/main/addons/languages/german/pack/src/main/res/xml/de_qwertz_symbols.xml — AnySoftKeyboard German hinted-symbol layout
