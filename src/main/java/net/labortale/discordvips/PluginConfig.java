package net.labortale.discordvips;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import net.labortale.discordvips.util.MapCodec;

import java.util.HashMap;
import java.util.Map;

public class PluginConfig {

    public static final BuilderCodec<PluginConfig> CODEC =
            BuilderCodec.builder(PluginConfig.class, PluginConfig::new)
                    .append(new KeyedCodec<>("DiscordToken", Codec.STRING), PluginConfig::setDiscordToken, PluginConfig::getDiscordToken).add()
                    .append(new KeyedCodec<>("DiscordGuildIdWhitelist", Codec.LONG_ARRAY), PluginConfig::setGuildIdWhitelist, PluginConfig::getGuildIdWhitelist).add()
                    .append(new KeyedCodec<>("RoleMap", new MapCodec<>(Codec.STRING, Codec.STRING)), PluginConfig::setRoleMap, PluginConfig::getRoleMap).add()
                    .build();

    public PluginConfig() {}

    private String discordToken = "";
    private Map<String, String> roleMap = new HashMap<>();
    private long[] guildIdWhitelist = {};

    public String getDiscordToken() {
        return discordToken;
    }

    private void setDiscordToken(String discordToken) {
        this.discordToken = discordToken;
    }

    public Map<String, String> getRoleMap() {
        return roleMap;
    }

    private void setRoleMap(Map<String,String> roleMap) {
        this.roleMap = roleMap;
    }

    public long[] getGuildIdWhitelist() {
        return guildIdWhitelist;
    }

    public void setGuildIdWhitelist(long[] guildIdWhitelist) {
        this.guildIdWhitelist = guildIdWhitelist;
    }
}
