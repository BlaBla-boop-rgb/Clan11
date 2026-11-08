package de.clans.gui;

import de.clans.ClanColor;
import de.clans.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Baut die GUI-Items. Mit custom=true bekommen sie ein eigenes Modell aus dem Resource Pack
 * (Basis-Item PAPER + item_model "clans:..."), sonst ein normales Minecraft-Item.
 * Die Auswahl-Hervorhebung läuft über das erste custom_model_data-Flag (true -> "_sel"-Textur).
 */
public final class Items {
    private Items() {}

    private static final String NS = "clans";
    private static final Map<String, ItemStack> TILES = new HashMap<>();

    private static NamespacedKey key(String id) {
        return new NamespacedKey(NS, id);
    }

    private static void flags(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
    }

    private static void setFlag(ItemMeta meta, boolean value) {
        CustomModelDataComponent c = meta.getCustomModelDataComponent();
        c.setFlags(List.of(value));
        meta.setCustomModelDataComponent(c);
    }

    private static List<Component> lore(String... lines) {
        return Arrays.stream(lines).map(Text::mm).toList();
    }

    private static ItemStack make(boolean custom, String model, Material fallback, Component name, String... lore) {
        ItemStack it = new ItemStack(custom ? Material.PAPER : fallback);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        if (lore.length > 0) meta.lore(lore(lore));
        flags(meta);
        if (custom) {
            meta.setItemModel(key(model));
            setFlag(meta, false);
        }
        it.setItemMeta(meta);
        return it;
    }

    /** Karten-Button mit Icon. */
    public static ItemStack btn(boolean custom, Icon icon, String name, String... lore) {
        return make(custom, icon.model(), icon.fallback(), Text.mm(name), lore);
    }

    /** Karten-Button mit Icon und fertigem Component als Name (z.B. Clan-Tag). */
    public static ItemStack named(boolean custom, Icon icon, Component name, String... lore) {
        return make(custom, icon.model(), icon.fallback(), name, lore);
    }

    /** Teilstück (r,c = 0..1) einer großen 2x2-Karte mit Icon. */
    public static ItemStack big(boolean custom, Icon icon, int r, int c, Component name, String... lore) {
        return make(custom, "big_" + icon.model() + "_" + r + c, icon.fallback(), name, lore);
    }

    /** Teilstück (r = 0..1) einer hohen 1x2-Karte. */
    public static ItemStack tall(boolean custom, Icon icon, int r, Component name, String... lore) {
        return make(custom, "tall_" + icon.model() + "_" + r, icon.fallback(), name, lore);
    }

    /** Teilstück einer großen 2x2-Clan-Banner-Karte. */
    public static ItemStack bigBanner(boolean custom, ClanColor col, int r, int c, Component name, String... lore) {
        ClanColor k = col != null ? col : ClanColor.GREEN;
        return make(custom, "big_banner_" + k.id() + "_" + r + c, k.banner(), name, lore);
    }

    /** Tab-Teilstück (c = 0 links, 1 rechts; Tabs sind 2 Slots breit). */
    public static ItemStack tab(boolean custom, Icon icon, int c, String name, String... lore) {
        return make(custom, "tab_" + icon.model() + "_" + c, icon.fallback(), Text.mm(name), lore);
    }

    /** Clan-Banner in Clanfarbe (c == null -> grün). */
    public static ItemStack banner(boolean custom, ClanColor c, Component name, String... lore) {
        ClanColor col = c != null ? c : ClanColor.GREEN;
        return make(custom, "banner_" + col.id(), col.banner(), name, lore);
    }

    /** Farbfeld für die Farbauswahl. */
    public static ItemStack swatch(boolean custom, ClanColor col, String name, String... lore) {
        return make(custom, "swatch_" + col.id(), col.swatch(), Text.mm(name), lore);
    }

    public static ItemStack head(OfflinePlayer op, String name, String... lore) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        meta.setOwningPlayer(op);
        meta.displayName(Text.mm(name));
        if (lore.length > 0) meta.lore(lore(lore));
        flags(meta);
        it.setItemMeta(meta);
        return it;
    }

    /** Hintergrund-/Rahmen-Kachel ohne Tooltip. */
    public static ItemStack tile(boolean custom, String id) {
        return TILES.computeIfAbsent((custom ? "c:" : "f:") + id, k -> {
            ItemStack it = new ItemStack(custom ? Material.PAPER : paneFor(id));
            ItemMeta meta = it.getItemMeta();
            meta.setHideTooltip(true);
            if (custom) meta.setItemModel(key(id));
            it.setItemMeta(meta);
            return it;
        });
    }

    private static Material paneFor(String id) {
        if (id.equals("header")) return Material.RED_STAINED_GLASS_PANE;
        if (id.equals("scroll_track") || id.equals("bar")) return Material.GRAY_STAINED_GLASS_PANE;
        if (id.equals("scroll_handle")) return Material.ORANGE_STAINED_GLASS_PANE;
        return Material.BLACK_STAINED_GLASS_PANE;
    }

    /** Markiert ein Item als ausgewählt (Rahmen-Textur bzw. Glanz) und hängt optional eine Hinweiszeile an. */
    public static ItemStack select(ItemStack src, boolean custom, String accentHex, String label) {
        ItemStack it = src.clone();
        ItemMeta meta = it.getItemMeta();
        if (custom && it.getType() == Material.PAPER) {
            setFlag(meta, true);
        } else {
            Component old = meta.hasDisplayName() ? meta.displayName() : Component.text(it.getType().name());
            TextColor col = TextColor.fromHexString(accentHex);
            meta.displayName(Component.text("▶ ", col).decoration(TextDecoration.ITALIC, false).append(old));
            meta.setEnchantmentGlintOverride(true);
        }
        if (label != null) {
            List<Component> l = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            l.add(Component.empty());
            l.add(Text.mm("<green>✔ " + label));
            meta.lore(l);
        }
        it.setItemMeta(meta);
        return it;
    }
}
