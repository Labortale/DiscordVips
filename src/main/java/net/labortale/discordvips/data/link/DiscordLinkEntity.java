package net.labortale.discordvips.data.link;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "discord_links")
public class DiscordLinkEntity {

    @DatabaseField(id = true)
    public UUID hytaleUuid;

    @DatabaseField(unique = true, index = true, canBeNull = false)
    public long discordId;

    @DatabaseField
    public long linkedAt;

    public DiscordLinkEntity() {}

    public DiscordLinkEntity(UUID hytaleUuid, long discordId) {
        this.hytaleUuid = hytaleUuid;
        this.discordId = discordId;
        this.linkedAt = System.currentTimeMillis();
    }
}
