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

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @Mock
    private RedisServerCommands serverCommands;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private RedisHealthChecker healthChecker;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
    }

    private Properties createMockRedisInfo() {
        Properties properties = new Properties();
        properties.setProperty("used_memory", "1048576");
        properties.setProperty("maxmemory", "2097152");
        properties.setProperty("connected_clients", "5");
        properties.setProperty("redis_version", "7.0.0");
        return properties;
    }

    // ========== Successful Health Check Tests ==========

    @Test
    void isRedisActive_WhenRedisIsUp_ShouldReturnConnectedTrue() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertTrue(result.isConnected());
        verify(connection).ping();
        verify(connection).serverCommands();
    }

    @Test
    void isRedisActive_WhenRedisIsUp_ShouldMeasureResponseTime() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertTrue(result.isConnected());
        assertTrue(result.getResponseTime() >= 0, "Response time should be non-negative");
    }

    @Test
    void isRedisActive_WhenPingSucceeds_ShouldReturnSuccessMessage() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertTrue(result.isConnected());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Redis is up and running") ||
                result.getErrorMessage().contains("active") ||
                result.getErrorMessage().contains("connected"),
                "Message should indicate successful connection");
    }

    // ========== Failed Health Check Tests ==========

    @Test
    void isRedisActive_WhenRedisTemplateIsNull_ShouldReturnNotConnected() {
        // Given
        RedisHealthChecker checkerWithNullTemplate = new RedisHealthChecker(null);

        // When
        RedisStatusDto result = checkerWithNullTemplate.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.getErrorMessage().contains("not configured"));
    }

    @Test
    void isRedisActive_WhenConnectionFactoryIsNull_ShouldReturnNotConnected() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(null);

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.getErrorMessage().contains("not available"));
    }

    @Test
    void isRedisActive_WhenRedisIsDown_ShouldReturnConnectedFalse() {
        // Given
        when(connectionFactory.getConnection()).thenThrow(new DataAccessException("Connection refused") {});

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    @Test
    void isRedisActive_WhenRedisIsDown_ShouldIncludeErrorMessage() {
        // Given
        String errorMessage = "Connection refused";
        when(connectionFactory.getConnection()).thenThrow(new DataAccessException(errorMessage) {});

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Error checking Redis status"));
    }

    @Test
    void isRedisActive_WhenExceptionThrown_ShouldReturnZeroResponseTime() {
        // Given
        when(connectionFactory.getConnection()).thenThrow(new DataAccessException("Connection error") {});

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
        assertEquals(0L, result.getResponseTime());
    }

    @Test
    void isRedisActive_WhenPingReturnsNonPong_ShouldReturnNotConnected() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("INVALID");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.getErrorMessage().contains("not responding as expected"));
    }

    // ========== Edge Cases ==========

    @Test
    void isRedisActive_WhenPingReturnsNull_ShouldHandleGracefully() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(null);
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    @Test
    void isRedisActive_WhenPingReturnsEmptyString_ShouldHandleGracefully() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    @Test
    void isRedisActive_WhenPingReturnsPongLowercase_ShouldReturnConnected() {
        // Given
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("pong");
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(createMockRedisInfo());

        // When
        RedisStatusDto result = healthChecker.isRedisActive();

        // Then
        assertNotNull(result);
        assertTrue(result.isConnected());
    }
}
