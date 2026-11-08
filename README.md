# Clans 2.0 (Paper 1.21.11)

Clan-System, das komplett über ein Menü (`/clan`) bedient wird. Mit Custom-Icons aus einem Resource Pack.

## Bauen
Java 21 + Maven: `mvn clean package` -> `target/Clans-2.0.0.jar` in `plugins/`.
Oder einfach das Projekt auf GitHub hochladen: die Action in `.github/workflows/build.yml` baut die JAR.

## Resource Pack (Custom-Icons)
Das Pack steckt in der JAR und liegt nach dem ersten Start unter `plugins/Clans/ClansPack.zip`.
Es muss einmal öffentlich erreichbar sein. Trage den Direktlink bei `resource-pack.url` ein
(Standard: die Datei aus dem GitHub-Repo per raw.githubusercontent.com).

Danach `/clan reload` bzw. Server neustarten. Spieler bekommen das Pack beim Join.
Wer das Pack ablehnt, sieht dasselbe Menü mit normalen Minecraft-Items (mode `auto`).

## Eigene Icons
`python3 tools/make_pack.py src/main/resources/ClansPack.zip` erzeugt das Pack neu.
Alle Texturen sind 16x16 und liegen in `assets/clans/textures/item/` (Namen siehe `make_pack.py`).
Du kannst sie in der Zip durch eigene PNGs ersetzen, solange die Dateinamen gleich bleiben.

## Befehle
- `/clan` - Menü (alles andere läuft darüber)
- `/cc <Nachricht>`, `/clan-chat <Nachricht>`, `/clan chat <Nachricht>` - Clan-Chat
- `/clan pack` - Icon-Pack erneut senden
- `/clan admin disband <Clan>` - Clan auflösen (`clans.admin`, standardmäßig OPs; auch per Knopf in "Alle Clans")
- `/clan admin import` - Clans aus dem alten Skript übernehmen (`plugins/Skript/variables.csv` oder `plugins/Clans/variables.csv`)
- `/clan admin reload` - Config neu laden

Permissions (Namen in der Config änderbar):
- `booster.use` - vorgegebene Farben
- `vipbooster.use` - vorgegebene + eigene Farben, kein Farbverlauf
- `mainbooster.use` - alles (Farben, eigene, Farbverlauf, fett/kursiv)
- `media.use` - vorgegebene + eigene Farben, fett/kursiv, kein Farbverlauf
- `clans.admin` (OPs),
`clans.bypass-cooldown` (keine Cooldowns, standardmäßig niemand).

PlaceholderAPI: `%clan_name%`, `%clan_tab%`, `%clan_tag%`, `%clan_kills%`, `%clan_deaths%`, `%clan_blocks%`, `%clan_members%`, `%clan_leader%`

## Design

- Dunkles Panel im Stil von Mineville: rote Kante unter der Titelleiste, Tabs mit Icon und Text, große 2x2-Icon-Karten mit Beschriftung, Scrollleiste. Texturen sind 4-fach hochauflösend (64 px pro Slot) mit 3D-Licht; Schrift: Silkscreen (OFL).
- Die GUI ist statisch (keine Animationen) und sendet nur geänderte Slots - dadurch kein Flackern.
- Das dunkle Fenster ist als Schrift-Grafik im Fenstertitel (`assets/clans/font/gui.json`, Zeichen U+E010..U+E053) und
  gilt deshalb **nur** für die Clan-GUI. Normale Truhen und andere Menüs bleiben wie in Vanilla.
- Einzige globale Änderung: Die Hover-Hervorhebung eines Slots (`slot_highlight_back/front`) ist transparent, weil Minecraft
  sonst nur eine Zelle einer 2x2-Karte markiert. Das gilt für alle Inventare. Löschst du die beiden PNGs aus dem Pack,
  kommt die Vanilla-Hervorhebung zurück.

## Übernahme aus dem alten Skript

Beim ersten Start liest das Plugin `plugins/Skript/variables.csv` und übernimmt alle Clans (Name, Leiter, Mitglieder,
Farben, Kills/Tode/Blöcke, Allianzen). Das passiert nur einmal; bereits vorhandene Clans werden nie überschrieben.
Später von Hand: `/clan admin import`.
