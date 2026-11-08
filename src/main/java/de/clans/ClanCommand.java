package de.clans;

import de.clans.gui.View;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /clan öffnet die GUI. Alles Weitere läuft über das Menü.
 * /cc, /clan-chat <Nachricht> = Clan-Chat. Für OPs: /clan admin disband|import|reload.
 */
public final class ClanCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("invites", "members", "allies", "color", "clans", "chat", "pack", "help");
    private static final List<String> ADMIN = List.of("disband", "import", "reload");

    private final ClansPlugin plugin;

    public ClanCommand(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        boolean isClan = cmd.getName().equalsIgnoreCase("clan");

        // Admin-Befehle (auch von der Konsole)
        if (isClan && a.length > 0 && (a[0].equalsIgnoreCase("admin") || a[0].equalsIgnoreCase("reload"))) {
            admin(sender, a[0].equalsIgnoreCase("reload") ? new String[]{"reload"} : Arrays.copyOfRange(a, 1, a.length));
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        if (!isClan) {
            if (a.length == 0) plugin.err(p, "Verwendung: <yellow>/cc [Nachricht]");
            else plugin.clans().chat(p, String.join(" ", a));
            return true;
        }

        if (a.length == 0) {
            plugin.gui().open(p, null);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "invites" -> plugin.gui().open(p, View.INVITES);
            case "members" -> plugin.gui().open(p, View.MEMBERS);
            case "allies" -> plugin.gui().open(p, View.ALLIES);
            case "color", "farbe" -> plugin.gui().open(p, View.COLOR);
            case "clans", "list" -> plugin.gui().open(p, View.CLANS);
            case "chat" -> {
                if (a.length < 2) plugin.err(p, "Verwendung: <yellow>/clan chat [Nachricht]");
                else plugin.clans().chat(p, String.join(" ", Arrays.copyOfRange(a, 1, a.length)));
            }
            case "pack" -> {
                if (plugin.pack().send(p)) plugin.send(p, "<gray>Resource Pack wird gesendet ...");
                else plugin.err(p, "Es ist kein Pack-Link konfiguriert.");
            }
            case "help" -> {
                plugin.send(p, "<gray>Alles läuft über das Menü: <yellow>/clan");
                plugin.send(p, "<yellow>/cc [Nachricht] <gray>oder <yellow>/clan-chat [Nachricht] <gray>- Clan-Chat");
                plugin.send(p, "<yellow>/clan pack <gray>- Icon-Pack erneut laden");
                if (p.hasPermission("clans.admin")) {
                    plugin.send(p, "<red>/clan admin disband [Clan] <gray>- Clan auflösen (Admin)");
                    plugin.send(p, "<red>/clan admin import <gray>- Clans aus dem alten Skript übernehmen");
                }
            }
            default -> plugin.gui().open(p, null);
        }
        return true;
    }

    private void admin(CommandSender s, String[] a) {
        if (!s.hasPermission("clans.admin")) {
            plugin.err(s, "Dafür hast du keine Berechtigung.");
            return;
        }
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.clans().refreshAll();
                plugin.send(s, "<green>Konfiguration neu geladen.");
            }
            case "disband", "delete", "dissolve" -> {
                if (a.length < 2) plugin.err(s, "Verwendung: <yellow>/clan admin disband [Clanname]");
                else plugin.clans().adminDisband(s, a[1]);
            }
            case "import" -> {
                Path csv = a.length > 1 ? Path.of(a[1]) : plugin.clans().findSkriptFile();
                if (csv == null || !csv.toFile().isFile()) {
                    plugin.err(s, "Keine <yellow>variables.csv <red>gefunden. Lege sie nach <yellow>plugins/Clans/variables.csv<red>.");
                    return;
                }
                int n = plugin.clans().importSkript(csv, s);
                plugin.send(s, "<green>" + n + " Clan(s) übernommen <gray>(bereits vorhandene bleiben unverändert).");
            }
            default -> {
                plugin.send(s, "<red>/clan admin disband [Clan] <gray>- Clan auflösen");
                plugin.send(s, "<red>/clan admin import <gray>- Clans aus dem alten Skript übernehmen");
                plugin.send(s, "<red>/clan admin reload <gray>- Config neu laden");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!cmd.getName().equalsIgnoreCase("clan")) return out;
        boolean adm = sender.hasPermission("clans.admin");
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        if (a.length == 1) {
            out.addAll(SUBS);
            if (adm) out.add("admin");
        } else if (a.length == 2 && adm && a[0].equalsIgnoreCase("admin")) {
            out.addAll(ADMIN);
        } else if (a.length == 3 && adm && a[0].equalsIgnoreCase("admin") && a[1].equalsIgnoreCase("disband")) {
            for (Clan c : plugin.clans().all()) out.add(c.name);
        }
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
