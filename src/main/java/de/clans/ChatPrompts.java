package de.clans;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Fängt die nächste Chatnachricht eines Spielers ab (Texteingaben aus der GUI). */
public final class ChatPrompts implements Listener {

    private record Prompt(Consumer<String> callback, Runnable onCancel, long expires) {}

    private final ClansPlugin plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatPrompts(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    /** Schließt die GUI und wartet 30 Sekunden auf eine Chatnachricht. onCancel läuft bei Abbruch/Timeout. */
    public void ask(Player p, String hintMini, Consumer<String> callback, Runnable onCancel) {
        long exp = System.currentTimeMillis() + 30_000L;
        prompts.put(p.getUniqueId(), new Prompt(callback, onCancel, exp));
        p.closeInventory();
        plugin.send(p, hintMini + " <dark_gray>(<red>abbrechen<dark_gray> zum Abbruch, 30s)");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Prompt pr = prompts.get(p.getUniqueId());
            if (pr != null && pr.expires() == exp) {
                prompts.remove(p.getUniqueId());
                plugin.err(p, "Die Eingabe ist abgelaufen.");
                if (pr.onCancel() != null && p.isOnline()) pr.onCancel().run();
            }
        }, 20L * 30 + 5);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Prompt pr = prompts.remove(e.getPlayer().getUniqueId());
        if (pr == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("abbrechen") || text.equalsIgnoreCase("cancel")) {
                plugin.send(p, "<gray>Eingabe abgebrochen.");
                if (pr.onCancel() != null) pr.onCancel().run();
                return;
            }
            pr.callback().accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        prompts.remove(e.getPlayer().getUniqueId());
    }
}
