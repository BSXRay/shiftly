package bsx.de.shiftly.command;

import bsx.de.shiftly.config.MessageConfig;
import bsx.de.shiftly.service.MoveService;
import bsx.de.shiftly.util.LuckPermsBridge;
import bsx.de.shiftly.util.PermissionHelper;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /move command - move players between servers
 */
public class MoveCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final MoveService moveService;
    private final MessageConfig messageConfig;
    private final LuckPermsBridge luckPermsBridge;
    private final PermissionHelper permissionHelper;

    public MoveCommand(ProxyServer proxyServer,
                       MoveService moveService,
                       LuckPermsBridge bridge,
                       MessageConfig messageConfig,
                       PermissionHelper permissionHelper) {

        this.proxyServer = proxyServer;
        this.moveService = moveService;
        this.luckPermsBridge = bridge;
        this.messageConfig = messageConfig;
        this.permissionHelper = permissionHelper;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            invocation.source().sendMessage(messageConfig.get("messages.usage_move"));
            return;
        }

        String targetName = args[0];

        // Handle -group option
        if (targetName.equalsIgnoreCase("-group")) {
            handleGroupMove(invocation, args);
            return;
        }

        // Regular move: /move <player> <server>
        if (args.length != 2) {
            invocation.source().sendMessage(messageConfig.get("messages.usage_move"));
            return;
        }

        handlePlayerMove(invocation, args);
    }

    private void handleGroupMove(Invocation invocation, String[] args) {
        // Check permission
        if (!permissionHelper.checkAdmin(invocation.source()) && 
            !permissionHelper.checkMoveGroup(invocation.source())) {
            invocation.source().sendMessage(messageConfig.get("messages.no_permission"));
            return;
        }

        // Check LuckPerms availability
        if (!luckPermsBridge.isAvailable()) {
            invocation.source().sendMessage(Component.text("LuckPerms is not available."));
            return;
        }

        if (args.length != 3) {
            invocation.source().sendMessage(messageConfig.get("messages.usage_move_group"));
            return;
        }

        String groupName = args[1];
        String serverName = args[2];

        // Get players in group
        List<Player> groupTargets = luckPermsBridge.getPlayersInGroup(groupName);

        if (groupTargets.isEmpty()) {
            invocation.source().sendMessage(
                    Component.text("No players found in group '" + groupName + "'."));
            return;
        }

        // Find server
        var serverOpt = moveService.findServer(serverName);
        if (serverOpt.isEmpty()) {
            invocation.source().sendMessage(messageConfig.get("messages.server_not_found"));
            return;
        }

        RegisteredServer server = serverOpt.get();

        // Send single message based on scope
        String scope = messageConfig.getMessageScope();
        Component baseMessageTemplate = messageConfig.get("messages.moved_group_success");

        Component message = baseMessageTemplate
                .replaceText(builder -> builder.matchLiteral("<group>").replacement(groupName))
                .replaceText(builder -> builder.matchLiteral("<server>").replacement(server.getServerInfo().getName()));

        sendGroupMessage(scope, message, groupTargets);

        // Move all players
        for (Player player : groupTargets) {
            moveService.movePlayer(player, server);
        }
    }

    private void handlePlayerMove(Invocation invocation, String[] args) {
        String targetName = args[0];
        String serverName = args[1];

        // Check permission
        if (!permissionHelper.checkAdmin(invocation.source()) && 
            !permissionHelper.checkMove(invocation.source())) {
            invocation.source().sendMessage(messageConfig.get("messages.no_permission"));
            return;
        }

        // Find server
        var serverOpt = moveService.findServer(serverName);
        if (serverOpt.isEmpty()) {
            invocation.source().sendMessage(messageConfig.get("messages.server_not_found"));
            return;
        }

        RegisteredServer server = serverOpt.get();

        // Get targets
        List<Player> targets = new ArrayList<>();

        if (targetName.equalsIgnoreCase("@a")) {
            targets.addAll(proxyServer.getAllPlayers());
        } else {
            moveService.findPlayer(targetName).ifPresent(targets::add);
        }

        if (targets.isEmpty()) {
            invocation.source().sendMessage(messageConfig.get("messages.player_not_found"));
            return;
        }

        // Send messages
        String scope = messageConfig.getMessageScope();
        Component baseMessageTemplate = messageConfig.get("messages.moved_success");

        for (Player player : targets) {
            Component message = baseMessageTemplate
                    .replaceText(builder -> builder.matchLiteral("<player>").replacement(player.getUsername()))
                    .replaceText(builder -> builder.matchLiteral("<server>").replacement(server.getServerInfo().getName()));

            sendMessage(scope, message, player);
            moveService.movePlayer(player, server);
        }
    }

    private void sendMessage(String scope, Component message, Player target) {
        switch (scope) {
            case "TARGET_ONLY" -> target.sendMessage(message);
            case "TARGET_AND_ADMINS" -> {
                target.sendMessage(message);
                proxyServer.getAllPlayers().stream()
                        .filter(p -> permissionHelper.checkAdmin(p) || permissionHelper.checkMove(p))
                        .forEach(p -> p.sendMessage(message));
            }
            default -> proxyServer.getAllPlayers().forEach(p -> p.sendMessage(message));
        }
    }

    private void sendGroupMessage(String scope, Component message, List<Player> groupTargets) {
        switch (scope) {
            case "TARGET_ONLY" -> groupTargets.forEach(p -> p.sendMessage(message));
            case "TARGET_AND_ADMINS" -> {
                groupTargets.forEach(p -> p.sendMessage(message));
                proxyServer.getAllPlayers().stream()
                        .filter(p -> permissionHelper.checkAdmin(p) || permissionHelper.checkMove(p))
                        .forEach(p -> p.sendMessage(message));
            }
            default -> proxyServer.getAllPlayers().forEach(p -> p.sendMessage(message));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        if (permissionHelper.checkAdmin(invocation.source())) {
            return true;
        }

        if (invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("-group")) {
            return permissionHelper.checkMoveGroup(invocation.source());
        }

        return permissionHelper.checkMove(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        // First argument: target or @a or -group
        if (args.length == 0 || args.length == 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";

            List<String> result = new ArrayList<>();

            if (input.isEmpty() || "@a".startsWith(input)) {
                result.add("@a");
            }
            if (luckPermsBridge.isAvailable() && (input.isEmpty() || "-group".startsWith(input))) {
                result.add("-group");
            }

            // Add online players
            proxyServer.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input))
                    .sorted()
                    .forEach(result::add);

            return result;
        }

        // Second argument: group name (when -group is selected)
        if (args.length == 2 && args[0].equalsIgnoreCase("-group") && luckPermsBridge.isAvailable()) {
            String input = args[1].toLowerCase();
            return luckPermsBridge.getGroupNames().stream()
                    .filter(n -> input.isEmpty() || n.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Second argument: server name
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return proxyServer.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(n -> input.isEmpty() || n.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Third argument: server name (when -group is selected)
        if (args.length == 3 && args[0].equalsIgnoreCase("-group")) {
            String input = args[2].toLowerCase();
            return proxyServer.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(n -> input.isEmpty() || n.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
