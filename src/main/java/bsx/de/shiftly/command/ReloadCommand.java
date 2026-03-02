package bsx.de.shiftly.command;

import bsx.de.shiftly.config.MessageConfig;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ReloadCommand implements SimpleCommand {

    private final MessageConfig messageConfig;

    public ReloadCommand(MessageConfig messageConfig) {
        this.messageConfig = messageConfig;
    }

    @Override
    public void execute(Invocation invocation) {

        messageConfig.reload();

        invocation.source().sendMessage(
                Component.text("Shiftly configs reloaded.")
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("shiftly.admin.reload");
    }

    @Override
    public List<String> suggest(Invocation invocation) {

        // TAB Completion for /shiftly reload

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