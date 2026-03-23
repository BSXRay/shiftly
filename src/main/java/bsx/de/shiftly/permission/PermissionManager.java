package bsx.de.shiftly.permission;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PermissionManager {

    private final Map<String, Boolean> defaults = new HashMap<>();

    public void load() {
        Yaml yaml = new Yaml();

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("permissions.yml")) {

            if (input == null) return;

            Map<String, Object> data = yaml.load(input);

            if (data == null || !data.containsKey("permissions")) return;

            Object permissionsObj = data.get("permissions");
            if (!(permissionsObj instanceof Map)) return;

            Map<?, ?> permissions = (Map<?, ?>) permissionsObj;

            for (Map.Entry<?, ?> entry : permissions.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    Map<?, ?> permData = (Map<?, ?>) value;
                    Object defaultObj = permData.get("default");
                    boolean defaultValue = defaultObj instanceof Boolean && (Boolean) defaultObj;
                    defaults.put(key, defaultValue);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isDefaultEnabled(String permissionKey) {
        return defaults.getOrDefault(permissionKey, false);
    }
}
