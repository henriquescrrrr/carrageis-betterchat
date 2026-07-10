package pt.henrique.betterchat.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pt.henrique.betterchat.BetterChat;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class BetterChatCommand {

    private BetterChatCommand() {
    }

    public static void register(BetterChat plugin, Commands commands) {
        commands.register(
                Commands.literal("betterchat")
                        .then(Commands.literal("reload")
                                .requires(source -> source.getSender().hasPermission("betterchat.admin"))
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    try {
                                        plugin.getConfigManager().reload();
                                        plugin.getLangManager().reload();
                                        plugin.getPlaceholderResolver().detectPapi();

                                        boolean papiNow = plugin.getPlaceholderResolver().isPapiAvailable();
                                        plugin.getLogger().info("PlaceholderAPI detected: " + (papiNow ? "yes" : "no"));

                                        sender.sendMessage(plugin.getLangManager().get("reload-success"));
                                    } catch (Exception e) {
                                        sender.sendMessage(plugin.getLangManager().get("reload-fail"));
                                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error reloading BetterChat", e);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("debugpapi")
                                .requires(source -> source.getSender().hasPermission("betterchat.admin"))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String input = builder.getRemainingLowerCase();
                                            for (Player p : Bukkit.getOnlinePlayers()) {
                                                if (p.getName().toLowerCase().startsWith(input)) {
                                                    builder.suggest(p.getName());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> executeDebugFormat(plugin, ctx))
                                )
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    if (sender instanceof Player player) {
                                        return executeDebugFormatForPlayer(plugin, sender, player);
                                    }
                                    sender.sendMessage(Component.text("Usage: /betterchat debugpapi <player>", NamedTextColor.YELLOW));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("debugformat")
                                .requires(source -> source.getSender().hasPermission("betterchat.admin"))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String input = builder.getRemainingLowerCase();
                                            for (Player p : Bukkit.getOnlinePlayers()) {
                                                if (p.getName().toLowerCase().startsWith(input)) {
                                                    builder.suggest(p.getName());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> executeDebugFormat(plugin, ctx))
                                )
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    if (sender instanceof Player player) {
                                        return executeDebugFormatForPlayer(plugin, sender, player);
                                    }
                                    sender.sendMessage(Component.text("Usage: /betterchat debugformat <player>", NamedTextColor.YELLOW));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            sender.sendMessage(plugin.getLangManager().get("usage"));
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "BetterChat admin command",
                List.of("bchat")
        );
    }

    private static int executeDebugFormat(BetterChat plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String targetName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        return executeDebugFormatForPlayer(plugin, sender, target);
    }

    private static int executeDebugFormatForPlayer(BetterChat plugin, CommandSender sender, Player target) {
        String chatFormat = plugin.getConfigManager().getChatFormat();
        String joinFormat = plugin.getConfigManager().getJoinFormat();
        String quitFormat = plugin.getConfigManager().getQuitFormat();
        boolean papiAvailable = plugin.getPlaceholderResolver().isPapiAvailable();

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("=== BetterChat Format Debug ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Target player: ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("PlaceholderAPI detected: ", NamedTextColor.GRAY)
                .append(Component.text(papiAvailable ? "yes" : "no", papiAvailable ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.empty());

        // Chat format debug
        sender.sendMessage(Component.text("--- Chat Format ---", NamedTextColor.AQUA));
        String[] chatStages = plugin.getPlaceholderResolver().resolveDebug(target, chatFormat);
        printStages(sender, chatStages);

        // Join format debug
        sender.sendMessage(Component.text("--- Join Format ---", NamedTextColor.AQUA));
        String[] joinStages = plugin.getPlaceholderResolver().resolveDebug(target, joinFormat);
        printStages(sender, joinStages);

        // Quit format debug
        sender.sendMessage(Component.text("--- Quit Format ---", NamedTextColor.AQUA));
        String[] quitStages = plugin.getPlaceholderResolver().resolveDebug(target, quitFormat);
        printStages(sender, quitStages);

        sender.sendMessage(Component.text("================================", NamedTextColor.GOLD));
        sender.sendMessage(Component.empty());

        return Command.SINGLE_SUCCESS;
    }

    private static void printStages(CommandSender sender, String[] stages) {
        sender.sendMessage(Component.text("1) Raw config format:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("   " + stages[0], NamedTextColor.WHITE));

        sender.sendMessage(Component.text("2) After built-in placeholders:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("   " + stages[1], NamedTextColor.WHITE));

        sender.sendMessage(Component.text("3) After PAPI resolution:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("   " + stages[2], NamedTextColor.WHITE));

        sender.sendMessage(Component.text("4) Final MiniMessage string:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("   " + stages[3], NamedTextColor.WHITE));

        sender.sendMessage(Component.empty());
    }
}



