package net.labortale.discordvips.data.link;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface LinkDatabase {
    void setLink(UUID hytaleUuid, long discordId);
    void removeLink(UUID hytaleUuid);
    void removeLink(long discordId);
    UUID getHytale(long discordId);
    long getDiscord(UUID hytaleUuid);
    Set<Long> getAllDiscordIds();
    Map<Long, UUID> getAll();
}
