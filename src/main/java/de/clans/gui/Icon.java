package de.clans.gui;

import org.bukkit.Material;

/** Custom-Icon (Textur-Name im Pack) + Ersatz-Item für Spieler ohne Pack. */
public enum Icon {
    OVERVIEW("overview", Material.SHIELD),
    MEMBERS("members", Material.PLAYER_HEAD),
    ALLIES("allies", Material.BELL),
    COLOR("color", Material.PINK_DYE),
    CLANS("clans", Material.BOOKSHELF),
    INVITES("invites", Material.WRITABLE_BOOK),
    HOME("home", Material.CAMPFIRE),
    PLUS("plus", Material.EMERALD_BLOCK),
    CHECK("check", Material.LIME_CONCRETE),
    CROSS("cross", Material.RED_CONCRETE),
    KICK("kick", Material.IRON_BOOTS),
    CROWN("crown", Material.GOLDEN_HELMET),
    CHAT("chat", Material.WRITABLE_BOOK),
    INVITE("invite", Material.EMERALD),
    TNT("tnt", Material.TNT),
    DOOR("door", Material.OAK_DOOR),
    ARROW_UP("arrow_up", Material.ARROW),
    ARROW_DOWN("arrow_down", Material.ARROW),
    ARROW_LEFT("arrow_left", Material.ARROW),
    RESET("reset", Material.BARRIER),
    HEX("hex", Material.PAPER),
    INFO("info", Material.BOOK),
    SWORD("sword", Material.DIAMOND_SWORD),
    SKULL("skull", Material.SKELETON_SKULL),
    KD("kd", Material.EXPERIENCE_BOTTLE),
    PICKAXE("pickaxe", Material.DIAMOND_PICKAXE),
    CLOSE("close", Material.BARRIER),
    LOCK("lock", Material.BARRIER),
    PREVIEW("preview", Material.NAME_TAG),
    BOLD("bold", Material.IRON_INGOT),
    ITALIC("italic", Material.FEATHER),
    GRADIENT("gradient", Material.AMETHYST_SHARD),
    LOGO("logo", Material.NETHER_STAR);

    private final String model;
    private final Material fallback;

    Icon(String model, Material fallback) {
        this.model = model;
        this.fallback = fallback;
    }

    public String model() { return model; }
    public Material fallback() { return fallback; }
}
