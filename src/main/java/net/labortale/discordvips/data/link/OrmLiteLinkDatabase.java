package net.labortale.discordvips.data.link;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import net.labortale.discordvips.DiscordVips;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrmLiteLinkDatabase implements LinkDatabase {

    private final Dao<DiscordLinkEntity, UUID> dao;

    public OrmLiteLinkDatabase(ConnectionSource source) throws SQLException {
        TableUtils.createTableIfNotExists(source, DiscordLinkEntity.class);
        this.dao = DaoManager.createDao(source, DiscordLinkEntity.class);
    }

    @Override
    public void setLink(UUID hytaleUuid, long discordId) {
        try {
            removeLink(hytaleUuid);
            removeLink(discordId);
            dao.create(new DiscordLinkEntity(hytaleUuid, discordId));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeLink(UUID hytaleUuid) {
        try {
            dao.deleteById(hytaleUuid);
        } catch (SQLException e) {
            DiscordVips.getInstance().getLogger().atWarning().log("DB error while removing link", e);
        }
    }

    @Override
    public void removeLink(long discordId) {
        try {
            DeleteBuilder<DiscordLinkEntity, UUID> db = dao.deleteBuilder();
            db.where().eq("discordId", discordId);
            dao.delete(db.prepare());
        } catch (SQLException e) {
            DiscordVips.getInstance().getLogger().atWarning().log("DB error while removing link", e);
        }
    }

    @Override
    public UUID getHytale(long discordId) {
        try {
            QueryBuilder<DiscordLinkEntity, UUID> qb = dao.queryBuilder();
            qb.where().eq("discordId", discordId);
            DiscordLinkEntity e = dao.queryForFirst(qb.prepare());
            return e != null ? e.hytaleUuid : null;
        } catch (SQLException e) {
            DiscordVips.getInstance().getLogger().atWarning().log("DB error while gettind Hytale uuid from Discord id", e);
            return null;
        }
    }

    @Override
    public long getDiscord(UUID hytaleUuid) {
        try {
            DiscordLinkEntity e = dao.queryForId(hytaleUuid);
            return e != null ? e.discordId : -1;
        } catch (SQLException e) {
            DiscordVips.getInstance().getLogger().atWarning().log("DB error while gettind Discord id from Hytale uuid", e);
            return -1;
        }
    }

    @Override
    public Set<Long> getAllDiscordIds() {
        try {
            return dao.queryForAll().stream()
                    .map(e -> e.discordId)
                    .collect(Collectors.toSet());
        } catch (SQLException e) {
            DiscordVips.getInstance().getLogger().atWarning().log("DB error while gettind all Discord ids", e);
            return Set.of();
        }
    }

    @Override
    public Map<Long, UUID> getAll() {
        try {
            return dao.queryForAll().stream().collect(Collectors.toMap(e -> e.discordId, e -> e.hytaleUuid));
        } catch (SQLException e) {
            DiscordVips.getInstance().getLogger().atWarning().log("DB error while gettind all Discord ids", e);
            return Map.of();
        }
    }
}
