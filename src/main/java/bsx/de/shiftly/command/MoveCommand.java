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

        if (targetName.equalsIgnoreCase("-group")) {
            handleGroupMove(invocation, args);
            return;
        }

        if (args.length != 2) {
            invocation.source().sendMessage(messageConfig.get("messages.usage_move"));
            return;
        }

        handlePlayerMove(invocation, args);
    }

    private void handleGroupMove(Invocation invocation, String[] args) {
        if (!permissionHelper.checkAdmin(invocation.source()) &&
            !permissionHelper.checkMoveGroup(invocation.source())) {
            invocation.source().sendMessage(messageConfig.get("messages.no_permission"));
            return;
        }

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

        List<Player> groupTargets = luckPermsBridge.getPlayersInGroup(groupName);

        if (groupTargets.isEmpty()) {
            invocation.source().sendMessage(
                    Component.text("No players found in group '" + groupName + "'."));
            return;
        }

        var serverOpt = moveService.findServer(serverName);
        if (serverOpt.isEmpty()) {
            invocation.source().sendMessage(messageConfig.get("messages.server_not_found"));
            return;
        }

        RegisteredServer server = serverOpt.get();
        String targetServer = server.getServerInfo().getName();

        groupTargets.forEach(p -> moveService.movePlayer(p, server));

        String scope = messageConfig.getMessageScope();

        Component globalMsg = messageConfig.get("messages.moved_global_group")
                .replaceText(builder -> builder.matchLiteral("<group>").replacement(groupName))
                .replaceText(builder -> builder.matchLiteral("<server>").replacement(targetServer));

        Component adminMsg = messageConfig.get("messages.moved_admin_group")
                .replaceText(builder -> builder.matchLiteral("<group>").replacement(groupName))
                .replaceText(builder -> builder.matchLiteral("<server>").replacement(targetServer));

        Component youMsg = messageConfig.get("messages.moved_you_group")
                .replaceText(builder -> builder.matchLiteral("<group>").replacement(groupName))
                .replaceText(builder -> builder.matchLiteral("<server>").replacement(targetServer));

        for (Player player : proxyServer.getAllPlayers()) {
            boolean isTarget = groupTargets.contains(player);
            boolean isAdmin = permissionHelper.checkAdmin(player);

            if (scope.equals("TARGET_AND_ADMINS")) {
                if (isTarget) {
                    player.sendMessage(youMsg);
                } else if (isAdmin) {
                    player.sendMessage(adminMsg);
                }
            } else {
                if (isTarget) {
                    player.sendMessage(youMsg);
                } else if (isAdmin) {
                    player.sendMessage(adminMsg);
                } else {
                    player.sendMessage(globalMsg);
                }
            }
        }
    }

    private void handlePlayerMove(Invocation invocation, String[] args) {
        String targetName = args[0];
        String serverName = args[1];

        if (!permissionHelper.checkAdmin(invocation.source()) &&
            !permissionHelper.checkMove(invocation.source())) {
            invocation.source().sendMessage(messageConfig.get("messages.no_permission"));
            return;
        }

        var serverOpt = moveService.findServer(serverName);
        if (serverOpt.isEmpty()) {
            invocation.source().sendMessage(messageConfig.get("messages.server_not_found"));
            return;
        }

        RegisteredServer server = serverOpt.get();
        String targetServer = server.getServerInfo().getName();

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

        targets.forEach(p -> moveService.movePlayer(p, server));

        String scope = messageConfig.getMessageScope();

        Component globalMsg = messageConfig.get("messages.moved_global")
                .replaceText(builder -> builder.matchLiteral("<player>").replacement(targetName))
                .replaceText(builder -> builder.matchLiteral("<server>").replacement(targetServer));

        Component adminMsg = messageConfig.get("messages.moved_admin")
                .replaceText(builder -> builder.matchLiteral("<player>").replacement(targetName))
                .replaceText(builder -> builder.matchLiteral("<server>").replacement(targetServer));

        Component youMsg = messageConfig.get("messages.moved_you")
                .replaceText(builder -> builder.matchLiteral("<server>").replacement(targetServer));

        for (Player player : proxyServer.getAllPlayers()) {
            boolean isTarget = targets.contains(player);
            boolean isAdmin = permissionHelper.checkAdmin(player);

            if (scope.equals("TARGET_AND_ADMINS")) {
                if (isTarget) {
                    player.sendMessage(youMsg);
                } else if (isAdmin) {
                    player.sendMessage(adminMsg);
                }
            } else {
                if (isTarget) {
                    player.sendMessage(youMsg);
                } else if (isAdmin) {
                    player.sendMessage(adminMsg);
                } else {
                    player.sendMessage(globalMsg);
                }
            }
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

        if (args.length == 0 || args.length == 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";

            List<String> result = new ArrayList<>();

            if (input.isEmpty() || "@a".startsWith(input)) {
                result.add("@a");
            }
            if (luckPermsBridge.isAvailable() && (input.isEmpty() || "-group".startsWith(input))) {
                result.add("-group");
            }

            proxyServer.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input))
                    .sorted()
                    .forEach(result::add);

            return result;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("-group") && luckPermsBridge.isAvailable()) {
            String input = args[1].toLowerCase();
            return luckPermsBridge.getGroupNames().stream()
                    .filter(n -> input.isEmpty() || n.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return proxyServer.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(n -> input.isEmpty() || n.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

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
