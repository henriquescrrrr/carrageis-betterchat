package pt.henrique.betterchat;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import pt.henrique.betterchat.chat.ChatFormatter;
import pt.henrique.betterchat.command.BetterChatCommand;
import pt.henrique.betterchat.config.ConfigManager;
import pt.henrique.betterchat.config.LangManager;
import pt.henrique.betterchat.listener.ChatListener;
import pt.henrique.betterchat.listener.JoinQuitListener;
import pt.henrique.betterchat.placeholder.PlaceholderResolver;

public final class BetterChat extends JavaPlugin {

    private ConfigManager configManager;
    private LangManager langManager;
    private PlaceholderResolver placeholderResolver;
    private ChatFormatter chatFormatter;

    @Override
    public void onEnable() {
        // Register permissions in code. The `permissions:` block in paper-plugin.yml is IGNORED by
        // Paper, so without this betterchat.format would fall through to the implicit op default
        // instead of the intended `false`.
        registerPermissions();

        // Initialize managers
        configManager = new ConfigManager(this);
        langManager = new LangManager(this);
        placeholderResolver = new PlaceholderResolver(this);
        chatFormatter = new ChatFormatter(this);

        // Log PAPI status
        boolean papiDetected = placeholderResolver.isPapiAvailable();
        getLogger().info("PlaceholderAPI detected: " + (papiDetected ? "yes" : "no"));
        getLogger().info("PAPI placeholders enabled: " + (papiDetected ? "yes" : "no"));

        if (configManager.isDebugPapi()) {
            getLogger().info("[Debug] PAPI debug mode is ON — use /betterchat debugpapi <player> to inspect resolution");
            getLogger().info("[Debug] Chat format: " + configManager.getChatFormat());
        }

        if (configManager.isDebugPlaceholderFlow()) {
            getLogger().info("[Debug] Placeholder flow logging is ON — each chat/join/quit will log all resolution stages");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);

        getLogger().info("BetterChat has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BetterChat has been disabled.");
    }

    /**
     * Register plugin permission nodes at runtime.
     * <p>
     * Paper ignores the {@code permissions:} section of {@code paper-plugin.yml}, so nodes must be
     * registered here for their intended defaults to apply (and to be visible to permission plugins).
     * {@code betterchat.admin} defaults to op; {@code betterchat.format} defaults to false so the
     * (cosmetic-only) chat-formatting capability must be granted explicitly rather than auto-given to
     * every op.
     */
    private void registerPermissions() {
        var pm = getServer().getPluginManager();
        registerPermission(pm, "betterchat.admin", org.bukkit.permissions.PermissionDefault.OP,
                "Access to /betterchat admin commands (reload, debugpapi, debugformat).");
        registerPermission(pm, "betterchat.format", org.bukkit.permissions.PermissionDefault.FALSE,
                "Allows using cosmetic MiniMessage formatting (colours/gradients) in chat messages.");
    }

    private void registerPermission(org.bukkit.plugin.PluginManager pm, String node,
                                    org.bukkit.permissions.PermissionDefault def, String description) {
        // Guard against re-registration (e.g. after a server /reload re-enables the plugin).
        if (pm.getPermission(node) != null) {
            return;
        }
        pm.addPermission(new org.bukkit.permissions.Permission(node, description, def));
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onLoad() {
        // Register commands using Paper's Lifecycle event API (Brigadier)
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            BetterChatCommand.register(this, commands);
        });
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public PlaceholderResolver getPlaceholderResolver() {
        return placeholderResolver;
    }

    public ChatFormatter getChatFormatter() {
        return chatFormatter;
    }
}
