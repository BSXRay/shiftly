package bsx.de.shiftly.command;

import bsx.de.shiftly.config.MessageConfig;
import bsx.de.shiftly.service.MoveService;
import bsx.de.shiftly.util.LuckPermsBridge;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MoveCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final MoveService moveService;
    private final MessageConfig messageConfig;
    private final LuckPermsBridge luckPermsBridge;

    public MoveCommand(ProxyServer proxyServer,
                       MoveService moveService,
                       LuckPermsBridge bridge,
                       MessageConfig messageConfig) {

        this.proxyServer = proxyServer;
        this.moveService = moveService;
        this.luckPermsBridge = bridge;
        this.messageConfig = messageConfig;
    }

    @Override
    public void execute(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length == 0) {
            invocation.source().sendMessage(
                    messageConfig.get("messages.usage_move"));
            return;
        }

        String targetName = args[0];

        if (targetName.equalsIgnoreCase("-group")) {

            if (!invocation.source().hasPermission("shiftly.admin.move.group")) {
                invocation.source().sendMessage(
                        Component.text("No permission."));
                return;
            }

            if (!luckPermsBridge.isAvailable()) {
                invocation.source().sendMessage(
                        Component.text("LuckPerms is not available."));
                return;
            }

            if (args.length != 3) {
                invocation.source().sendMessage(
                        Component.text("Usage: /move -group <group> <server>"));
                return;
            }

            String groupName = args[1];
            String serverName = args[2];

            List<Player> groupTargets = luckPermsBridge.getPlayersInGroup(groupName);

            if (groupTargets.isEmpty()) {
                invocation.source().sendMessage(
                        Component.text("No players found in group '" + groupName + "'."));
                return;
            }

            var serverOpt = moveService.findServer(serverName);

            if (serverOpt.isEmpty()) {
                invocation.source().sendMessage(
                        messageConfig.get("messages.server_not_found"));
                return;
            }

            final RegisteredServer server = serverOpt.get();

            String scope = messageConfig.getMessageScope();
            Component baseMessageTemplate = messageConfig.get("messages.moved_success");

            groupTargets.forEach(player -> {
                moveService.movePlayer(player, server);

                Component message = baseMessageTemplate
                        .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.getUsername()))
                        .replaceText(builder -> builder.matchLiteral("<server>").replacement(server.getServerInfo().getName()));

                switch (scope) {
                    case "TARGET_ONLY" -> player.sendMessage(message);
                    case "TARGET_AND_ADMINS" -> {
                        player.sendMessage(message);
                        proxyServer.getAllPlayers().stream()
                                .filter(p -> p.hasPermission("shiftly.admin.move"))
                                .forEach(p -> p.sendMessage(message));
                    }
                    default -> proxyServer.getAllPlayers().forEach(p -> p.sendMessage(message));
                }
            });

            return;
        }

        if (args.length != 2) {
            invocation.source().sendMessage(
                    messageConfig.get("messages.usage_move"));
            return;
        }

        String serverName = args[1];

        var serverOpt = moveService.findServer(serverName);

        if (serverOpt.isEmpty()) {
            invocation.source().sendMessage(
                    messageConfig.get("messages.server_not_found"));
            return;
        }

        RegisteredServer server = serverOpt.get();

        List<Player> targets = new ArrayList<>();

        if (targetName.equalsIgnoreCase("@a")) {
            targets.addAll(proxyServer.getAllPlayers());
        } else {
            moveService.findPlayer(targetName).ifPresent(targets::add);
        }

        if (targets.isEmpty()) {
            invocation.source().sendMessage(
                    messageConfig.get("messages.player_not_found"));
            return;
        }

        String scope = messageConfig.getMessageScope();

        Component baseMessageTemplate =
                messageConfig.get("messages.moved_success");

        targets.forEach(player -> {

            moveService.movePlayer(player, server);

            Component message = baseMessageTemplate
                    .replaceText(builder -> builder
                            .matchLiteral("<player>")
                            .replacement(player.getUsername()))
                    .replaceText(builder -> builder
                            .matchLiteral("<server>")
                            .replacement(server.getServerInfo().getName()));

            switch (scope) {

                case "TARGET_ONLY" -> player.sendMessage(message);

                case "TARGET_AND_ADMINS" -> {

                    player.sendMessage(message);

                    proxyServer.getAllPlayers().stream()
                            .filter(p -> p.hasPermission("shiftly.admin.move"))
                            .forEach(p -> p.sendMessage(message));
                }

                default -> proxyServer.getAllPlayers()
                        .forEach(p -> p.sendMessage(message));
            }
        });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {

        if (invocation.arguments().length >= 1 && invocation.arguments()[0].equalsIgnoreCase("-group")) {
            return invocation.source().hasPermission("shiftly.admin.move.group");
        }

        return invocation.source().hasPermission("shiftly.admin.move");
    }

    @Override
    public List<String> suggest(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length == 0) {
            List<String> result = new ArrayList<>();
            result.add("@a");
            if (luckPermsBridge.isAvailable()) {
                result.add("-group");
            }
            result.addAll(proxyServer.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .sorted()
                    .collect(Collectors.toList()));
            return result;
        }

        if (args.length == 1) {

            String input = args[0].toLowerCase();

            List<String> players = proxyServer.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> input.isEmpty() ||
                            name.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());

            List<String> result = new ArrayList<>();

            result.add("@a");
            if (luckPermsBridge.isAvailable()) {
                result.add("-group");
            }
            result.addAll(players);

            return result;
        }

        if (args.length == 2) {

            if (args[0].equalsIgnoreCase("-group") && luckPermsBridge.isAvailable()) {
                String input = args[1].toLowerCase();
                return luckPermsBridge.getGroupNames().stream()
                        .filter(n -> input.isEmpty() || n.toLowerCase().startsWith(input))
                        .sorted()
                        .collect(Collectors.toList());
            }

            String input = args[1].toLowerCase();

            return proxyServer.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(n -> input.isEmpty() ||
                            n.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("-group")) {
            String input = args[2].toLowerCase();

            return proxyServer.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(n -> input.isEmpty() ||
                            n.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
