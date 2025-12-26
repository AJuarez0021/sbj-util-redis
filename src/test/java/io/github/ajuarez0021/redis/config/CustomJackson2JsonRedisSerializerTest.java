package io.github.ajuarez0021.redis.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomJackson2JsonRedisSerializer.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
class CustomJackson2JsonRedisSerializerTest {

    @Mock
    private ObjectMapper objectMapper;

    private CustomJackson2JsonRedisSerializer serializer;
    private CustomJackson2JsonRedisSerializer realSerializer;

    @BeforeEach
    void setUp() {
        serializer = new CustomJackson2JsonRedisSerializer(objectMapper);
        realSerializer = new CustomJackson2JsonRedisSerializer(new ObjectMapper());
    }

    // ========== Serialize Tests ==========

    @Test
    void serialize_WithNullValue_ShouldReturnEmptyByteArray() {
        // When
        byte[] result = serializer.serialize(null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.length);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void serialize_WithValidObject_ShouldReturnSerializedBytes() throws JsonProcessingException {
        // Given
        String value = "test value";
        byte[] expectedBytes = new byte[]{1, 2, 3, 4};

        when(objectMapper.writeValueAsBytes(any())).thenReturn(expectedBytes);

        // When
        byte[] result = serializer.serialize(value);

        // Then
        assertNotNull(result);
        assertEquals(expectedBytes, result);
        verify(objectMapper).writeValueAsBytes(any());
    }

    @Test
    void serialize_WhenJsonProcessingFails_ShouldThrowSerializationException() throws JsonProcessingException {
        // Given
        String value = "test value";
        JsonProcessingException exception = mock(JsonProcessingException.class);

        when(objectMapper.writeValueAsBytes(any())).thenThrow(exception);

        // When & Then
        SerializationException thrown = assertThrows(SerializationException.class,
                () -> serializer.serialize(value));
        assertEquals("Error serializing", thrown.getMessage());
        assertSame(exception, thrown.getCause());
    }

    @Test
    void serialize_WithComplexObject_ShouldSerializeCorrectly() throws JsonProcessingException {
        // Given
        TestObject testObject = new TestObject("John", 30);
        byte[] expectedBytes = new byte[]{5, 6, 7, 8};

        when(objectMapper.writeValueAsBytes(any())).thenReturn(expectedBytes);

        // When
        byte[] result = serializer.serialize(testObject);

        // Then
        assertNotNull(result);
        assertEquals(expectedBytes, result);
        verify(objectMapper).writeValueAsBytes(any());
    }

    // ========== Deserialize Tests ==========

    @Test
    void deserialize_WithEmptyByteArray_ShouldReturnNull() {
        // Given
        byte[] emptyBytes = new byte[0];

        // When
        Object result = serializer.deserialize(emptyBytes);

        // Then
        assertNull(result);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void deserialize_WhenIOExceptionOccurs_ShouldThrowSerializationException() throws IOException {
        // Given
        byte[] bytes = new byte[]{1, 2, 3, 4};
        IOException exception = new IOException("Read error");

        when(objectMapper.readValue(eq(bytes), any(Class.class))).thenThrow(exception);

        // When & Then
        SerializationException thrown = assertThrows(SerializationException.class,
                () -> serializer.deserialize(bytes));
        assertEquals("Error deserializing", thrown.getMessage());
    }

    // ========== Helper Methods and Classes ==========

    /**
     * Test object for serialization tests
     */
    public static class TestObject {
        private String name;
        private int age;

        public TestObject() {
        }

        public TestObject(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
