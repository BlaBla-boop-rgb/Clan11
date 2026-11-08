package de.clans;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class ClanListener implements Listener {

    private final ClansPlugin plugin;

    public ClanListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        ClanManager m = plugin.clans();
        Clan c = m.getClan(p.getUniqueId());
        if (c != null) {
            // Namen aktuell halten (Namensänderungen)
            c.members.put(p.getUniqueId(), p.getName());
            // Leiter ohne VIP verliert die Clanfarbe (wie im Skript)
            if (c.owner.equals(p.getUniqueId())) m.enforceColor(c, p);
            m.markDirty();
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> m.refresh(p), 2L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        ClanManager m = plugin.clans();
        Player victim = e.getEntity();
        Clan vc = m.getClan(victim.getUniqueId());
        if (vc != null) {
            vc.deaths++;
            m.markDirty();
        }
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            Clan kc = m.getClan(killer.getUniqueId());
            if (kc != null) {
                kc.kills++;
                m.markDirty();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Clan c = plugin.clans().getClan(e.getPlayer().getUniqueId());
        if (c != null) {
            c.blocks++;
            // nur speichern, keine GUI-Aktualisierung bei jedem Block
            plugin.clans().saveLater();
        }
    }
}
