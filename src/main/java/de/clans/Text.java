package de.clans;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Text {
    public static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {}

    /** MiniMessage -> Component ohne Standard-Kursiv (für Item-Namen/Lore). */
    public static Component mm(String s) {
        return MM.deserialize(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static String esc(String s) {
        return MM.escapeTags(s);
    }
}
