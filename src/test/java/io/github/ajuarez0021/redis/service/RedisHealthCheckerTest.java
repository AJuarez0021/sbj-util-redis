package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.dto.RedisStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisHealthChecker.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
class RedisHealthCheckerTest {

    /** The connection factory. */
    @Mock
    private RedisConnectionFactory connectionFactory;

    /** The connection. */
    @Mock
    private RedisConnection connection;

    /** The server commands. */
    @Mock
    private RedisServerCommands serverCommands;

    /** The redis template. */
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    /** The health checker. */
    @InjectMocks
    private RedisHealthChecker healthChecker;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
    }

    /**
     * Creates the mock redis info.
     *
     * @return the properties
     */
    private Properties createMockRedisInfo() {
        Properties properties = new Properties();
        properties.setProperty("used_memory", "1048576");
        properties.setProperty("maxmemory", "2097152");
        properties.setProperty("connected_clients", "5");
        properties.setProperty("redis_version", "7.0.0");
        return properties;
    }

   
    /**
     * Checks if is redis active when redis is up should return connected true.
     */
    @Test
    void isRedisActive_WhenRedisIsUp_ShouldReturnConnectedTrue() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        verify(connection).ping();
        verify(connection).serverCommands();
    }

    /**
     * Checks if is redis active when redis is up should measure response time.
     */
    @Test
    void isRedisActive_WhenRedisIsUp_ShouldMeasureResponseTime() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertTrue(result.getResponseTime() >= 0, "Response time should be non-negative");
    }

    /**
     * Checks if is redis active when ping succeeds should return success message.
     */
    @Test
    void isRedisActive_WhenPingSucceeds_ShouldReturnSuccessMessage() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Redis is up and running") ||
                result.getErrorMessage().contains("active") ||
                result.getErrorMessage().contains("connected"),
                "Message should indicate successful connection");
    }

    /**
     * Checks if is redis active when redis template is null should return not connected.
     */
    @Test
    void isRedisActive_WhenRedisTemplateIsNull_ShouldReturnNotConnected() {
        RedisHealthChecker checkerWithNullTemplate = new RedisHealthChecker(null);

        RedisStatusDto result = checkerWithNullTemplate.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.getErrorMessage().contains("not configured"));
    }

    /**
     * Checks if is redis active when connection factory is null should return not connected.
     */
    @Test
    void isRedisActive_WhenConnectionFactoryIsNull_ShouldReturnNotConnected() {
        when(redisTemplate.getConnectionFactory()).thenReturn(null);

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.getErrorMessage().contains("not available"));
    }

    /**
     * Checks if is redis active when redis is down should return connected false.
     */
    @SuppressWarnings("serial")
	@Test
    void isRedisActive_WhenRedisIsDown_ShouldReturnConnectedFalse() {
        when(connectionFactory.getConnection()).thenThrow(new DataAccessException("Connection refused") {});

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    /**
     * Checks if is redis active when redis is down should include error message.
     */
    @SuppressWarnings("serial")
	@Test
    void isRedisActive_WhenRedisIsDown_ShouldIncludeErrorMessage() {
        String errorMessage = "Connection refused";
        when(connectionFactory.getConnection()).thenThrow(new DataAccessException(errorMessage) {});

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Error checking Redis status"));
    }

    /**
     * Checks if is redis active when exception thrown should return zero response time.
     */
    @SuppressWarnings("serial")
	@Test
    void isRedisActive_WhenExceptionThrown_ShouldReturnZeroResponseTime() {
        when(connectionFactory.getConnection()).thenThrow(new DataAccessException("Connection error") {});

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
        assertEquals(0L, result.getResponseTime());
    }

    /**
     * Checks if is redis active when ping returns non pong should return not connected.
     */
    @Test
    void isRedisActive_WhenPingReturnsNonPong_ShouldReturnNotConnected() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("INVALID");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.getErrorMessage().contains("not responding as expected"));
    }

    /**
     * Checks if is redis active when ping returns null should handle gracefully.
     */
    @Test
    void isRedisActive_WhenPingReturnsNull_ShouldHandleGracefully() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(null);
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    /**
     * Checks if is redis active when ping returns empty string should handle gracefully.
     */
    @Test
    void isRedisActive_WhenPingReturnsEmptyString_ShouldHandleGracefully() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    /**
     * Checks if is redis active when ping returns pong lowercase should return connected.
     */
    @Test
    void isRedisActive_WhenPingReturnsPongLowercase_ShouldReturnConnected() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("pong");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
    }
}
