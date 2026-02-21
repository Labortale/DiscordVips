package net.labortale.discordvips.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import net.labortale.discordvips.DiscordVips;
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

        Message msg = Message.empty();

        // Check if player is already synced
        boolean synced = DiscordVips.getLinkDb().getDiscord(ctx.sender().getUuid()) != null;

        if(synced) {
            msg = msg.insert(Message.raw("Attenzione: hai già linkato un account Discord! Puoi ripetere il link.\n").color(Color.YELLOW));
        }

        String code = generateCode();
        long expiresAt = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        DiscordVips.getPendingLinkDb().createCode(code, ctx.sender().getUuid(), expiresAt);

        msg = msg
                .insert(Message.raw("Scrivi sul nostro server Discord:\n").color(Color.GREEN))
                .insert(Message.raw(" /link " + code).color(Color.YELLOW))
                .insert(Message.raw("\nIl codice scade tra 5 minuti.\n").color(Color.GREEN));

        ctx.sendMessage(msg);

        return CompletableFuture.completedFuture(null);
    }

    private String generateCode() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();
    }
}