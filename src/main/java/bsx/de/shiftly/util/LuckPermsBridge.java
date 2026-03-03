package bsx.de.shiftly.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bridge to LuckPerms API - optional dependency
 */
public class LuckPermsBridge {

    private LuckPerms luckPerms;
    private ProxyServer proxyServer;

    /**
     * Initialize LuckPerms bridge
     * @param proxyServer velocity proxy server
     */
    public void init(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Exception ignored) {
            luckPerms = null;
        }
    }

    /**
     * Check if player has a specific permission
     * @param playerName player name
     * @param permission permission node
     * @return true if has permission
     */
    public boolean hasPermission(String playerName, String permission) {
        if (luckPerms == null) {
            return false;
        }

        var userManager = luckPerms.getUserManager();
        var user = userManager.getUser(playerName);

        if (user == null) return false;

        return user.getCachedData()
                .getPermissionData()
                .checkPermission(permission)
                .asBoolean();
    }

    /**
     * Get all players in a specific LuckPerms group
     * @param groupName group name
     * @return list of players
     */
    public List<Player> getPlayersInGroup(String groupName) {
        if (luckPerms == null || proxyServer == null) {
            return List.of();
        }

        List<Player> result = new ArrayList<>();

        for (Player player : proxyServer.getAllPlayers()) {
            var userManager = luckPerms.getUserManager();
            var user = userManager.getUser(player.getUniqueId());

            if (user == null) continue;

            var primaryGroup = user.getPrimaryGroup();

            if (primaryGroup.equalsIgnoreCase(groupName)) {
                result.add(player);
            }
        }

        return result;
    }

    /**
     * Check if LuckPerms is available
     * @return true if available
     */
    public boolean isAvailable() {
        return luckPerms != null;
    }

    /**
     * Get all loaded group names from LuckPerms
     * @return list of group names
     */
    public List<String> getGroupNames() {
        if (luckPerms == null) {
            return List.of();
        }

        var groupManager = luckPerms.getGroupManager();
        var groups = groupManager.getLoadedGroups();

        return groups.stream()
                .map(g -> g.getName())
                .sorted()
                .collect(Collectors.toList());
    }
}
