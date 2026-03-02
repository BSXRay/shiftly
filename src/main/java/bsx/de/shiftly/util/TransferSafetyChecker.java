package bsx.de.shiftly.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.concurrent.CompletableFuture;

public class TransferSafetyChecker {

    private TransferSafetyChecker() {}

    /**
     * Checks asynchronously if player transfer is safe.
     */
    public static CompletableFuture<Boolean> checkTransfer(Player player, RegisteredServer server) {

        return CompletableFuture.supplyAsync(() -> {

            try {

                // Server online check
                if (!server.getPlayersConnected().isEmpty() || server.getServerInfo() != null) {
                    return true;
                }

            } catch (Exception ignored) {
                return false;
            }

            return true;
        });
    }
}