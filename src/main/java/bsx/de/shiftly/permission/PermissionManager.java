package bsx.de.shiftly.permission;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class PermissionManager {

    private Map<String, Object> configData;

    public void load() {
        Yaml yaml = new Yaml();

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config/permissions.yml")) {

            if (input != null) {
                configData = yaml.load(input);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isOpDefault(String permissionKey) {

        if (configData == null) return false;

        Object permissionsObj = configData.get("permissions");
        if (!(permissionsObj instanceof Map)) return false;

        Map<?, ?> permissions = (Map<?, ?>) permissionsObj;

        Object permEntryObj = permissions.get(permissionKey);
        if (!(permEntryObj instanceof Map)) return false;

        Map<?, ?> permEntry = (Map<?, ?>) permEntryObj;

        Object opDefault = permEntry.get("default_op");

        return opDefault instanceof Boolean && (Boolean) opDefault;
    }
}