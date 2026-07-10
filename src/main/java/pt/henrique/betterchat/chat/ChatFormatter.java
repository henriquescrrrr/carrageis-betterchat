package pt.henrique.betterchat.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.entity.Player;
import pt.henrique.betterchat.BetterChat;
import pt.henrique.betterchat.placeholder.PlaceholderResolver;

/**
 * Handles MiniMessage formatting with placeholder resolution.
 * <p>
 * Security model:
 * <ul>
 *   <li>The <b>template</b> (config/lang) is operator-authored and therefore trusted: it is parsed
 *       with the full MiniMessage tag set ({@link #MINI}).</li>
 *   <li>The <b>player message</b> is attacker-controlled and is NEVER parsed with the full tag set.
 *       It is inserted as a pre-built {@link Component} via {@link Placeholder#component}, built with
 *       either {@link #PLAYER_MINI} (a cosmetic-only resolver — colours/decorations/gradient, but no
 *       {@code <click>}/{@code <hover>}/{@code <insert>}/{@code <newline>}/{@code <selector>}) when the
 *       player has the formatting permission, or as literal text otherwise.</li>
 * </ul>
 * This makes it impossible for a player to inject interactive tags (clickable command runners,
 * spoofed hovers, multi-line messages) into the chat, regardless of the {@code betterchat.format}
 * permission.
 */
public final class ChatFormatter {

    /** Full MiniMessage — used ONLY for operator-authored templates (config/lang). */
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /**
     * Restricted MiniMessage for player-supplied text. Allows only cosmetic tags. The dangerous
     * interactive/structural tags (click, hover, insert, newline, selector, score, nbt, keybind,
     * translatable) are deliberately excluded so a player with {@code betterchat.format} can add
     * colour but cannot forge clickable commands, spoof hovers, or inject extra lines.
     */
    private static final MiniMessage PLAYER_MINI = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.gradient(),
                    StandardTags.rainbow(),
                    StandardTags.transition(),
                    StandardTags.reset()
            ))
            .build();

    /** Internal placeholder tag used to splice the player message in as a Component. */
    private static final String MESSAGE_TAG = "bc_message";

    private final BetterChat plugin;

    public ChatFormatter(BetterChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Format a chat message into a Component.
     * <p>
     * The template is resolved (built-ins + PAPI) with {@code {message}} swapped for an internal
     * placeholder tag, then parsed with the full MiniMessage tag set. The player's message is built
     * separately as a Component (cosmetic tags only when {@code allowFormatting}, otherwise literal)
     * and injected via a component placeholder, so player input can never introduce interactive tags.
     *
     * @param player          the sender
     * @param messageComponent the already-built player message Component (see
     *                          {@link #buildMessageComponent})
     * @param formatTemplate  the format template from config
     * @return the fully rendered Component
     */
    public Component formatChat(Player player, Component messageComponent, String formatTemplate) {
        PlaceholderResolver resolver = plugin.getPlaceholderResolver();
        // Resolve built-ins + PAPI, replacing {message} with the internal placeholder tag <bc_message>.
        String resolved = resolver.resolve(player, formatTemplate, "<" + MESSAGE_TAG + ">");
        return MINI.deserialize(resolved, Placeholder.component(MESSAGE_TAG, messageComponent));
    }

    /**
     * Build the player message as a Component from its raw plain text.
     * <p>
     * When {@code allowFormatting} is true the text is parsed with the cosmetic-only
     * {@link #PLAYER_MINI} resolver (colours/decorations/gradient, no interactive tags); otherwise it
     * is returned as literal text. Either way, interactive/structural tags typed by the player are
     * inert.
     *
     * @param rawMessage      the raw plain-text message from the player
     * @param allowFormatting whether the player may use cosmetic MiniMessage tags
     * @return a safe Component representing the player's message
     */
    public Component buildMessageComponent(String rawMessage, boolean allowFormatting) {
        if (allowFormatting) {
            // PLAYER_MINI cannot emit interactive tags; unknown/excluded tags are shown literally.
            return PLAYER_MINI.deserialize(rawMessage);
        }
        return Component.text(rawMessage);
    }

    /**
     * Format a join/quit message into a Component. These templates are operator-authored and contain
     * no player free-text, so the full MiniMessage tag set is safe here.
     *
     * @param player         the player
     * @param formatTemplate the format template from config
     * @return the fully rendered Component
     */
    public Component formatJoinQuit(Player player, String formatTemplate) {
        PlaceholderResolver resolver = plugin.getPlaceholderResolver();
        String resolved = resolver.resolve(player, formatTemplate);
        return MINI.deserialize(resolved);
    }

    /**
     * Render a trusted (operator-authored) MiniMessage fragment, e.g. the mention-highlight format.
     * Not for player-controlled input.
     */
    public static Component renderTrusted(String miniMessageFragment) {
        return MINI.deserialize(miniMessageFragment);
    }

    /**
     * Escape MiniMessage tags in a string (kept for compatibility / defensive use).
     */
    public static String escapeTags(String input) {
        return MINI.escapeTags(input);
    }
}
