package pt.henrique.betterchat.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pt.henrique.betterchat.BetterChat;
import pt.henrique.betterchat.config.ConfigManager;

public final class JoinQuitListener implements Listener {

    private final BetterChat plugin;

    public JoinQuitListener(BetterChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isJoinQuitMessagesEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Check for first join
        if (config.isFirstJoinMessageEnabled() && !player.hasPlayedBefore()) {
            String format = config.getFirstJoinFormat();
            Component component = plugin.getChatFormatter().formatJoinQuit(player, format);
            event.joinMessage(component);
            return;
        }

        String format = config.getJoinFormat();
        Component component = plugin.getChatFormatter().formatJoinQuit(player, format);
        event.joinMessage(component);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        ConfigManager config = plugin.getConfigManager();

        if (!config.isJoinQuitMessagesEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String format = config.getQuitFormat();
        Component component = plugin.getChatFormatter().formatJoinQuit(player, format);
        event.quitMessage(component);
    }
}

