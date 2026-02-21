package net.labortale.discordvips.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import net.labortale.discordvips.DiscordVips;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class SyncVipsCommand extends AbstractAsyncCommand {

    public SyncVipsCommand() {
        super("syncvips", "Forza sincronizzazione dei vip da Discord a Hytale");
        requirePermission("labor.discordvips.command.sync");
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext ctx) {

        if(!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Solo i giocatori possono usare questo comando."));
            return CompletableFuture.completedFuture(null);
        }

        DiscordVips.getDiscordBot().fullSync();

        return CompletableFuture.completedFuture(null);
    }
}
