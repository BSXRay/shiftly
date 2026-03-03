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
import bsx.de.shiftly.command.ReloadCommand;
import bsx.de.shiftly.config.CommandConfig;
import bsx.de.shiftly.config.ConfigLoader;
import bsx.de.shiftly.config.MessageConfig;
import bsx.de.shiftly.permission.PermissionManager;
import bsx.de.shiftly.service.MoveService;
import bsx.de.shiftly.util.LuckPermsBridge;
import bsx.de.shiftly.util.PermissionHelper;

import java.nio.file.Path;

/**
 * Shiftly - Velocity plugin for player management
 */
@Plugin(
        id = "shiftly",
        name = "Shiftly",
        version = "1.0.0",
        authors = {"bsxray"}
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
        // Initialize bStats
        int pluginId = 29883;
        Metrics metrics = metricsFactory.make(this, pluginId);
        
        // Startup message with ANSI colors
        String ANSI_PINK = "\u001b[35m";
        String ANSI_BLUE = "\u001b[34m";
        String ANSI_GREEN = "\u001b[32m";
        String ANSI_RESET = "\u001b[0m";
        logger.info(ANSI_PINK + "Shiftly" + ANSI_RESET + " by " + ANSI_BLUE + "BSXRay" + ANSI_RESET + " " + ANSI_GREEN + "enabled!" + ANSI_RESET);
        
        bootstrapConfigs();
        initializeSystems();
        registerCommands();
    }

    /**
     * Copy default config files if they don't exist
     */
    private void bootstrapConfigs() {
        ConfigLoader.saveResourceIfNotExists(dataDirectory, "config/messages.yml");
        ConfigLoader.saveResourceIfNotExists(dataDirectory, "config/permissions.yml");
        ConfigLoader.saveResourceIfNotExists(dataDirectory, "config/commands.yml");
    }

    /**
     * Initialize all services
     */
    private void initializeSystems() {
        messageConfig = new MessageConfig();
        messageConfig.load();

        commandConfig = new CommandConfig();
        commandConfig.load();

        luckPermsBridge = new LuckPermsBridge();
        luckPermsBridge.init(proxyServer);

        PermissionManager permissionManager = new PermissionManager();
        permissionManager.load();

        permissionHelper = new PermissionHelper(permissionManager, luckPermsBridge);

        moveService = new MoveService(proxyServer);
    }

    /**
     * Register all commands
     */
    private void registerCommands() {
        var commandManager = proxyServer.getCommandManager();

        // Register move command
        String moveAlias = commandConfig.getAlias("move");
        commandManager.register(
                commandManager.metaBuilder(moveAlias).build(),
                new MoveCommand(proxyServer, moveService, luckPermsBridge, messageConfig, permissionHelper)
        );

        // Register reload command
        String reloadAlias = commandConfig.getAlias("reload");
        commandManager.register(
                commandManager.metaBuilder(reloadAlias).build(),
                new ReloadCommand(messageConfig, permissionHelper, this)
        );
    }

    /**
     * Reload configuration (messages only) - aliases require restart
     */
    public void reloadConfig() {
        messageConfig.reload();
    }
}
