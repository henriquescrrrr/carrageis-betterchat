package pt.henrique.betterchat.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pt.henrique.betterchat.BetterChat;
import pt.henrique.betterchat.chat.ChatFormatter;
import pt.henrique.betterchat.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public final class ChatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** Hard cap on how many distinct players a single message may ping (anti sound-spam). */
    private static final int MAX_MENTIONS_PER_MESSAGE = 5;

    private final BetterChat plugin;

    public ChatListener(BetterChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isChatFormatEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Get the raw plain-text message. Kept as a String only to build a locked Component below;
        // it is NEVER concatenated into a MiniMessage template.
        String rawMessage = PLAIN.serialize(event.originalMessage());

        boolean allowFormatting = config.isAllowMiniMessageWithPermission()
                && player.hasPermission("betterchat.format");

        // Build the player message as a safe Component. Even with formatting permission the player
        // gets cosmetic tags only (no <click>/<hover>/<insert>/<newline>); mentions are highlighted
        // with the trusted config fragment. This makes template injection impossible.
        Component messageComponent = config.isMentionEnabled()
                ? buildMessageWithMentions(player, rawMessage, allowFormatting, config)
                : plugin.getChatFormatter().buildMessageComponent(rawMessage, allowFormatting);

        // Build the final component: template (trusted) + message (locked Component placeholder).
        String format = config.getChatFormat();
        Component rendered = plugin.getChatFormatter().formatChat(player, messageComponent, format);

        // Set the renderer to return our custom component.
        event.renderer((source, sourceDisplayName, message, audience) -> rendered);
    }

    /**
     * Build the player message Component while highlighting {@code @Name} mentions.
     * <p>
     * The raw text is scanned left-to-right for {@code @<onlinePlayerName>} tokens. Text between
     * mentions is rendered through the safe player resolver (cosmetic-only or literal); each matched
     * mention is rendered with the trusted mention-highlight fragment. This keeps the highlight (a
     * trusted config template) working without ever parsing player free-text as full MiniMessage.
     * <p>
     * At most {@link #MAX_MENTIONS_PER_MESSAGE} distinct players are pinged per message to prevent
     * mass sound-spam / harassment.
     */
    private Component buildMessageWithMentions(Player sender, String raw, boolean allowFormatting,
                                               ConfigManager config) {
        ChatFormatter formatter = plugin.getChatFormatter();

        // Snapshot online players once (immutable copy on Paper) to avoid O(online) rescans.
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());

        Component result = Component.empty();
        StringBuilder pending = new StringBuilder();  // buffered non-mention text
        List<Player> toPing = new ArrayList<>();
        int i = 0;
        int len = raw.length();

        while (i < len) {
            char c = raw.charAt(i);
            if (c == '@') {
                Player matched = null;
                String matchedName = null;
                // Longest-name-first match so "@Steve" isn't shadowed by a hypothetical "@St".
                int bestLen = -1;
                for (Player p : online) {
                    String name = p.getName();
                    int nameLen = name.length();
                    if (nameLen > bestLen
                            && raw.regionMatches(true, i + 1, name, 0, nameLen)) {
                        matched = p;
                        matchedName = name;
                        bestLen = nameLen;
                    }
                }
                if (matched != null && toPing.size() < MAX_MENTIONS_PER_MESSAGE) {
                    // Flush buffered text as a safe component.
                    if (pending.length() > 0) {
                        result = result.append(formatter.buildMessageComponent(pending.toString(), allowFormatting));
                        pending.setLength(0);
                    }
                    // Append the mention highlight. The format is operator-authored, but the name
                    // spliced into it is not: offline-mode logins are not username-validated, so
                    // the name is escaped before the fragment is parsed as full MiniMessage.
                    String fragment = config.getMentionFormat()
                            .replace("{mentioned}", ChatFormatter.escapeTags(matchedName));
                    result = result.append(ChatFormatter.renderTrusted(fragment));
                    if (!toPing.contains(matched)) {
                        toPing.add(matched);
                    }
                    i += 1 + matchedName.length();
                    continue;
                }
                // No match (or ping cap reached): treat '@' as ordinary text.
                pending.append(c);
                i++;
            } else {
                pending.append(c);
                i++;
            }
        }

        if (pending.length() > 0) {
            result = result.append(formatter.buildMessageComponent(pending.toString(), allowFormatting));
        }

        // Play the mention sound to each pinged player (safe off-main: playSound just sends a packet).
        playMentionSounds(toPing, config);

        return result;
    }

    private void playMentionSounds(List<Player> pinged, ConfigManager config) {
        String soundName = config.getMentionSound();
        if (soundName == null || soundName.isEmpty() || pinged.isEmpty()) {
            return;
        }
        Sound sound;
        try {
            sound = Sound.sound(Key.key(soundName), Sound.Source.PLAYER, 1.0f, 1.0f);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid mention sound: " + soundName);
            return;
        }
        for (Player p : pinged) {
            p.playSound(sound);
        }
    }
}
