package de.clans;

import de.clans.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClansPlugin extends JavaPlugin {

    private ClanManager clans;
    private GuiManager gui;
    private ChatPrompts prompts;
    private PackManager pack;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        clans = new ClanManager(this);
        clans.load();

        gui = new GuiManager(this);
        prompts = new ChatPrompts(this);
        pack = new PackManager(this);
        pack.enable();

        Bukkit.getPluginManager().registerEvents(new ClanListener(this), this);
        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(prompts, this);
        Bukkit.getPluginManager().registerEvents(pack, this);

        ClanCommand cmd = new ClanCommand(this);
        for (String name : new String[]{"clan", "cc"}) {
            var pc = getCommand(name);
            if (pc != null) {
                pc.setExecutor(cmd);
                pc.setTabCompleter(cmd);
            }
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanExpansion(this).register();
            getLogger().info("PlaceholderAPI gefunden: %clan_name%, %clan_tab%, %clan_tag% ... registriert.");
        }

        gui.start();

        // Tabliste auffrischen + Daten regelmäßig sichern
        Bukkit.getScheduler().runTaskTimer(this, () -> clans.refreshAll(), 100L, 100L);
        Bukkit.getScheduler().runTaskTimer(this, () -> clans.saveIfDirty(), 600L, 600L);
        Bukkit.getScheduler().runTask(this, () -> clans.refreshAll());
    }

    @Override
    public void onDisable() {
        if (gui != null) gui.stop();
        if (pack != null) pack.disable();
        if (clans != null) clans.save();
    }

    public ClanManager clans() { return clans; }
    public GuiManager gui() { return gui; }
    public ChatPrompts prompts() { return prompts; }
    public PackManager pack() { return pack; }

    public String prefix() {
        return getConfig().getString("prefix", "<dark_gray>[<gold>Clan<dark_gray>]<gray> ");
    }

    /** Chatnachricht; ist die GUI offen, wird sie zusätzlich dort als Toast angezeigt. */
    public void send(CommandSender to, String mini) {
        to.sendMessage(Text.mm(prefix() + mini));
        if (to instanceof Player p && gui != null) gui.toast(p, mini);
    }

    public void err(CommandSender to, String mini) {
        send(to, "<red>" + mini);
    }
}
