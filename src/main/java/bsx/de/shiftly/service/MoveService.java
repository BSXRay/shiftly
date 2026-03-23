package bsx.de.shiftly.service;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;

public class MoveService {

    private final ProxyServer proxyServer;

    public MoveService(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    public Optional<Player> findPlayer(String name) {
        return proxyServer.getPlayer(name);
    }

    public Optional<RegisteredServer> findServer(String name) {
        return proxyServer.getServer(name);
    }

    public void movePlayer(Player player, RegisteredServer server) {
        player.createConnectionRequest(server).connect();
    }
}
