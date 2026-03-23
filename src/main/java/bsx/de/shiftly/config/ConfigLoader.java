package bsx.de.shiftly.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {

    public static void saveResourceIfNotExists(Path dataFolder, String resourcePath) {
        try {
            Path target = dataFolder.resolve(resourcePath);

            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());

                try (InputStream in = ConfigLoader.class
                        .getClassLoader()
                        .getResourceAsStream(resourcePath)) {

                    if (in != null) {
                        Files.copy(in, target);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}