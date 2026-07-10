package pt.henrique.betterchat.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import pt.henrique.betterchat.BetterChat;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class LangManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final BetterChat plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String prefix = "";

    public LangManager(BetterChat plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        messages.clear();
        String locale = plugin.getConfigManager().getLanguage();

        // Save default lang files from JAR to data folder
        saveDefaultLang("en_US");
        saveDefaultLang("pt_PT");

        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        YamlConfiguration yaml;

        if (langFile.exists()) {
            yaml = YamlConfiguration.loadConfiguration(langFile);
        } else {
            // Fall back to bundled pt_PT (default locale)
            InputStream is = plugin.getResource("lang/pt_PT.yml");
            if (is == null) {
                plugin.getLogger().warning("Could not find default language file!");
                return;
            }
            yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        for (String key : yaml.getKeys(false)) {
            messages.put(key, yaml.getString(key, ""));
        }

        prefix = messages.getOrDefault("prefix", "");
    }

    public void reload() {
        load();
    }

    /**
     * Get a raw message string from the language file.
     * Replaces {prefix} with the configured prefix.
     */
    public String getRaw(String key) {
        String msg = messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
        return msg.replace("{prefix}", prefix);
    }

    /**
     * Get a parsed Component from the language file.
     */
    public Component get(String key) {
        return MINI.deserialize(getRaw(key));
    }

    /**
     * Get a parsed Component with placeholder replacements.
     */
    public Component get(String key, Map<String, String> replacements) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        return MINI.deserialize(raw);
    }

    private void saveDefaultLang(String locale) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        File file = new File(langDir, locale + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + locale + ".yml", false);
        }
    }
}

