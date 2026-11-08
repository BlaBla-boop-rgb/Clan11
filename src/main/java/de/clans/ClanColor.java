package de.clans;

import org.bukkit.Material;

import java.util.Locale;

/** Die 15 Clanfarben. Die ids müssen zu tools/make_pack.py passen (Textur-Namen). */
public enum ClanColor {
    RED("Rot", 255, 85, 85, Material.RED_CONCRETE, Material.RED_STAINED_GLASS_PANE, Material.RED_BANNER, "red"),
    DARK_RED("Dunkelrot", 170, 0, 0, Material.NETHER_WART_BLOCK, Material.RED_STAINED_GLASS_PANE, Material.RED_BANNER, "darkred", "dark_red"),
    ORANGE("Orange", 255, 170, 0, Material.ORANGE_CONCRETE, Material.ORANGE_STAINED_GLASS_PANE, Material.ORANGE_BANNER, "orange", "gold"),
    YELLOW("Gelb", 255, 255, 85, Material.YELLOW_CONCRETE, Material.YELLOW_STAINED_GLASS_PANE, Material.YELLOW_BANNER, "yellow"),
    GREEN("Grün", 85, 255, 85, Material.LIME_CONCRETE, Material.LIME_STAINED_GLASS_PANE, Material.LIME_BANNER, "green", "lime"),
    DARK_GREEN("Dunkelgrün", 0, 170, 0, Material.GREEN_CONCRETE, Material.GREEN_STAINED_GLASS_PANE, Material.GREEN_BANNER, "darkgreen", "dark_green"),
    AQUA("Aqua", 85, 255, 255, Material.LIGHT_BLUE_CONCRETE, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.LIGHT_BLUE_BANNER, "aqua", "cyan"),
    DARK_AQUA("Dunkelaqua", 0, 170, 170, Material.CYAN_CONCRETE, Material.CYAN_STAINED_GLASS_PANE, Material.CYAN_BANNER, "darkaqua", "dark_aqua"),
    BLUE("Blau", 85, 85, 255, Material.BLUE_CONCRETE, Material.BLUE_STAINED_GLASS_PANE, Material.BLUE_BANNER, "blue"),
    DARK_BLUE("Dunkelblau", 0, 0, 170, Material.LAPIS_BLOCK, Material.BLUE_STAINED_GLASS_PANE, Material.BLUE_BANNER, "darkblue", "dark_blue"),
    PINK("Pink", 255, 85, 255, Material.PINK_CONCRETE, Material.PINK_STAINED_GLASS_PANE, Material.PINK_BANNER, "pink", "magenta"),
    PURPLE("Lila", 170, 0, 170, Material.PURPLE_CONCRETE, Material.PURPLE_STAINED_GLASS_PANE, Material.PURPLE_BANNER, "purple", "darkpurple", "dark_purple"),
    WHITE("Weiß", 255, 255, 255, Material.WHITE_CONCRETE, Material.WHITE_STAINED_GLASS_PANE, Material.WHITE_BANNER, "white"),
    GRAY("Grau", 170, 170, 170, Material.LIGHT_GRAY_CONCRETE, Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.LIGHT_GRAY_BANNER, "gray", "grey"),
    DARK_GRAY("Dunkelgrau", 85, 85, 85, Material.GRAY_CONCRETE, Material.GRAY_STAINED_GLASS_PANE, Material.GRAY_BANNER, "darkgray", "darkgrey", "dark_gray");

    private final String german;
    private final int r, g, b;
    private final Material swatch, pane, banner;
    private final String[] names;

    ClanColor(String german, int r, int g, int b, Material swatch, Material pane, Material banner, String... names) {
        this.german = german;
        this.r = r;
        this.g = g;
        this.b = b;
        this.swatch = swatch;
        this.pane = pane;
        this.banner = banner;
        this.names = names;
    }

    public String german() { return german; }
    public Material swatch() { return swatch; }
    public Material pane() { return pane; }
    public Material banner() { return banner; }
    /** Kurzname, entspricht den Textur-Namen im Pack (z.B. "darkred"). */
    public String id() { return names[0]; }
    public String hex() { return String.format("#%02x%02x%02x", r, g, b); }

    public static ClanColor parse(String s) {
        if (s == null || s.isBlank()) return null;
        String l = s.toLowerCase(Locale.ROOT);
        for (ClanColor c : values()) {
            for (String n : c.names) if (n.equals(l)) return c;
        }
        return null;
    }

    /** "#rrggbb" für Farbnamen oder Hex-Eingaben, sonst null. */
    public static String toHex(String s) {
        if (s == null || s.isBlank()) return null;
        ClanColor c = parse(s);
        if (c != null) return c.hex();
        if (s.matches("(?i)#[0-9a-f]{6}")) return s.toLowerCase(Locale.ROOT);
        return null;
    }

    /** Nächstliegende Clanfarbe (auch für Hex-Werte), null wenn nichts gesetzt/ungültig. */
    public static ClanColor nearest(String s) {
        ClanColor c = parse(s);
        if (c != null) return c;
        String hex = toHex(s);
        if (hex == null) return null;
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        ClanColor best = null;
        long bestD = Long.MAX_VALUE;
        for (ClanColor k : values()) {
            long d = (long) (k.r - r) * (k.r - r) + (long) (k.g - g) * (k.g - g) + (long) (k.b - b) * (k.b - b);
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }
}
