package io.github.ajuarez0021.redis.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Unit tests for DefaultObjectMapperConfig.
 *
 * @author ajuar
 */
class DefaultObjectMapperConfigTest {

    /** The config. */
    private final DefaultObjectMapperConfig config = new DefaultObjectMapperConfig();

    /**
     * Configure should return configured object mapper.
     */
    @Test
    void configure_ShouldReturnConfiguredObjectMapper() {
        ObjectMapper mapper = config.configure();

        assertNotNull(mapper);
        assertInstanceOf(ObjectMapper.class, mapper);
    }

    /**
     * Configure should register java time module.
     */
    @Test
    void configure_ShouldRegisterJavaTimeModule() {
        ObjectMapper mapper = config.configure();

        assertNotNull(mapper);
        
        assertFalse(mapper.getRegisteredModuleIds().isEmpty());
    }

    /**
     * Configure should register jdk 8 module.
     */
    @Test
    void configure_ShouldRegisterJdk8Module() {
        ObjectMapper mapper = config.configure();

        assertNotNull(mapper);
        
        assertTrue(mapper.getRegisteredModuleIds().size() >= 2);
    }

    /**
     * Configure should disable write dates as timestamps.
     */
    @Test
    void configure_ShouldDisableWriteDatesAsTimestamps() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    /**
     * Configure should configure visibility.
     */
    @Test
    void configure_ShouldConfigureVisibility() {
        ObjectMapper mapper = config.configure();

        assertNotNull(mapper);
        
        assertNotNull(mapper.getVisibilityChecker());
    }

    /**
     * Configure should not fail on unknown properties.
     */
    @Test
    void configure_ShouldNotFailOnUnknownProperties() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    /**
     * Configure should not fail on empty beans.
     */
    @Test
    void configure_ShouldNotFailOnEmptyBeans() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
    }

    /**
     * Configure should not fail on null for primitives.
     */
    @Test
    void configure_ShouldNotFailOnNullForPrimitives() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
    }

    /**
     * Configure should accept single value as array.
     */
    @Test
    void configure_ShouldAcceptSingleValueAsArray() {
        ObjectMapper mapper = config.configure();

        assertTrue(mapper.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
    }

    /**
     * Configure should not accept empty string as null object.
     */
    @Test
    void configure_ShouldNotAcceptEmptyStringAsNullObject() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));
    }

    /**
     * Configure should not accept empty array as null object.
     */
    @Test
    void configure_ShouldNotAcceptEmptyArrayAsNullObject() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT));
    }

    /**
     * Configure should not read enums using to string.
     */
    @Test
    void configure_ShouldNotReadEnumsUsingToString() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING));
    }

    /**
     * Configure should not write enums using to string.
     */
    @Test
    void configure_ShouldNotWriteEnumsUsingToString() {
        ObjectMapper mapper = config.configure();

        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING));
    }

    /**
     * Configure should be usable for serialization.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_ShouldBeUsableForSerialization() throws Exception {
        ObjectMapper mapper = config.configure();
        TestObject testObj = new TestObject("test", 123);

        String json = mapper.writeValueAsString(testObj);

        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }

    /**
     * Configure should be usable for deserialization.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_ShouldBeUsableForDeserialization() throws Exception {
        ObjectMapper mapper = config.configure();

        String json =
                """
                 {"@class":"io.github.ajuarez0021.redis.config.DefaultObjectMapperConfigTest$TestObject","name":"test","value":123,"unknownField":"should be ignored"}
                """;

        TestObject result = mapper.readValue(json, TestObject.class);

        assertNotNull(result);
        assertEquals("test", result.name);
        assertEquals(123, result.value);
    }

    /**
     * Configure should handle java time types.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_ShouldHandleJavaTimeTypes() throws Exception {
        ObjectMapper mapper = config.configure();
        LocalDateTime now = LocalDateTime.now();
        TimeObject timeObj = new TimeObject(now);

        String json = mapper.writeValueAsString(timeObj);
        TimeObject result = mapper.readValue(json, TimeObject.class);

        assertNotNull(result);
        assertNotNull(result.timestamp);
    }

    /**
     * Configure should create new instance each time.
     */
    @Test
    void configure_ShouldCreateNewInstanceEachTime() {
        
    	ObjectMapper mapper1 = config.configure();
        ObjectMapper mapper2 = config.configure();

        assertNotNull(mapper1);
        assertNotNull(mapper2);
        assertNotSame(mapper1, mapper2);
    }

    
    /**
     * The Class TestObject.
     */
    private static class TestObject {
        
        /** The name. */
        private String name;
        
        /** The value. */
        private int value;

        /** The construct. */
        public TestObject () {
        }

        /**
         * Instantiates a new test object.
         *
         * @param name the name
         * @param value the value
         */
        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * The Class TimeObject.
     */
    private static class TimeObject {
        
        /** The timestamp. */
        private LocalDateTime timestamp;

        /** The construct. */
       public TimeObject() {

       }

        /**
         * Instantiates a new time object.
         *
         * @param timestamp the timestamp
         */
        public TimeObject(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
