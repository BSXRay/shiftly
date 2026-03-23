package bsx.de.shiftly.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LuckPermsBridge {

    private LuckPerms luckPerms;
    private ProxyServer proxyServer;

    public void init(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Exception ignored) {
            luckPerms = null;
        }
    }

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

    public boolean isAvailable() {
        return luckPerms != null;
    }

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
