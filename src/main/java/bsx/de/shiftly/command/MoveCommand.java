package bsx.de.shiftly.command;

import bsx.de.shiftly.config.MessageConfig;
import bsx.de.shiftly.service.MoveService;

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

    public MoveCommand(ProxyServer proxyServer,
                       MoveService moveService,
                       Object bridge,
                       MessageConfig messageConfig) {

        this.proxyServer = proxyServer;
        this.moveService = moveService;
        this.messageConfig = messageConfig;
    }

    @Override
    public void execute(Invocation invocation) {

        if (invocation.arguments().length != 2) {

            invocation.source().sendMessage(
                    messageConfig.get("messages.usage_move"));

            return;
        }

        String targetName = invocation.arguments()[0];
        String serverName = invocation.arguments()[1];

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
    public List<String> suggest(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length <= 1) {

            String input = args.length == 0 ? "" : args[0].toLowerCase();

            List<String> players = proxyServer.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> input.isEmpty() ||
                            name.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());

            List<String> result = new ArrayList<>();

            result.add("@a");
            result.addAll(players);

            return result;
        }

        if (args.length == 2) {

            String input = args[1].toLowerCase();

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