package de.clans.gui;

import de.clans.ClansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuiManager implements Listener {

    private final ClansPlugin plugin;
    private final Map<UUID, ClanGui> open = new HashMap<>();
    private BukkitTask task;
    private int refreshEvery;
    private boolean refreshPending;

    public GuiManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // nur ein sanfter Abgleich alle 2 Sekunden; gesendet wird nur, was sich geändert hat
        refreshEvery = 1;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (ClanGui g : new ArrayList<>(open.values())) g.player().closeInventory();
        open.clear();
    }

    /** Öffnet die GUI. start == null -> passende Startansicht. */
    public void open(Player p, View start) {
        ClanGui g = new ClanGui(plugin, p, start);
        open.put(p.getUniqueId(), g);
        p.openInventory(g.getInventory());
        g.opened();
    }

    /** Baut eine bereits offene GUI neu auf (z.B. nachdem das Pack geladen wurde). */
    public void reopen(Player p) {
        ClanGui g = open.get(p.getUniqueId());
        if (g != null) open(p, g.view());
    }

    public boolean isOpen(Player p) {
        return open.containsKey(p.getUniqueId());
    }

    /** Zeigt eine Meldung in der geöffneten GUI des Spielers (falls offen). */
    public void toast(Player p, String mini) {
        ClanGui g = open.get(p.getUniqueId());
        if (g != null) g.toast(mini);
    }

    /** Alle offenen GUIs im nächsten Tick neu zeichnen (zusammengefasst). */
    public void refreshSoon() {
        if (refreshPending || open.isEmpty()) return;
        refreshPending = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            refreshPending = false;
            for (ClanGui g : new ArrayList<>(open.values())) {
                if (g.player().isOnline()) g.refresh();
            }
        });
    }

    private void tick() {
        for (ClanGui g : new ArrayList<>(open.values())) {
            if (!g.player().isOnline()) continue;
            g.tick(refreshEvery);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ClanGui gui)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;
        if (!(e.getClickedInventory().getHolder() instanceof ClanGui)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.equals(gui.player())) return;
        gui.click(e.getRawSlot(), e.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ClanGui) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof ClanGui g) {
            UUID u = e.getPlayer().getUniqueId();
            if (open.get(u) == g) open.remove(u);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        open.remove(e.getPlayer().getUniqueId());
    }
}
