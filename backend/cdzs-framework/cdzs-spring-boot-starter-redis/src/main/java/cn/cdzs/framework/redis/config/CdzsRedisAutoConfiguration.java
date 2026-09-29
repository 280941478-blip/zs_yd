package cn.cdzs.framework.redis.config;

import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 配置类
 */
@AutoConfiguration(before = RedissonAutoConfigurationV2.class) // 目的：使用自己定义的 RedisTemplate Bean
public class CdzsRedisAutoConfiguration {

    /**
     * 创建 RedisTemplate Bean，使用 JSON 序列化方式
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置 RedisConnection 工厂。😈 它就是实现多种 Java Redis 客户端接入的秘密工厂。感兴趣的胖友，可以自己去撸下。
        template.setConnectionFactory(factory);
        // 使用 String 序列化方式，序列化 KEY 。
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        // 使用 JSON 序列化方式，序列化 VALUE
        RedisSerializer<?> redisSerializer = buildRedisSerializer();
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);
        return template;
    }

    public static RedisSerializer<?> buildRedisSerializer() {
        RedisSerializer<Object> json = RedisSerializer.json();
        // 解决 LocalDateTime 的序列化
        ObjectMapper objectMapper = (ObjectMapper) ReflectUtil.getFieldValue(json, "mapper");
        objectMapper.registerModules(new JavaTimeModule());
        // Existing cache entries can contain the previous Java package in their type metadata.
        // Resolve only type IDs; never rewrite user content stored in ordinary string fields.
        objectMapper.addHandler(new com.fasterxml.jackson.databind.deser.DeserializationProblemHandler() {
            @Override
            public com.fasterxml.jackson.databind.JavaType handleUnknownTypeId(
                    com.fasterxml.jackson.databind.DeserializationContext context,
                    com.fasterxml.jackson.databind.JavaType baseType, String typeId,
                    com.fasterxml.jackson.databind.jsontype.TypeIdResolver resolver, String message)
                    throws java.io.IOException {
                String legacyPrefix = "cn.iocoder.yudao.";
                if (!typeId.startsWith(legacyPrefix)) {
                    return null;
                }
                String migrated = "cn.cdzs." + typeId.substring(legacyPrefix.length()).replace("Yudao", "Cdzs");
                return context.resolveSubType(baseType, migrated);
            }
        });
        return new cn.cdzs.framework.redis.core.NamespaceCompatibleRedisSerializer(json);
    }

}
