package io.github.ajuarez0021.redis.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import io.github.ajuarez0021.redis.dto.TypedValueDto;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;


/**
 * The Class CustomJackson2JsonRedisSerializer.
 *
 * @author ajuar
 */
public class CustomJackson2JsonRedisSerializer implements RedisSerializer<Object> {

    /** The object mapper. */
    private final ObjectMapper objectMapper;

    /**
     * Instantiates a new custom jackson 2 json redis serializer.
     *
     * @param objectMapper the object mapper
     */
    public CustomJackson2JsonRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serialize.
     *
     * @param value the value
     * @return the byte[]
     * @throws SerializationException the serialization exception
     */
    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            TypedValueDto typedValue = new TypedValueDto(
                    value.getClass().getName(),
                    value
            );
            return objectMapper.writeValueAsBytes(typedValue);
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
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes.length == 0) {
            return null;
        }
        try {
            TypedValueDto typedValue = objectMapper.readValue(bytes, TypedValueDto.class);
            Class<?> clazz = Class.forName(typedValue.getType());
            return objectMapper.convertValue(typedValue.getValue(), clazz);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Error deserializing", e);
        }
    }


}
