package bsx.de.shiftly.util;

import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class LuckPermsBridge {

    private LuckPerms luckPerms;

    public void init() {
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
}