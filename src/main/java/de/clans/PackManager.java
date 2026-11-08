package de.clans;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Verteilt das Resource Pack mit den Custom-Icons und merkt sich, wer es geladen hat. */
public final class PackManager implements Listener {

    public static final String FILE_NAME = "ClansPack.zip";
    public static final UUID PACK_ID = UUID.nameUUIDFromBytes("clans-gui-pack".getBytes(StandardCharsets.UTF_8));

    private final ClansPlugin plugin;
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();
    private File packFile;
    private volatile byte[] sha1;
    private byte[] localSha1;
    private String url;

    public PackManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        try {
            plugin.getDataFolder().mkdirs();
            // immer überschreiben, damit das Pack zur Plugin-Version passt
            plugin.saveResource(FILE_NAME, true);
            packFile = new File(plugin.getDataFolder(), FILE_NAME);
            localSha1 = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(packFile.toPath()));
            sha1 = localSha1;
        } catch (Exception ex) {
            plugin.getLogger().warning("Resource Pack konnte nicht vorbereitet werden: " + ex.getMessage());
            return;
        }

        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("resource-pack");
        if (cfg == null) return;
        String configured = cfg.getString("url", "").trim();
        if (!configured.isEmpty()) {
            url = configured;
        }

        if (url == null) {
            plugin.getLogger().info("Kein Pack-Link gesetzt. Lade plugins/Clans/" + FILE_NAME
                    + " irgendwo hoch und trage den Link bei resource-pack.url ein (siehe config.yml).");
        } else {
            plugin.getLogger().info("Resource Pack wird verteilt von: " + url);
            checkRemote();
        }
    }

    /**
     * Lädt die Pack-Datei vom Link und nimmt deren Prüfsumme. So passt der Hash immer zu dem, was wirklich
     * im Netz liegt (auch wenn Repo und Plugin-Version nicht ganz zusammenpassen). Schreibt das Ergebnis in die Konsole.
     */
    private void checkRemote() {
        final String link = url;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                        .connectTimeout(java.time.Duration.ofSeconds(10)).build();
                java.net.http.HttpResponse<byte[]> res = client.send(
                        java.net.http.HttpRequest.newBuilder(java.net.URI.create(link))
                                .timeout(java.time.Duration.ofSeconds(30)).GET().build(),
                        java.net.http.HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() != 200) {
                    plugin.getLogger().warning("Pack-Link nicht erreichbar (HTTP " + res.statusCode()
                            + "). Ist das GitHub-Repo öffentlich und stimmt der Link bei resource-pack.url?");
                    return;
                }
                byte[] remote = MessageDigest.getInstance("SHA-1").digest(res.body());
                if (java.util.Arrays.equals(remote, localSha1)) {
                    plugin.getLogger().info("Pack-Link OK: Datei im Netz = Datei im Plugin.");
                } else {
                    plugin.getLogger().warning("Die Pack-Datei im Netz weicht von der im Plugin ab. "
                            + "Es wird die Prüfsumme der Netz-Datei verwendet (Pack im Repo evtl. veraltet).");
                }
                sha1 = remote;
            } catch (Exception ex) {
                plugin.getLogger().warning("Pack-Link konnte nicht geprüft werden (" + ex.getClass().getSimpleName()
                        + ": " + ex.getMessage() + "). Es wird die Prüfsumme der Plugin-Datei verwendet.");
            }
        });
    }

    public void disable() {
    }

    /** Sendet das Pack an einen Spieler. Gibt false zurück, wenn kein Link konfiguriert ist. */
    public boolean send(Player p) {
        if (url == null || sha1 == null) return false;
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        p.setResourcePack(PACK_ID, url, sha1,
                Component.text("Clan-Menü Icons"), required);
        return true;
    }

    /** Sollen für diesen Spieler Custom-Icons genutzt werden? */
    public boolean custom(Player p) {
        String mode = plugin.getConfig().getString("resource-pack.mode", "auto");
        return switch (mode.toLowerCase()) {
            case "always" -> true;
            case "never" -> false;
            default -> loaded.contains(p.getUniqueId());
        };
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("resource-pack.send-on-join", true)) return;
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) send(p);
        }, 20L);
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        if (!PACK_ID.equals(e.getID())) return;
        Player p = e.getPlayer();
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> {
                loaded.add(p.getUniqueId());
                // geöffnete GUI sofort mit Custom-Icons neu aufbauen
                plugin.gui().reopen(p);
            }
            case DECLINED, FAILED_DOWNLOAD, FAILED_RELOAD, INVALID_URL, DISCARDED -> {
                loaded.remove(p.getUniqueId());
                plugin.getLogger().warning("Resource Pack bei " + p.getName() + ": " + e.getStatus());
                plugin.send(p, "<gray>Das Clan-Pack konnte nicht geladen werden (<red>" + e.getStatus()
                        + "<gray>). Prüfe in den Server-Einstellungen, ob Server-Resourcepacks erlaubt sind, und nutze <white>/clan pack<gray>.");
            }
            default -> { }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        loaded.remove(e.getPlayer().getUniqueId());
    }
}
