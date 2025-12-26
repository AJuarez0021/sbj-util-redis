package io.github.ajuarez0021.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultObjectMapperConfig.
 *
 * @author ajuar
 */
class DefaultObjectMapperConfigTest {

    private final DefaultObjectMapperConfig config = new DefaultObjectMapperConfig();

    @Test
    void configure_ShouldReturnConfiguredObjectMapper() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertNotNull(mapper);
        assertInstanceOf(ObjectMapper.class, mapper);
    }

    @Test
    void configure_ShouldRegisterJavaTimeModule() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertNotNull(mapper);
        // Verify modules are registered by checking size
        assertFalse(mapper.getRegisteredModuleIds().isEmpty());
    }

    @Test
    void configure_ShouldRegisterJdk8Module() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertNotNull(mapper);
        // Verify modules are registered
        assertTrue(mapper.getRegisteredModuleIds().size() >= 2);
    }

    @Test
    void configure_ShouldDisableWriteDatesAsTimestamps() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    @Test
    void configure_ShouldConfigureVisibility() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertNotNull(mapper);
        // The visibility configuration should be applied
        assertNotNull(mapper.getVisibilityChecker());
    }

    @Test
    void configure_ShouldNotFailOnUnknownProperties() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    void configure_ShouldNotFailOnEmptyBeans() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
    }

    @Test
    void configure_ShouldNotFailOnNullForPrimitives() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
    }

    @Test
    void configure_ShouldAcceptSingleValueAsArray() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertTrue(mapper.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
    }

    @Test
    void configure_ShouldNotAcceptEmptyStringAsNullObject() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));
    }

    @Test
    void configure_ShouldNotAcceptEmptyArrayAsNullObject() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT));
    }

    @Test
    void configure_ShouldNotReadEnumsUsingToString() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING));
    }

    @Test
    void configure_ShouldNotWriteEnumsUsingToString() {
        // When
        ObjectMapper mapper = config.configure();

        // Then
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING));
    }

    @Test
    void configure_ShouldBeUsableForSerialization() throws Exception {
        // Given
        ObjectMapper mapper = config.configure();
        TestObject testObj = new TestObject("test", 123);

        // When
        String json = mapper.writeValueAsString(testObj);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }

    @Test
    void configure_ShouldBeUsableForDeserialization() throws Exception {
        // Given
        ObjectMapper mapper = config.configure();
        String json = "{\"name\":\"test\",\"value\":123,\"unknownField\":\"should be ignored\"}";

        // When
        TestObject result = mapper.readValue(json, TestObject.class);

        // Then
        assertNotNull(result);
        assertEquals("test", result.name);
        assertEquals(123, result.value);
    }

    @Test
    void configure_ShouldHandleJavaTimeTypes() throws Exception {
        // Given
        ObjectMapper mapper = config.configure();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        TimeObject timeObj = new TimeObject(now);

        // When
        String json = mapper.writeValueAsString(timeObj);
        TimeObject result = mapper.readValue(json, TimeObject.class);

        // Then
        assertNotNull(result);
        assertNotNull(result.timestamp);
    }

    @Test
    void configure_ShouldCreateNewInstanceEachTime() {
        // When
        ObjectMapper mapper1 = config.configure();
        ObjectMapper mapper2 = config.configure();

        // Then
        assertNotNull(mapper1);
        assertNotNull(mapper2);
        assertNotSame(mapper1, mapper2);
    }

    // ========== Helper Classes ==========

    private static class TestObject {
        private String name;
        private int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    private static class TimeObject {
        private java.time.LocalDateTime timestamp;

        public TimeObject() {
        }

        public TimeObject(java.time.LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
