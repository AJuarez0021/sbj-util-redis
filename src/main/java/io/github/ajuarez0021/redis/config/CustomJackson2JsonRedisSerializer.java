package io.github.ajuarez0021.redis.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;


/**
 * The Class CustomJackson2JsonRedisSerializer.
 *
 * @author ajuar
 * @param <T> The object
 */
public class CustomJackson2JsonRedisSerializer<T> implements RedisSerializer<T> {

    /** The object mapper. */
    private final ObjectMapper objectMapper;

    /**
     * The class type.
     */
    private final Class<T> type;

    /**
     * Instantiates a new custom jackson 2 json redis serializer.
     *
     * @param objectMapper the object mapper
     * @param type the type
     */
    public CustomJackson2JsonRedisSerializer(ObjectMapper objectMapper, Class<T> type) {
        this.objectMapper = objectMapper;
        this.type = type;
    }

    /**
     * Serialize.
     *
     * @param value the value
     * @return the byte[]
     * @throws SerializationException the serialization exception
     */
    @Override
    public byte[] serialize(T value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing", e);
        }
    }

    /**
     * Deserialize.
     *
     * @param bytes the bytes
     * @return the object
     * @throws SerializationException the serialization exception
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.convertValue(bytes, type);
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Error deserializing", e);
        }
    }


}
