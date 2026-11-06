# Anzeige & Chat-Hinweise

## Fortschritt
Wo „1/2 schlafen“ bzw. eine Blockade angezeigt wird, legt `sleep.display.mode` fest:

| Wert | Anzeige |
|---|---|
| `bossbar` (Standard) | Leiste oben am Bildschirm - wird vom Chat im Bett nicht verdeckt |
| `actionbar` | über der Hotbar (im Bett vom Chatfenster verdeckt) |
| `title` | Untertitel in der Bildschirmmitte |
| `none` | nichts |

Die Farben der Bossbar stellst du mit `sleep.display.bossbar-color` und `bossbar-blocked-color` ein (pink, blue, red, green, yellow, purple, white).

Die eingebaute Minecraft-Anzeige „x/y Spieler schlafen“ zählt alle Spieler und kennt die Regeln von MRsleep nicht. MRsleep ersetzt sie deshalb sofort durch den eigenen Fortschritt (bei `actionbar`) bzw. blendet sie aus (`sleep.display.hide-vanilla-message`, Standard an).

## Chat
- **Schlaf-Nachricht** „Alex hat sich schlafen gelegt (1/2)“ an die Welt, den ganzen Server oder niemanden: `sleep.chat.sleep-message` (`world` / `server` / `none`).
- **„Guten Morgen!“**, wenn die Nacht übersprungen wurde: `sleep.chat.announce-skip`.
- Hinweise, wenn die Nacht blockiert ist, wenn eine Blockade abläuft und wenn ein Admin etwas ändert.

Jeder Text steht in der Sprachdatei und lässt sich ändern oder mit `""` abschalten - siehe [Sprachen & Texte](https://mrtechnology.marcelr.at/wiki/allgemein/sprachen/).
