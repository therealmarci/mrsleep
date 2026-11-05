# Häufige Fragen

### Ich liege im Bett, aber die Nacht wird nicht übersprungen.
Führ `/mrsleep debug` aus oder öffne das [Admin-Menü](admin-menue.md): Dort steht für jeden Spieler, ob er zählt (und warum nicht), ob er schläft und ob jemand blockiert. Häufigste Ursache: Über `*` hat ein Spieler auch `mrsleep.exempt` - siehe [Wildcards](permissions.md#wildcards).

### Die Gamerule `playersSleepingPercentage` steht auf 100.
Das ist gewollt, die Quote kommt aus `sleep.percentage` - [warum](schlafquote.md#welten).

### Was zählt als „eine Nacht“?
Wer tagsüber `/nosleep` nutzt, blockiert die kommende Nacht, wer es nachts nutzt, die laufende. Blockaden gelten pro Welt.

### Meine Admins haben keinen Cooldown-Schutz - oder doch einen?
Der Cooldown gilt auch für OPs. Ohne Cooldown nur mit `mrsleep.nosleep.nocooldown` (in keiner Gruppe enthalten) oder einem Bypass.

### Ich habe einen Fehler gefunden oder einen Wunsch.
Gern über das **[Formular auf der Website](https://mrtechnology.marcelr.at/report/mrsleep/)** - am besten mit der Ausgabe von `/mrsleep version` und `/mrsleep debug`.
