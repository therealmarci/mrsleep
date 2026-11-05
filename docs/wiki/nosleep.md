# /nosleep - die Nacht festhalten

Du brauchst die Nacht - für eine Mob-Farm, zum Bauen im Dunkeln oder für ein Foto? Mit `/nosleep` verhinderst du, dass sie übersprungen wird.

| Eingabe | Wirkung |
|---|---|
| `/nosleep` | Blockiert die laufende Nacht (tagsüber: die kommende). Ist schon eine eigene Blockade aktiv, wird sie **aufgehoben**. |
| `/nosleep 3` | Blockiert die nächsten 3 Nächte (wie viele höchstens, legt der Server fest - Standard 3). |
| `/nosleep off` | Hebt die eigene Blockade auf (auch `aus` oder `cancel`). |
| `/nosleep info` | Zeigt deinen Status: wer schläft, ob du zählst, wer blockiert, dein Cooldown. |

## Gut zu wissen
- Solange **irgendein** Spieler blockiert, wird die Nacht nicht übersprungen. Alle anderen sehen, wer blockiert.
- Je nach Server kannst du in einer blockierten Nacht trotzdem ins Bett (z.B. um den Spawnpunkt zu setzen) - übersprungen wird sie nur nicht.
- Gehst du offline, endet deine Blockade normalerweise.
- Blockaden gelten **pro Welt**.

## Cooldown
Nach einer Blockade musst du eine Weile warten, bevor du wieder blockieren kannst (Standard 15 Minuten). Der Cooldown startet erst, **wenn die blockierte Nacht anbricht**: Wer tagsüber blockiert und vor Nachtbeginn wieder aufhebt, bekommt keinen. Wie lange du noch warten musst, zeigt `/nosleep info`.

> [!NOTE]
> Das Team kann deinen Cooldown aufheben oder dich von `/nosleep` ausschließen, wenn die Blockaden überhandnehmen.
