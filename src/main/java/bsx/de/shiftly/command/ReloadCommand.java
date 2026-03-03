package bsx.de.shiftly.command;

import bsx.de.shiftly.Shiftly;
import bsx.de.shiftly.config.MessageConfig;
import bsx.de.shiftly.util.PermissionHelper;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * /shiftly reload command - reload configuration
 */
public class ReloadCommand implements SimpleCommand {

    private final MessageConfig messageConfig;
    private final PermissionHelper permissionHelper;
    private final Shiftly shiftly;

    public ReloadCommand(MessageConfig messageConfig, PermissionHelper permissionHelper, Shiftly shiftly) {
        this.messageConfig = messageConfig;
        this.permissionHelper = permissionHelper;
        this.shiftly = shiftly;
    }

    @Override
    public void execute(Invocation invocation) {
        messageConfig.reload();
        shiftly.reloadConfig();
        invocation.source().sendMessage(Component.text("Shiftly configs reloaded. Note: Command aliases require proxy restart."));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return permissionHelper.checkReload(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            return List.of("reload");
        }

        if (invocation.arguments().length == 1) {
            String input = invocation.arguments()[0].toLowerCase();
            if ("reload".startsWith(input)) {
                return List.of("reload");
            }
        }

        return List.of();
    }
}
