package net.labortale.discordvips.data.pendinglink;

import java.util.UUID;

public interface PendingLinkDatabase {
    void createCode(String code, UUID hytaleUuid, long expiresAt);
    UUID consumeCode(String code); // ritorna UUID e invalida
    void purgeExpired(long now);
}
