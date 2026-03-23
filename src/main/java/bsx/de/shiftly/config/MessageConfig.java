package bsx.de.shiftly.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageConfig {

    private final Map<String, Object> messages = new ConcurrentHashMap<>();
    private final Map<String, Component> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> settings = new ConcurrentHashMap<>();
    private final Path dataDirectory;

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    private static final Map<String, String> DEFAULTS1 = Map.of(
            "no_permission", "&c&lYou don't have permission.",
            "usage_move", "&d&oUsage: &r/move &5<player|@a> <server>",
            "usage_move_group", "&d&oUsage: &r/move -group &5<group> <server>",
            "player_not_found", "&cPlayer not found.",
            "server_not_found", "&cServer not found."
    );

    private static final Map<String, String> DEFAULTS2 = Map.of(
            "moved_you", "&b&lYou got moved&r &d-> &a<server>",
            "moved_admin", "&d&lMoved&r &b<player> &d-> &a<server>",
            "moved_global", "&d-> &b<player> &dgot moved&r &d-> &a<server>",
            "moved_you_group", "&b&lYou got moved&r &d-> &a<server> &8(group: &b<group>&8)",
            "moved_admin_group", "&d&lMoved group&r &b<group> &d-> &a<server>",
            "moved_global_group", "&d-> &b&lGroup&r &b<group> &dgot moved&r &d-> &a<server>"
    );

    public MessageConfig(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public MessageConfig() {
        this.dataDirectory = Path.of("plugins", "Shiftly");
    }

    public void load() {
        try {
            Path file = dataDirectory.resolve("messages.yml");

            cache.clear();
            messages.clear();
            settings.clear();

            for (Map.Entry<String, String> entry : DEFAULTS1.entrySet()) {
                messages.put(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : DEFAULTS2.entrySet()) {
                messages.put(entry.getKey(), entry.getValue());
            }

            if (Files.exists(file)) {
                Yaml yaml = new Yaml();

                try (FileInputStream in = new FileInputStream(file.toFile())) {
                    Map<String, Object> data = yaml.load(in);

                    if (data != null) {
                        if (data.containsKey("messages")) {
                            Map<String, Object> loadedMessages = (Map<String, Object>) data.get("messages");
                            for (Map.Entry<String, Object> entry : loadedMessages.entrySet()) {
                                messages.put(entry.getKey(), entry.getValue());
                            }
                        }

                        if (data.containsKey("settings")) {
                            settings.putAll((Map<String, Object>) data.get("settings"));
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        load();
    }

    public Component get(String key) {
        return cache.computeIfAbsent(key, k -> {
            String path = k.replace("messages.", "");
            Object value = messages.get(path);

            if (value == null) {
                return Component.text(k);
            }

            return legacySerializer.deserialize(value.toString());
        });
    }

    public String getMessageScope() {
        Object scope = settings.get("message_scope");
        if (scope == null) return "TARGET_AND_ADMINS";
        return scope.toString();
    }
}
