package pt.henrique.betterchat.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pt.henrique.betterchat.BetterChat;

import java.util.logging.Logger;

public final class PlaceholderResolver {

    private final BetterChat plugin;
    private final Logger logger;
    private boolean papiAvailable;

    public PlaceholderResolver(BetterChat plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        detectPapi();
    }

    /**
     * Detect PlaceholderAPI presence via plugin manager.
     * Safe to call at any time (reload, startup, etc.).
     */
    public void detectPapi() {
        var papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        papiAvailable = papi != null && papi.isEnabled();
    }

    public boolean isPapiAvailable() {
        return papiAvailable;
    }

    /**
     * Replace built-in placeholders (except {message}).
     */
    private String replaceBuiltIn(Player player, String text) {
        String result = text;
        result = result.replace("{player}", player.getName());
        result = result.replace("{displayname}", player.getName());
        result = result.replace("{world}", player.getWorld().getName());
        result = result.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        return result;
    }

    /**
     * Resolve PAPI placeholders directly. No reflection, no error counter.
     * If PAPI is absent, the text is returned unchanged.
     * Catches NoClassDefFoundError as a safety net in case the plugin is present
     * but the class somehow can't be loaded.
     */
    private String resolvePapi(Player player, String text) {
        if (!papiAvailable || !text.contains("%")) return text;
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (NoClassDefFoundError e) {
            logger.severe("PlaceholderAPI class not found at runtime despite plugin being present. Disabling PAPI integration.");
            papiAvailable = false;
            return text;
        } catch (Exception e) {
            logger.warning("PAPI resolution failed: " + e.getMessage());
            return text;
        }
    }

    /**
     * Resolve all placeholders in a chat format string for the given player.
     * <p>
     * Order of operations:
     * <ol>
     *   <li>Replace built-in placeholders (except {message})</li>
     *   <li>Resolve PlaceholderAPI %placeholders% (if available)</li>
     *   <li>Insert the player message ({message}) last — so it never goes through PAPI</li>
     * </ol>
     *
     * @param player  the player context
     * @param text    the format string
     * @param message the raw message content (nullable; only used for {message} placeholder)
     * @return resolved string ready for MiniMessage parsing
     */
    public String resolve(Player player, String text, String message) {
        boolean debugFlow = plugin.getConfigManager().isDebugPlaceholderFlow();

        // 1) Built-in placeholders (except {message})
        String result = replaceBuiltIn(player, text);

        if (debugFlow) {
            logger.info("[PlaceholderFlow] Raw format: " + text);
            logger.info("[PlaceholderFlow] After built-in: " + result);
        }

        // 2) PlaceholderAPI — must run BEFORE {message} insertion so player content
        //    doesn't pass through PAPI, and BEFORE MiniMessage parsing so PAPI output
        //    (which may contain MiniMessage tags like <red>) is parsed correctly.
        if (papiAvailable) {
            result = resolvePapi(player, result);
            if (debugFlow) {
                logger.info("[PlaceholderFlow] After PAPI: " + result);
            }
        } else if (debugFlow) {
            logger.info("[PlaceholderFlow] PAPI not available, skipping");
        }

        // 3) Insert message content LAST (already escaped by caller if needed)
        if (message != null) {
            result = result.replace("{message}", message);
        }

        if (debugFlow) {
            logger.info("[PlaceholderFlow] Final (before MiniMessage parse): " + result);
        }

        return result;
    }

    /**
     * Resolve without message (for join/quit).
     */
    public String resolve(Player player, String text) {
        return resolve(player, text, null);
    }

    /**
     * Debug: resolve all placeholders step-by-step and return each stage.
     * Used by the /betterchat debugpapi and /betterchat debugformat commands.
     *
     * @param player the player context
     * @param text   the format string
     * @return array of [raw, afterBuiltIn, afterPapi, finalBeforeParse]
     */
    public String[] resolveDebug(Player player, String text) {
        String raw = text;

        // Stage 1: built-in
        String afterBuiltIn = replaceBuiltIn(player, text);

        // Stage 2: PAPI
        String afterPapi;
        if (papiAvailable) {
            afterPapi = resolvePapi(player, afterBuiltIn);
        } else {
            afterPapi = afterBuiltIn + " (PAPI not available)";
        }

        // Stage 3: final (same as afterPapi for non-chat; for chat, {message} would be inserted here)
        String finalString = afterPapi;

        return new String[]{raw, afterBuiltIn, afterPapi, finalString};
    }
}

