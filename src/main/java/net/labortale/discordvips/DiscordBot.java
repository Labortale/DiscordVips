package net.labortale.discordvips;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter {

    private final JDA jda;
    private final LuckPerms luckPerms = LuckPermsProvider.get();
    private ScheduledExecutorService scheduler;
    //concurrency lock
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();

    public DiscordBot(String token) {

        this.jda = JDABuilder.createDefault(token)
                .addEventListeners(this)
                .build();

        try {
            this.jda.awaitReady();
        } catch (InterruptedException e) {
            DiscordVips.getInstance().getLogger().atSevere().log("Exception in Discord bot initialization:", e);
        }

        jda.upsertCommand("link", "Collega il tuo account Hytale")
                .addOption(OptionType.STRING, "code", "Codice di collegamento", true)
                .queue();
    }

    /* =======================
       COMMANDO !link <code>
       ======================= */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("link"))
            return;

        if (!event.isFromGuild()) {
            event.reply("❌ Questo comando può essere usato solo in un server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (Arrays.stream(DiscordVips.getConfig().getGuildIdWhitelist()).noneMatch(l -> l == event.getGuild().getIdLong())) {
            event.reply("❌ Questo comando può essere usato solo nel server ufficiale.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.reply("❌ Impossibile determinare il membro Discord.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // /link <code>
        String code = event.getOption("code") != null
                ? event.getOption("code").getAsString()
                : null;

        if (code == null || code.isBlank()) {
            event.reply("❌ Devi specificare un codice valido.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        UUID hytaleUuid = DiscordVips.getPendingLinkDb().consumeCode(code);

        if (hytaleUuid == null) {
            event.reply("❌ Codice non valido o scaduto.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long discordId = event.getUser().getIdLong();

        if (DiscordVips.getLinkDb().getHytale(discordId) != null) {
            event.reply("❌ Questo account Discord è già collegato.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        DiscordVips.getLinkDb().setLink(hytaleUuid, discordId);

        event.reply("✅ Account Discord collegato con successo.")
                .setEphemeral(true)
                .queue();

        // sync immediata dei ruoli
        syncMember(member, hytaleUuid);
    }

    /* =======================
       EVENTI RUOLO
       ======================= */

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if(Arrays.stream(DiscordVips.getConfig().getGuildIdWhitelist()).noneMatch(l -> l == event.getGuild().getIdLong()))
            return;
        handleRoleChange(event.getMember());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if(Arrays.stream(DiscordVips.getConfig().getGuildIdWhitelist()).noneMatch(l -> l == event.getGuild().getIdLong()))
            return;
        handleRoleChange(event.getMember());
    }

    private void handleRoleChange(Member member) {
        long discordId = member.getIdLong();
        UUID hytaleUuid = DiscordVips.getLinkDb().getHytale(discordId);
        if (hytaleUuid == null) return;

        syncMember(member, hytaleUuid);
    }

    /* =======================
       SYNC LOGICA
       ======================= */

    private void syncMember(Member member, UUID hytaleUuid) {
        syncMember(member, hytaleUuid, DiscordVips.getConfig().getRoleMap());
    }

    private void syncMember(Member member, UUID hytaleUuid, Map<String,String> rolesToSync) {
        // calcola i ruoli Discord target
        Set<String> roleGroups = member.getRoles().stream()
                .map(r -> r.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        // lock per evitare race condition
        Object lock = locks.computeIfAbsent(hytaleUuid, k -> new Object());

        luckPerms.getUserManager().loadUser(hytaleUuid).thenAccept(lpUser -> {
            if (lpUser == null) return;

            synchronized (lock) {
                //compute diff
                Set<String> currentGroups = lpUser.data().toCollection().stream()
                        .filter(node -> node.getKey().startsWith("group."))
                        .map(node -> node.getKey().substring(6)) // rimuovi "group."
                        .collect(Collectors.toSet());

                Set<String> targetGroups = roleGroups.stream()
                        .map(rolesToSync::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                Set<String> groupsToAdd = new HashSet<>(targetGroups);
                groupsToAdd.removeAll(currentGroups);

                Set<String> groupsToRemove = new HashSet<>(currentGroups);
                groupsToRemove.removeAll(targetGroups);

                // aggiungi solo i gruppi mancanti
                for (String group : groupsToAdd) {
                    lpUser.data().add(Node.builder("group." + group).build());
                }

                // rimuovi solo i gruppi che non devono più esserci
                for (String group : groupsToRemove) {
                    lpUser.data().clear(node -> node.getKey().equals("group." + group));
                }

                // salva utente
                luckPerms.getUserManager().saveUser(lpUser);
            }

            // cleanup lock se non più necessario
            locks.remove(hytaleUuid, lock);
        });
    }

    /* =======================
       SYNC PERIODICA COMPLETA
       ======================= */

    /**
     * Starts
     * @param intervalSeconds - seconds between syncs
     * @param initialDelay - seconds to wait to the first execution
     */
    public void startFullSyncTask(long intervalSeconds, long initialDelay) {
        if(scheduler != null) throw new IllegalStateException("Cannot start a fullsync start since one is already running!");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::fullSync, initialDelay, intervalSeconds, TimeUnit.SECONDS);
    }

    private void fullSync() {
        Map<String, String> rolesToSync = DiscordVips.getConfig().getRoleMap();
        Map<Long, UUID> linked = DiscordVips.getLinkDb().getAll();

        for (Guild guild : jda.getGuilds()) {
            if(Arrays.stream(DiscordVips.getConfig().getGuildIdWhitelist()).noneMatch(l -> l == guild.getIdLong()))
                continue;
            for (Map.Entry<Long, UUID> entry : linked.entrySet()) {
                Member member = guild.getMemberById(entry.getKey());
                if (member == null) continue;
                syncMember(member, entry.getValue(), rolesToSync);
            }
        }
    }

    public void shutdown() {
        jda.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            DiscordVips.getInstance().getLogger().atWarning().log("Cannot cleanly shutdown fullsync scheduler! ", e);
        }
    }
}

