package cn.cdzs.framework.redis.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Reads existing cache type metadata after the package rename; writes only new class names. */
public final class NamespaceCompatibleRedisSerializer implements RedisSerializer<Object> {
    private static final String LEGACY_PREFIX = "cn.iocoder.yudao.";
    private static final ObjectMapper TREE_MAPPER = new ObjectMapper();
    private final RedisSerializer<Object> delegate;

    public NamespaceCompatibleRedisSerializer(RedisSerializer<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        return delegate.serialize(value);
    }

    @Override
    public Object deserialize(byte[] source) throws SerializationException {
        if (source == null || source.length == 0
                || !new String(source, StandardCharsets.UTF_8).contains(LEGACY_PREFIX)) {
            return delegate.deserialize(source);
        }
        try {
            JsonNode tree = TREE_MAPPER.readTree(source);
            if (migrateTypeMetadata(tree)) {
                return delegate.deserialize(TREE_MAPPER.writeValueAsBytes(tree));
            }
            return delegate.deserialize(source);
        } catch (IOException e) {
            throw new SerializationException("Unable to read legacy cache type metadata", e);
        }
    }

    private boolean migrateTypeMetadata(JsonNode node) {
        if (node == null) {
            return false;
        }
        boolean changed = false;
        if (node instanceof ObjectNode object) {
            JsonNode type = object.get("@class");
            if (type != null && type.isTextual() && type.asText().startsWith(LEGACY_PREFIX)) {
                object.put("@class", "cn.cdzs." + type.asText().substring(LEGACY_PREFIX.length())
                        .replace("Yudao", "Cdzs"));
                changed = true;
            }
        }
        for (JsonNode child : node) {
            changed |= migrateTypeMetadata(child);
        }
        return changed;
    }
}
