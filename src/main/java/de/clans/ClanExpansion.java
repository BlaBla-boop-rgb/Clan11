package de.clans;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

/** %clan_name%, %clan_tab%, %clan_tag%, %clan_kills%, %clan_deaths%, %clan_blocks%, %clan_members%, %clan_leader% */
public final class ClanExpansion extends PlaceholderExpansion {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final ClansPlugin plugin;

    public ClanExpansion(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "clan"; }
    @Override public String getAuthor() { return "Clans"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        Clan c = plugin.clans().getClan(player.getUniqueId());
        String noName = plugin.getConfig().getString("no-clan-name", "kein Clan");
        Component noTab = Text.MM.deserialize(plugin.getConfig().getString("no-clan-tab", ""));
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "name" -> c != null ? c.name : noName;
            case "tab" -> LEGACY.serialize(c != null ? c.tag : noTab);
            case "tag" -> c != null ? LEGACY.serialize(c.tag) : "";
            case "kills" -> c != null ? String.valueOf(c.kills) : "0";
            case "deaths" -> c != null ? String.valueOf(c.deaths) : "0";
            case "blocks" -> c != null ? String.valueOf(c.blocks) : "0";
            case "members" -> c != null ? String.valueOf(c.members.size()) : "0";
            case "leader" -> c != null ? c.leaderName() : "";
            default -> null;
        };
    }
}
