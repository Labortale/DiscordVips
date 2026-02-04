package net.labortale.discordvips.data.pendinglink;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPendingLinkDatabase implements PendingLinkDatabase {

    private static class Pending {
        UUID uuid;
        long expiresAt;
    }

    private final Map<String, Pending> codes = new ConcurrentHashMap<>();

    @Override
    public void createCode(String code, UUID hytaleUuid, long expiresAt) {
        Pending p = new Pending();
        p.uuid = hytaleUuid;
        p.expiresAt = expiresAt;
        codes.put(code, p);
    }

    @Override
    public UUID consumeCode(String code) {
        Pending p = codes.remove(code);
        if (p == null) return null;
        if (p.expiresAt < System.currentTimeMillis()) return null;
        return p.uuid;
    }

    @Override
    public void purgeExpired(long now) {
        codes.entrySet().removeIf(e -> e.getValue().expiresAt < now);
    }
}
