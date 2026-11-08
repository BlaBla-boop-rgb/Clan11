package de.clans;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest die Clan-Daten des alten Skripts aus Skripts Variablen-Datei (plugins/Skript/variables.csv).
 *
 * Das Skript speichert pro Clan (k = Clanname in Kleinbuchstaben):
 *   clan.registry::k            Anzeigename
 *   clan::k::name / owner / c1 / c2          Text
 *   clan::k::kills / deaths / blocks         Zahl
 *   clan::k::members::UUID                   Spielername
 *   clan::k::allies::andererKey              Allianz
 * Jede Zeile der Datei lautet "Name, Typ, Wert", der Wert ist als Hex-Text abgelegt. Es gibt keine Abhängigkeit
 * zu Bukkit, damit sich das Einlesen einfach testen lässt.
 */
public final class SkriptImport {
    private SkriptImport() {}

    public record Raw(String key, String name, UUID owner, String c1, String c2, int kills, int deaths, int blocks,
                      Map<UUID, String> members, Set<String> allies) {}

    private static final Pattern UUID_RE = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final class Acc {
        String registry;
        String name, c1 = "", c2 = "";
        UUID owner;
        int kills, deaths, blocks;
        final Map<UUID, String> members = new LinkedHashMap<>();
        final Set<String> allies = new LinkedHashSet<>();
    }

    public static List<Raw> read(Path csv) throws IOException {
        return parse(Files.readAllLines(csv, StandardCharsets.UTF_8));
    }

    public static List<Raw> parse(List<String> lines) {
        Map<String, Acc> map = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int c2 = line.lastIndexOf(',');
            if (c2 < 0) continue;
            int c1 = line.lastIndexOf(',', c2 - 1);
            if (c1 < 0) continue;
            String name = unquote(line.substring(0, c1).strip()).toLowerCase(Locale.ROOT);
            String type = line.substring(c1 + 1, c2).strip().toLowerCase(Locale.ROOT);
            String hex = line.substring(c2 + 1).strip();
            if (!(name.startsWith("clan::") || name.startsWith("clan.registry::"))) continue;
            byte[] data = unhex(hex);
            if (data == null) continue;
            String[] p = name.split("::");

            if (p[0].equals("clan.registry") && p.length == 2) {
                map.computeIfAbsent(p[1], k -> new Acc()).registry = text(data);
                continue;
            }
            if (!p[0].equals("clan") || p.length < 3) continue;
            Acc a = map.computeIfAbsent(p[1], k -> new Acc());
            switch (p[2]) {
                case "name" -> a.name = text(data);
                case "owner" -> a.owner = uuidOf(text(data));
                case "c1" -> a.c1 = text(data).toLowerCase(Locale.ROOT);
                case "c2" -> a.c2 = text(data).toLowerCase(Locale.ROOT);
                case "kills" -> a.kills = number(type, data);
                case "deaths" -> a.deaths = number(type, data);
                case "blocks" -> a.blocks = number(type, data);
                case "members" -> {
                    if (p.length >= 4) {
                        UUID u = uuidOf(p[3]);
                        if (u != null) a.members.put(u, text(data));
                    }
                }
                case "allies" -> {
                    if (p.length >= 4) a.allies.add(p[3]);
                }
                default -> { }
            }
        }
        List<Raw> out = new ArrayList<>();
        for (Map.Entry<String, Acc> e : map.entrySet()) {
            Acc a = e.getValue();
            String key = e.getKey();
            String display = firstNonBlank(a.name, a.registry, key);
            // Clanname muss zu den Regeln des Plugins passen
            if (!display.matches("[A-Za-z0-9_]+")) display = key;
            if (!key.matches("[a-z0-9_]+")) continue;
            out.add(new Raw(key, display, a.owner,
                    a.c1, a.c2, a.kills, a.deaths, a.blocks, a.members, a.allies));
        }
        return out;
    }

    // ------------------------------------------------------------ Dekodieren

    private static UUID uuidOf(String s) {
        if (s == null) return null;
        Matcher m = UUID_RE.matcher(s);
        String found = null;
        while (m.find()) found = m.group();
        if (found == null) return null;
        try {
            return UUID.fromString(found);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String firstNonBlank(String... v) {
        for (String s : v) if (s != null && !s.isBlank()) return s.strip();
        return "";
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    static byte[] unhex(String h) {
        if (h.length() % 2 != 0) return null;
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int hi = Character.digit(h.charAt(i * 2), 16);
            int lo = Character.digit(h.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return null;
            b[i] = (byte) (hi * 16 + lo);
        }
        return b;
    }

    /**
     * Text aus den gespeicherten Bytes. Egal ob reiner UTF-8-Text oder mit kurzem Kopf (Typ-/Längenbyte, alle unter
     * 0x20): der Text ist der letzte zusammenhängende Block druckbarer Zeichen.
     */
    static String text(byte[] d) {
        int end = d.length;
        while (end > 0 && (d[end - 1] & 0xFF) < 0x20) end--;
        int start = end;
        while (start > 0 && (d[start - 1] & 0xFF) >= 0x20) start--;
        return new String(d, start, end - start, StandardCharsets.UTF_8).strip();
    }

    /** Zahl: Skript legt sie als 8 Byte (long bzw. double) ab, ggf. mit Kopfbytes davor. */
    static int number(String type, byte[] d) {
        if (d.length == 0) return 0;
        double v;
        if (d.length >= 8) {
            long bits = 0;
            for (int i = d.length - 8; i < d.length; i++) bits = (bits << 8) | (d[i] & 0xFFL);
            v = type.contains("double") || type.contains("float") ? Double.longBitsToDouble(bits) : (double) bits;
        } else {
            long bits = 0;
            for (byte x : d) bits = (bits << 8) | (x & 0xFFL);
            v = bits;
        }
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, Math.round(v));
    }
}
