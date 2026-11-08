package de.clans;

import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Clan {
    public final String key;
    public String name;
    public UUID owner;
    public long created = System.currentTimeMillis();
    public final LinkedHashMap<UUID, String> members = new LinkedHashMap<>();
    public String c1 = "";
    public String c2 = "";
    public int kills;
    public int deaths;
    public int blocks;
    /** Keys verbündeter Clans (gespeichert). */
    public final Set<String> allies = new LinkedHashSet<>();
    /** Keys von Clans, die uns eine Allianz angeboten haben (temporär). */
    public final Set<String> allyRequests = new LinkedHashSet<>();
    /** Eingeladene Spieler -> Ablaufzeit in ms (temporär). */
    public final Map<UUID, Long> invites = new HashMap<>();
    public Component tag = Component.empty();

    public Clan(String key, String name, UUID owner) {
        this.key = key;
        this.name = name;
        this.owner = owner;
    }

    public double kd() {
        if (deaths <= 0) return kills;
        return Math.round(((double) kills / deaths) * 100.0) / 100.0;
    }

    public String leaderName() {
        return members.getOrDefault(owner, "?");
    }
}
