package bsx.de.shiftly;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import bsx.de.shiftly.command.MoveCommand;
import bsx.de.shiftly.command.ReloadCommand;
import bsx.de.shiftly.config.ConfigLoader;
import bsx.de.shiftly.config.MessageConfig;
import bsx.de.shiftly.service.MoveService;
import bsx.de.shiftly.util.LuckPermsBridge;

import java.nio.file.Path;

@Plugin(
        id = "shiftly",
        name = "Shiftly",
        version = "1.0.0",
        authors = {"bsx"}
)
public class Shiftly {

    private final ProxyServer proxyServer;
    private final Path dataDirectory;

    private MessageConfig messageConfig;
    private MoveService moveService;
    private LuckPermsBridge bridge;

    @Inject
    public Shiftly(ProxyServer proxyServer,
                   @DataDirectory Path dataDirectory) {

        this.proxyServer = proxyServer;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        bootstrapConfigs();
        initializeSystems();
        registerCommands();
    }

    private void bootstrapConfigs() {

        ConfigLoader.saveResourceIfNotExists(
                dataDirectory,
                "config/messages.yml"
        );

        ConfigLoader.saveResourceIfNotExists(
                dataDirectory,
                "config/permissions.yml"
        );
    }

    private void initializeSystems() {

        messageConfig = new MessageConfig();
        messageConfig.load();

        bridge = new LuckPermsBridge();
        bridge.init();

        moveService = new MoveService(proxyServer);
    }

    private void registerCommands() {

        var commandManager = proxyServer.getCommandManager();

        commandManager.register(
                commandManager.metaBuilder("move").build(),
                new MoveCommand(
                        proxyServer,
                        moveService,
                        bridge,
                        messageConfig
                )
        );

        commandManager.register(
                commandManager.metaBuilder("shiftly").build(),
                new ReloadCommand(messageConfig)
        );
    }
}