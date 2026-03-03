package bsx.de.shiftly.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message configuration manager with caching for performance
 */
public final class MessageConfig {

    private final Map<String, Object> messages = new ConcurrentHashMap<>();
    private final Map<String, Component> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> settings = new ConcurrentHashMap<>();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Load messages from config file
     */
    @SuppressWarnings("unchecked")
    public void load() {
        try {
            Path file = Path.of("plugins/Shiftly/config/messages.yml");

            if (!Files.exists(file)) return;

            Yaml yaml = new Yaml();

            try (FileInputStream in = new FileInputStream(file.toFile())) {
                Map<String, Object> data = yaml.load(in);

                if (data == null) return;

                cache.clear();
                messages.clear();
                settings.clear();

                if (data.containsKey("messages")) {
                    messages.putAll((Map<String, Object>) data.get("messages"));
                }

                if (data.containsKey("settings")) {
                    settings.putAll((Map<String, Object>) data.get("settings"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reload configuration
     */
    public void reload() {
        load();
    }

    /**
     * Get formatted message with MiniMessage
     * @param key message key
     * @return formatted component
     */
    public Component get(String key) {
        if (messages.isEmpty()) {
            return Component.text(key);
        }

        return cache.computeIfAbsent(key, k -> {
            String path = k.replace("messages.", "");
            Object value = messages.get(path);

            if (value == null) {
                return Component.text(k);
            }

            return miniMessage.deserialize(value.toString());
        });
    }

    /**
     * Get message scope setting
     * @return scope (GLOBAL, TARGET_ONLY, TARGET_AND_ADMINS)
     */
    public String getMessageScope() {
        Object scope = settings.get("message_scope");
        if (scope == null) return "GLOBAL";
        return scope.toString();
    }
}
