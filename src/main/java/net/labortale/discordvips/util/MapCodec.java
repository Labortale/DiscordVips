package net.labortale.discordvips.util;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MapCodec<K, V> implements Codec<Map<K, V>> {

    private final Codec<K> keyCodec;
    private final Codec<V> valueCodec;

    public MapCodec(Codec<K> keyCodec, Codec<V> valueCodec) {
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    @Nullable
    @Override
    public Map<K, V> decode(BsonValue bsonValue, ExtraInfo extraInfo) {
        if (!bsonValue.isDocument()) {
            return Map.of();
        }

        BsonDocument doc = bsonValue.asDocument();
        Map<K, V> result = new HashMap<>();

        for (Map.Entry<String, BsonValue> entry : doc.entrySet()) {
            K key = keyCodec.decode(new BsonString(entry.getKey()), extraInfo);
            if (key == null) continue;

            V value = valueCodec.decode(entry.getValue(), extraInfo);
            result.put(key, value);
        }

        return result;
    }

    @Override
    public BsonValue encode(Map<K, V> map, ExtraInfo extraInfo) {
        BsonDocument doc = new BsonDocument();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            BsonValue keyValue = keyCodec.encode(entry.getKey(), extraInfo);
            if (!(keyValue instanceof BsonString keyString)) {
                throw new IllegalStateException("Map keys must encode to BsonString");
            }

            doc.put(keyString.getValue(), valueCodec.encode(entry.getValue(), extraInfo));
        }

        return doc;
    }

    @NotNull
    @Override
    public Schema toSchema(@NotNull SchemaContext schemaContext) {
        ObjectSchema schema = new ObjectSchema();

        // valori della mappa
        Schema valueSchema = schemaContext.refDefinition(valueCodec);
        schema.setAdditionalProperties(valueSchema);

        return schema;
    }
}
