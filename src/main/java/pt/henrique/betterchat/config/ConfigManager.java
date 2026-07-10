package pt.henrique.betterchat.config;

import org.bukkit.configuration.file.FileConfiguration;
import pt.henrique.betterchat.BetterChat;

public final class ConfigManager {

    private final BetterChat plugin;

    // Features
    private boolean chatFormatEnabled;
    private boolean joinQuitMessagesEnabled;
    private boolean firstJoinMessageEnabled;
    private boolean allowMiniMessageWithPermission;

    // Mention
    private boolean mentionEnabled;
    private String mentionSound;
    private String mentionFormat;

    // Formats
    private String chatFormat;
    private String joinFormat;
    private String quitFormat;
    private String firstJoinFormat;

    // Language
    private String language;

    // Debug
    private boolean debugPapi;
    private boolean debugPlaceholderFlow;

    public ConfigManager(BetterChat plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration cfg = plugin.getConfig();

        // Features
        chatFormatEnabled = cfg.getBoolean("features.chat-format", true);
        joinQuitMessagesEnabled = cfg.getBoolean("features.join-quit-messages", true);
        firstJoinMessageEnabled = cfg.getBoolean("features.first-join-message", true);
        allowMiniMessageWithPermission = cfg.getBoolean("features.allow-message-minimessage-with-permission", true);

        // Mention
        mentionEnabled = cfg.getBoolean("features.mention-highlight.enabled", false);
        mentionSound = cfg.getString("features.mention-highlight.sound", "entity.experience_orb.pickup");
        mentionFormat = cfg.getString("features.mention-highlight.format", "<yellow>@{mentioned}</yellow>");

        // Formats
        chatFormat = cfg.getString("formats.chat",
                "%serverroles_prefix_formatted% %serverroles_color%{player}<reset>: <white>{message}</white>");
        joinFormat = cfg.getString("formats.join", "<green>+ <gray>{player}</gray></green>");
        quitFormat = cfg.getString("formats.quit", "<red>- <gray>{player}</gray></red>");
        firstJoinFormat = cfg.getString("formats.first-join",
                "<gold>★ <gray>{player}</gray> <yellow>joined for the first time!</yellow></gold>");

        // Language (default pt_PT)
        language = cfg.getString("language", "pt_PT");

        // Debug
        debugPapi = cfg.getBoolean("debug.papi", false);
        debugPlaceholderFlow = cfg.getBoolean("debug.placeholder-flow", false);
    }

    public void reload() {
        load();
    }

    // Getters

    public boolean isChatFormatEnabled() {
        return chatFormatEnabled;
    }

    public boolean isJoinQuitMessagesEnabled() {
        return joinQuitMessagesEnabled;
    }

    public boolean isFirstJoinMessageEnabled() {
        return firstJoinMessageEnabled;
    }

    public boolean isAllowMiniMessageWithPermission() {
        return allowMiniMessageWithPermission;
    }

    public boolean isMentionEnabled() {
        return mentionEnabled;
    }

    public String getMentionSound() {
        return mentionSound;
    }

    public String getMentionFormat() {
        return mentionFormat;
    }

    public String getChatFormat() {
        return chatFormat;
    }

    public String getJoinFormat() {
        return joinFormat;
    }

    public String getQuitFormat() {
        return quitFormat;
    }

    public String getFirstJoinFormat() {
        return firstJoinFormat;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDebugPapi() {
        return debugPapi;
    }

    public boolean isDebugPlaceholderFlow() {
        return debugPlaceholderFlow;
    }
}

