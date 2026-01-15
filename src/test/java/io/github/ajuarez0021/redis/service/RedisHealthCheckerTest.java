package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.dto.RedisStatusDto;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;


import java.util.concurrent.TimeUnit;

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

    /** The redis async commands. */
    @Mock
    private RedisAsyncCommands<Object, Object> asyncCommands;

    /** The redis future. */
    @Mock
    private RedisFuture<String> redisFuture;

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
        lenient().when(connectionFactory.getConnection()).thenReturn(connection);
    }

    /**
     * Creates the mock redis info string.
     *
     * @return the info string
     */
    private String createMockRedisInfoString() {
        return """
            # Server
            redis_version:7.0.0
            redis_mode:standalone
            os:Linux 5.10.0-28-amd64 x86_64
            arch_bits:64

            # Clients
            connected_clients:5

            # Memory
            used_memory:1048576
            used_memory_human:1.00M
            used_memory_rss:2097152
            maxmemory:2097152
            maxmemory_human:2.00M
            total_system_memory:2097152

            # Stats
            total_connections_received:100
            total_commands_processed:1000
            """;
    }


    /**
     * Checks if is redis active when redis is up should return connected true.
     */
    @Test
    void isRedisActive_WhenRedisIsUp_ShouldReturnConnectedTrue() throws Exception {
        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(createMockRedisInfoString());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertEquals(1048576L, result.getUsedMemory());
        assertEquals(2097152L, result.getMaxMemory());
        assertEquals(5, result.getConnectedClients());
        assertEquals("7.0.0", result.getRedisVersion());
        verify(connection).ping();
        verify(connection).getNativeConnection();
    }

    /**
     * Checks if is redis active when redis is up should measure response time.
     */
    @Test
    void isRedisActive_WhenRedisIsUp_ShouldMeasureResponseTime() throws Exception {
        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(createMockRedisInfoString());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertTrue(result.getResponseTime() >= 0, "Response time should be non-negative");
    }

    /**
     * Checks if is redis active when ping succeeds should return success message.
     */
    @Test
    void isRedisActive_WhenPingSucceeds_ShouldReturnSuccessMessage() throws Exception {
        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(createMockRedisInfoString());

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
    void isRedisActive_WhenPingReturnsNonPong_ShouldReturnNotConnected() throws Exception {
        when(connection.ping()).thenReturn("INVALID");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(createMockRedisInfoString());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.getErrorMessage().contains("not responding as expected"));
    }

    /**
     * Checks if is redis active when ping returns null should handle gracefully.
     */
    @Test
    void isRedisActive_WhenPingReturnsNull_ShouldHandleGracefully() throws Exception {
        when(connection.ping()).thenReturn(null);
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(createMockRedisInfoString());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    /**
     * Checks if is redis active when ping returns empty string should handle gracefully.
     */
    @Test
    void isRedisActive_WhenPingReturnsEmptyString_ShouldHandleGracefully() throws Exception {
        when(connection.ping()).thenReturn("");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(createMockRedisInfoString());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertFalse(result.isConnected());
    }

    /**
     * Checks if is redis active when ping returns pong lowercase should return connected.
     */
    @Test
    void isRedisActive_WhenPingReturnsPongLowercase_ShouldReturnConnected() throws Exception {
        when(connection.ping()).thenReturn("pong");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(createMockRedisInfoString());

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
    }

    /**
     * Checks if is redis active when redis info has invalid memory values should handle gracefully.
     */
    @Test
    void isRedisActive_WhenRedisInfoHasInvalidMemoryValues_ShouldHandleGracefully() throws Exception {
        String infoWithInvalidNumbers = """
            # Server
            redis_version:7.0.0

            # Clients
            connected_clients:invalid_number

            # Memory
            used_memory:not_a_number
            total_system_memory:invalid
            """;

        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(infoWithInvalidNumbers);

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertEquals(0L, result.getUsedMemory());
        assertEquals(0L, result.getMaxMemory());
        assertEquals(0, result.getConnectedClients());
    }

    /**
     * Checks if is redis active when redis info has malformed lines should handle gracefully.
     */
    @Test
    void isRedisActive_WhenRedisInfoHasMalformedLines_ShouldHandleGracefully() throws Exception {
        String infoWithMalformedLines = """
            # Server
            redis_version:7.0.0

            # Invalid lines
            line_without_colon
            :value_without_key

            # Clients
            connected_clients:5

            # Memory
            used_memory:1048576
            total_system_memory:2097152
            """;

        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(infoWithMalformedLines);

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertEquals("7.0.0", result.getRedisVersion());
        assertEquals(5, result.getConnectedClients());
    }

    /**
     * Checks if is redis active when redis info is empty string should handle gracefully.
     */
    @Test
    void isRedisActive_WhenRedisInfoIsEmptyString_ShouldHandleGracefully() throws Exception {
        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class))).thenReturn("");

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertEquals(0L, result.getUsedMemory());
        assertEquals(0L, result.getMaxMemory());
        assertEquals(0, result.getConnectedClients());
        assertEquals("", result.getRedisVersion());
    }

    /**
     * Checks if is redis active when native connection info times out should handle gracefully.
     */
    @Test
    void isRedisActive_WhenNativeConnectionInfoTimesOut_ShouldHandleGracefully() throws Exception {
        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class)))
                .thenThrow(new java.util.concurrent.TimeoutException("Timeout"));

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertEquals(0L, result.getUsedMemory());
        assertEquals(0L, result.getMaxMemory());
    }

    /**
     * Checks if is redis active when native connection info throws execution exception
     * should handle gracefully.
     */
    @Test
    void isRedisActive_WhenNativeConnectionInfoThrowsExecutionException_ShouldHandleGracefully()
            throws Exception {
        when(connection.ping()).thenReturn("PONG");
        when(connection.getNativeConnection()).thenReturn(asyncCommands);
        when(asyncCommands.info()).thenReturn(redisFuture);
        when(redisFuture.get(anyLong(), any(TimeUnit.class)))
                .thenThrow(new java.util.concurrent.ExecutionException("Execution failed", new Exception()));

        RedisStatusDto result = healthChecker.isRedisActive();

        assertNotNull(result);
        assertTrue(result.isConnected());
        assertEquals(0L, result.getUsedMemory());
        assertEquals(0L, result.getMaxMemory());
    }
}
