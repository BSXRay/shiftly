package bsx.de.shiftly.permission;

import com.velocitypowered.api.command.CommandSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionCache {

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public boolean hasPermission(CommandSource source, String permission) {

        String key = source.hashCode() + ":" + permission;

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        boolean result = source.hasPermission(permission);

        cache.put(key, result);

        return result;
    }

    public void clear() {
        cache.clear();
    }
}