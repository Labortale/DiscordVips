package net.labortale.discordvips;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LinkDiscordCommand extends AbstractAsyncCommand {

    public LinkDiscordCommand() {
        super("linkdiscord", "linka il tuo account Discord");
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext ctx) {

        if(!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Solo i giocatori possono usare questo comando."));
            return CompletableFuture.completedFuture(null);
        }

        String code = generateCode();
        long expiresAt = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes

        DiscordVips.getPendingLinkDb().createCode(code, ctx.sender().getUuid(), expiresAt);

        ctx.sendMessage(Message.raw("")
                .insert(Message.raw("Scrivi sul nostro server Discord:").color(Color.GREEN))
                .insert(Message.raw("/link " + code).color(Color.YELLOW))
                .insert(Message.raw("Il codice scade tra 5 minuti.").color(Color.GREEN))
        );

        return CompletableFuture.completedFuture(null);
    }

    private String generateCode() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();
    }
}