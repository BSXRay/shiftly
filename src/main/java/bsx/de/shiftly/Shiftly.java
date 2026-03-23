package bsx.de.shiftly;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import bsx.de.shiftly.command.MoveCommand;
import bsx.de.shiftly.command.ShiftlyCommand;
import bsx.de.shiftly.config.CommandConfig;
import bsx.de.shiftly.config.ConfigLoader;
import bsx.de.shiftly.config.MessageConfig;
import bsx.de.shiftly.permission.PermissionManager;
import bsx.de.shiftly.service.MoveService;
import bsx.de.shiftly.util.LuckPermsBridge;
import bsx.de.shiftly.util.PermissionHelper;

import java.nio.file.Path;

@Plugin(
        id = "shiftly",
        name = "Shiftly",
        version = "1.0.0",
        authors = {"BSXRay"}
)
public class Shiftly {

    private final ProxyServer proxyServer;
    private final Path dataDirectory;
    private final Logger logger;
    private final Metrics.Factory metricsFactory;

    private MessageConfig messageConfig;
    private CommandConfig commandConfig;
    private MoveService moveService;
    private LuckPermsBridge luckPermsBridge;
    private PermissionHelper permissionHelper;

    @Inject
    public Shiftly(ProxyServer proxyServer,
                   @DataDirectory Path dataDirectory,
                   Logger logger,
                   Metrics.Factory metricsFactory) {
        this.proxyServer = proxyServer;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        metricsFactory.make(this, 29883);
        logger.info("\u001b[35mShiftly\u001b[0m by \u001b[34mBSXRay\u001b[0m \u001b[32menabled!\u001b[0m");

        ConfigLoader.saveResourceIfNotExists(dataDirectory, "messages.yml");
        ConfigLoader.saveResourceIfNotExists(dataDirectory, "permissions.yml");
        ConfigLoader.saveResourceIfNotExists(dataDirectory, "commands.yml");

        messageConfig = new MessageConfig(dataDirectory);
        messageConfig.load();

        commandConfig = new CommandConfig(dataDirectory);
        commandConfig.load();

        luckPermsBridge = new LuckPermsBridge();
        luckPermsBridge.init(proxyServer);

        PermissionManager permissionManager = new PermissionManager();
        permissionManager.load();

        permissionHelper = new PermissionHelper(permissionManager, luckPermsBridge);
        moveService = new MoveService(proxyServer);

        var commandManager = proxyServer.getCommandManager();

        commandManager.register(
                commandManager.metaBuilder("shiftly").build(),
                new ShiftlyCommand(messageConfig, permissionHelper)
        );

        String moveAlias = commandConfig.getAlias("move");
        commandManager.register(
                commandManager.metaBuilder(moveAlias).build(),
                new MoveCommand(proxyServer, moveService, luckPermsBridge, messageConfig, permissionHelper)
        );
    }

    public void reloadConfig() {
        messageConfig.reload();
    }
}
