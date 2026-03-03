package bsx.de.shiftly.service;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;

/**
 * Service for moving players between servers
 */
public class MoveService {

    private final ProxyServer proxyServer;

    public MoveService(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    /**
     * Find a player by name
     * @param name player name
     * @return optional player
     */
    public Optional<Player> findPlayer(String name) {
        return proxyServer.getPlayer(name);
    }

    /**
     * Find a server by name
     * @param name server name
     * @return optional server
     */
    public Optional<RegisteredServer> findServer(String name) {
        return proxyServer.getServer(name);
    }

    /**
     * Move a player to a server
     * @param player player to move
     * @param server destination server
     */
    public void movePlayer(Player player, RegisteredServer server) {
        player.createConnectionRequest(server).connect();
    }
}
