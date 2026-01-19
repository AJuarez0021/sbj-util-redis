package io.github.ajuarez0021.redis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import io.github.ajuarez0021.redis.dto.CoalescedResponseDto;
import io.github.ajuarez0021.redis.dto.EvictionEventDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CoalesceCacheManager.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
class CoalesceCacheManagerTest {

    /** The redis template. */
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    /** The value operations. */
    @Mock
    private ValueOperations<String, Object> valueOperations;

    /** The listener container. */
    @Mock
    private RedisMessageListenerContainer listenerContainer;

    /** The cache manager. */
    private CoalesceCacheManager cacheManager;

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        cacheManager = new CoalesceCacheManager(redisTemplate, listenerContainer);
    }

    /**
     * Sets up value operations mock.
     */
    private void setupValueOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * Constructor should register message listeners.
     */
    @Test
    void constructor_ShouldRegisterMessageListeners() {
        verify(listenerContainer, times(2)).addMessageListener(
                any(),
                any(ChannelTopic.class));
    }

    /**
     * Put with TTL should set value with expiration.
     */
    @Test
    void put_WithTTL_ShouldSetValueWithExpiration() {
        setupValueOperations();
        String key = "testKey";
        Object value = "testValue";
        long ttlSeconds = 300;

        cacheManager.put(key, value, ttlSeconds);

        verify(valueOperations).set(
                "coalesce:cache:testKey",
                value,
                Duration.ofSeconds(ttlSeconds));
    }

    /**
     * Put without TTL should set value without expiration.
     */
    @Test
    void put_WithoutTTL_ShouldSetValueWithoutExpiration() {
        setupValueOperations();
        String key = "testKey";
        Object value = "testValue";
        long ttlSeconds = 0;

        cacheManager.put(key, value, ttlSeconds);

        verify(valueOperations).set(
                "coalesce:cache:testKey",
                value);
    }

    /**
     * Put with negative TTL should set value without expiration.
     */
    @Test
    void put_WithNegativeTTL_ShouldSetValueWithoutExpiration() {
        setupValueOperations();
        String key = "testKey";
        Object value = "testValue";
        long ttlSeconds = -1;

        cacheManager.put(key, value, ttlSeconds);

        verify(valueOperations).set(
                "coalesce:cache:testKey",
                value);
    }

    /**
     * Get when cache hit should return value.
     */
    @Test
    void get_WhenCacheHit_ShouldReturnValue() {
        setupValueOperations();
        String key = "testKey";
        Object expectedValue = "testValue";
        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(expectedValue);

        Optional<Object> result = cacheManager.get(key);

        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
        verify(valueOperations).get("coalesce:cache:testKey");
    }

    /**
     * Get when cache miss should return empty.
     */
    @Test
    void get_WhenCacheMiss_ShouldReturnEmpty() {
        setupValueOperations();
        String key = "testKey";
        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(null);

        Optional<Object> result = cacheManager.get(key);

        assertFalse(result.isPresent());
        verify(valueOperations).get("coalesce:cache:testKey");
    }

    /**
     * Evict should delete key and publish event.
     */
    @Test
    void evict_ShouldDeleteKeyAndPublishEvent() {
        String key = "testKey";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        cacheManager.evict(key);

        verify(redisTemplate).delete("coalesce:cache:testKey");
        verify(redisTemplate).convertAndSend(
                eq("coalesce:evict"),
                any());
    }

    /**
     * Evict all with matching keys should delete all and publish events.
     */
    @Test
    void evictAll_WithMatchingKeys_ShouldDeleteAllAndPublishEvents() {
        String prefix = "users";
        Set<String> keys = new HashSet<>(Arrays.asList(
                "coalesce:cache:users:1",
                "coalesce:cache:users:2"));
        when(redisTemplate.keys("coalesce:cache:users*")).thenReturn(keys);

        cacheManager.evictAll(prefix);

        verify(redisTemplate).delete(keys);
        verify(redisTemplate, times(2)).convertAndSend(
                eq("coalesce:evict"),
                any());
    }

    /**
     * Evict all with no matching keys should not delete.
     */
    @Test
    void evictAll_WithNoMatchingKeys_ShouldNotDelete() {
        String prefix = "users";
        when(redisTemplate.keys("coalesce:cache:users*")).thenReturn(null);

        cacheManager.evictAll(prefix);

        verify(redisTemplate, never()).delete(any(Set.class));
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    /**
     * Evict all with empty set should not delete.
     */
    @Test
    void evictAll_WithEmptySet_ShouldNotDelete() {
        String prefix = "users";
        when(redisTemplate.keys("coalesce:cache:users*")).thenReturn(Collections.emptySet());

        cacheManager.evictAll(prefix);

        verify(redisTemplate, never()).delete(any(Set.class));
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    /**
     * Evict pattern with matching keys should delete all and publish events.
     */
    @Test
    void evictPattern_WithMatchingKeys_ShouldDeleteAllAndPublishEvents() {
        String pattern = "users:*";
        Set<String> keys = new HashSet<>(Arrays.asList(
                "coalesce:cache:users:1",
                "coalesce:cache:users:2"));
        when(redisTemplate.keys("coalesce:cache:users:*")).thenReturn(keys);

        cacheManager.evictPattern(pattern);

        verify(redisTemplate).delete(keys);
        verify(redisTemplate, times(2)).convertAndSend(
                eq("coalesce:evict"),
                any());
    }

    /**
     * Evict pattern with no matching keys should not delete.
     */
    @Test
    void evictPattern_WithNoMatchingKeys_ShouldNotDelete() {
        String pattern = "users:*";
        when(redisTemplate.keys("coalesce:cache:users:*")).thenReturn(null);

        cacheManager.evictPattern(pattern);

        verify(redisTemplate, never()).delete(any(Set.class));
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    /**
     * Evict pattern with empty set should not delete.
     */
    @Test
    void evictPattern_WithEmptySet_ShouldNotDelete() {
        String pattern = "users:*";
        when(redisTemplate.keys("coalesce:cache:users:*")).thenReturn(Collections.emptySet());

        cacheManager.evictPattern(pattern);

        verify(redisTemplate, never()).delete(any(Set.class));
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    /**
     * Evict multiple should delete all keys and publish events.
     */
    @Test
    void evictMultiple_ShouldDeleteAllKeysAndPublishEvents() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");

        cacheManager.evictMultiple(keys);

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).delete(keysCaptor.capture());

        List<String> capturedKeys = keysCaptor.getValue();
        assertEquals(3, capturedKeys.size());
        assertTrue(capturedKeys.contains("coalesce:cache:key1"));
        assertTrue(capturedKeys.contains("coalesce:cache:key2"));
        assertTrue(capturedKeys.contains("coalesce:cache:key3"));

        verify(redisTemplate, times(3)).convertAndSend(
                eq("coalesce:evict"),
                any());
    }

    /**
     * Evict multiple with empty list should not delete.
     */
    @Test
    void evictMultiple_WithEmptyList_ShouldNotDelete() {
        List<String> keys = Collections.emptyList();

        cacheManager.evictMultiple(keys);

        verify(redisTemplate, never()).delete(any(List.class));
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    /**
     * Exists when key exists should return true.
     */
    @Test
    void exists_WhenKeyExists_ShouldReturnTrue() {
        String key = "testKey";
        when(redisTemplate.hasKey("coalesce:cache:testKey")).thenReturn(true);

        boolean result = cacheManager.exists(key);

        assertTrue(result);
        verify(redisTemplate).hasKey("coalesce:cache:testKey");
    }

    /**
     * Exists when key does not exist should return false.
     */
    @Test
    void exists_WhenKeyDoesNotExist_ShouldReturnFalse() {
        String key = "testKey";
        when(redisTemplate.hasKey("coalesce:cache:testKey")).thenReturn(false);

        boolean result = cacheManager.exists(key);

        assertFalse(result);
        verify(redisTemplate).hasKey("coalesce:cache:testKey");
    }

    /**
     * Exists when result is null should return false.
     */
    @Test
    void exists_WhenResultIsNull_ShouldReturnFalse() {
        String key = "testKey";
        when(redisTemplate.hasKey("coalesce:cache:testKey")).thenReturn(null);

        boolean result = cacheManager.exists(key);

        assertFalse(result);
        verify(redisTemplate).hasKey("coalesce:cache:testKey");
    }

    /**
     * Clear with matching keys should delete all.
     */
    @Test
    void clear_WithMatchingKeys_ShouldDeleteAll() {
        Set<String> keys = new HashSet<>(Arrays.asList(
                "coalesce:cache:key1",
                "coalesce:cache:key2"));
        when(redisTemplate.keys("coalesce:cache:*")).thenReturn(keys);

        cacheManager.clear();

        verify(redisTemplate).delete(keys);
    }

    /**
     * Clear with no matching keys should not delete.
     */
    @Test
    void clear_WithNoMatchingKeys_ShouldNotDelete() {
        when(redisTemplate.keys("coalesce:cache:*")).thenReturn(null);

        cacheManager.clear();

        verify(redisTemplate, never()).delete(any(Set.class));
    }

    /**
     * Clear with empty set should not delete.
     */
    @Test
    void clear_WithEmptySet_ShouldNotDelete() {
        when(redisTemplate.keys("coalesce:cache:*")).thenReturn(Collections.emptySet());

        cacheManager.clear();

        verify(redisTemplate, never()).delete(any(Set.class));
    }

    /**
     * Get time to live should return TTL.
     */
    @Test
    void getTimeToLive_ShouldReturnTTL() {
        String key = "testKey";
        when(redisTemplate.getExpire("coalesce:cache:testKey", TimeUnit.SECONDS))
                .thenReturn(300L);

        long result = cacheManager.getTimeToLive(key);

        assertEquals(300L, result);
        verify(redisTemplate).getExpire("coalesce:cache:testKey", TimeUnit.SECONDS);
    }

    /**
     * Get time to live when null should return minus one.
     */
    @Test
    void getTimeToLive_WhenNull_ShouldReturnMinusOne() {
        String key = "testKey";
        when(redisTemplate.getExpire("coalesce:cache:testKey", TimeUnit.SECONDS))
                .thenReturn(null);

        long result = cacheManager.getTimeToLive(key);

        assertEquals(-1L, result);
        verify(redisTemplate).getExpire("coalesce:cache:testKey", TimeUnit.SECONDS);
    }

    /**
     * Expire should set expiration.
     */
    @Test
    void expire_ShouldSetExpiration() {
        String key = "testKey";
        long ttlSeconds = 600;
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        cacheManager.expire(key, ttlSeconds);

        verify(redisTemplate).expire(
                "coalesce:cache:testKey",
                Duration.ofSeconds(ttlSeconds));
    }


    /**
     * GetOrLoad when cache hit should return cached value.
     */
    @Test
    void getOrLoad_WhenCacheHit_ShouldReturnCachedValue() {
        setupValueOperations();
        String key = "testKey";
        Object expectedValue = "cachedValue";
        Supplier<Object> loader = () -> "loadedValue";

        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(expectedValue);

        Object result = cacheManager.getOrLoad(key, loader, 300, false);

        assertEquals(expectedValue, result);
        verify(valueOperations).get("coalesce:cache:testKey");
        verify(valueOperations, never()).setIfAbsent(anyString(), any(), any(Duration.class));
    }

    /**
     * GetOrLoad when lock acquired should execute loader and cache result.
     */
    @Test
    void getOrLoad_WhenLockAcquired_ShouldExecuteLoaderAndCacheResult() {
        setupValueOperations();
        String key = "testKey";
        Object loadedValue = "loadedValue";
        Supplier<Object> loader = () -> loadedValue;

        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq("coalesce:lock:testKey"),
                anyString(),
                eq(Duration.ofSeconds(30)))).thenReturn(true);
        when(redisTemplate.delete("coalesce:lock:testKey")).thenReturn(true);

        Object result = cacheManager.getOrLoad(key, loader, 300, false);

        assertEquals(loadedValue, result);
        verify(valueOperations).set(
                eq("coalesce:cache:testKey"),
                eq(loadedValue),
                eq(Duration.ofSeconds(300)));
        verify(redisTemplate).convertAndSend(eq("coalesce:ready"), any(CoalescedResponseDto.class));
        verify(redisTemplate).delete("coalesce:lock:testKey");
    }

    /**
     * GetOrLoad when loader returns null and cacheNull is true should cache null.
     */
    @Test
    void getOrLoad_WhenLoaderReturnsNullAndCacheNullTrue_ShouldCacheNull() {
        setupValueOperations();
        String key = "testKey";
        Supplier<Object> loader = () -> null;

        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq("coalesce:lock:testKey"),
                anyString(),
                eq(Duration.ofSeconds(30)))).thenReturn(true);
        when(redisTemplate.delete("coalesce:lock:testKey")).thenReturn(true);

        Object result = cacheManager.getOrLoad(key, loader, 300, true);

        assertNull(result);
        verify(valueOperations).set(
                eq("coalesce:cache:testKey"),
                isNull(),
                eq(Duration.ofSeconds(300)));
        verify(redisTemplate).delete("coalesce:lock:testKey");
    }

    /**
     * GetOrLoad when loader returns null and cacheNull is false should not cache.
     */
    @Test
    void getOrLoad_WhenLoaderReturnsNullAndCacheNullFalse_ShouldNotCache() {
        setupValueOperations();
        String key = "testKey";
        Supplier<Object> loader = () -> null;

        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq("coalesce:lock:testKey"),
                anyString(),
                eq(Duration.ofSeconds(30)))).thenReturn(true);
        when(redisTemplate.delete("coalesce:lock:testKey")).thenReturn(true);

        Object result = cacheManager.getOrLoad(key, loader, 300, false);

        assertNull(result);
        verify(valueOperations, never()).set(
                eq("coalesce:cache:testKey"),
                any(),
                any(Duration.class));
        verify(redisTemplate).delete("coalesce:lock:testKey");
    }

    /**
     * GetOrLoad when loader throws exception should publish error and rethrow.
     */
    @Test
    void getOrLoad_WhenLoaderThrowsException_ShouldPublishErrorAndRethrow() {
        setupValueOperations();
        String key = "testKey";
        RuntimeException expectedException = new RuntimeException("Loader failed");
        Supplier<Object> loader = () -> {
            throw expectedException;
        };

        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq("coalesce:lock:testKey"),
                anyString(),
                eq(Duration.ofSeconds(30)))).thenReturn(true);
        when(redisTemplate.delete("coalesce:lock:testKey")).thenReturn(true);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> cacheManager.getOrLoad(key, loader, 300, false));

        assertEquals(expectedException, thrown);
        verify(redisTemplate).convertAndSend(eq("coalesce:ready"), any(CoalescedResponseDto.class));
        verify(redisTemplate).delete("coalesce:lock:testKey");
    }

    /**
     * GetOrLoad when lock not acquired should wait for result.
     */
    @Test
    void getOrLoad_WhenLockNotAcquired_ShouldThrowOnTimeout() {
        setupValueOperations();
        String key = "testKey";
        Supplier<Object> loader = () -> "loadedValue";

        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq("coalesce:lock:testKey"),
                anyString(),
                eq(Duration.ofSeconds(30)))).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> cacheManager.getOrLoad(key, loader, 1, false));
    }

    /**
     * GetOrLoad when lock not acquired and cache populated should return cached value.
     */
    @Test
    void getOrLoad_WhenLockNotAcquiredAndCachePopulated_ShouldReturnCachedValue() {
        setupValueOperations();
        String key = "testKey";
        Object cachedValue = "cachedByOtherInstance";
        Supplier<Object> loader = () -> "loadedValue";

        when(valueOperations.get("coalesce:cache:testKey"))
                .thenReturn(null)
                .thenReturn(cachedValue);
        when(valueOperations.setIfAbsent(
                eq("coalesce:lock:testKey"),
                anyString(),
                eq(Duration.ofSeconds(30)))).thenReturn(false);

        Object result = cacheManager.getOrLoad(key, loader, 1, false);

        assertEquals(cachedValue, result);
    }

    /**
     * GetOrLoad with zero TTL should use default timeout.
     */
    @Test
    void getOrLoad_WithZeroTTL_ShouldUseDefaultTimeout() {
        setupValueOperations();
        String key = "testKey";
        Object loadedValue = "loadedValue";
        Supplier<Object> loader = () -> loadedValue;

        when(valueOperations.get("coalesce:cache:testKey")).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq("coalesce:lock:testKey"),
                anyString(),
                eq(Duration.ofSeconds(30)))).thenReturn(true);
        when(redisTemplate.delete("coalesce:lock:testKey")).thenReturn(true);

        Object result = cacheManager.getOrLoad(key, loader, 0, false);

        assertEquals(loadedValue, result);
        verify(valueOperations).set("coalesce:cache:testKey", loadedValue);
    }


    /**
     * HandleCoalesceEvent with successful response should complete future.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleCoalesceEvent_WithSuccessResponse_ShouldCompleteFuture() throws Exception {
        String key = "testKey";
        Object expectedResult = "result";
        CoalescedResponseDto response = new CoalescedResponseDto(
                "requestId", key, expectedResult, null, LocalDateTime.now());

        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doReturn(response).when(serializer).deserialize(any());

        CompletableFuture<Object> future = new CompletableFuture<>();
        ConcurrentHashMap<String, CompletableFuture<Object>> pendingRequests = getPendingRequests();
        pendingRequests.put(key, future);

        invokeHandleCoalesceEvent(message, null);

        assertTrue(future.isDone());
        assertEquals(expectedResult, future.get());
    }

    /**
     * HandleCoalesceEvent with error response should complete exceptionally.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleCoalesceEvent_WithErrorResponse_ShouldCompleteExceptionally() throws Exception {
        String key = "testKey";
        RuntimeException error = new RuntimeException("Test error");
        CoalescedResponseDto response = new CoalescedResponseDto(
                "requestId", key, null, error, LocalDateTime.now());

        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doReturn(response).when(serializer).deserialize(any());

        CompletableFuture<Object> future = new CompletableFuture<>();
        ConcurrentHashMap<String, CompletableFuture<Object>> pendingRequests = getPendingRequests();
        pendingRequests.put(key, future);

        invokeHandleCoalesceEvent(message, null);

        assertTrue(future.isCompletedExceptionally());
    }

    /**
     * HandleCoalesceEvent with null response should not throw.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleCoalesceEvent_WithNullResponse_ShouldNotThrow() throws Exception {
        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doReturn(null).when(serializer).deserialize(any());

        assertDoesNotThrow(() -> invokeHandleCoalesceEvent(message, null));
    }

    /**
     * HandleCoalesceEvent with no pending future should not throw.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleCoalesceEvent_WithNoPendingFuture_ShouldNotThrow() throws Exception {
        String key = "nonExistentKey";
        CoalescedResponseDto response = new CoalescedResponseDto(
                "requestId", key, "result", null, LocalDateTime.now());

        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doReturn(response).when(serializer).deserialize(any());

        assertDoesNotThrow(() -> invokeHandleCoalesceEvent(message, null));
    }

    /**
     * HandleCoalesceEvent with serialization exception should not throw.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleCoalesceEvent_WithSerializationException_ShouldNotThrow() throws Exception {
        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doThrow(new SerializationException("Test")).when(serializer).deserialize(any());

        assertDoesNotThrow(() -> invokeHandleCoalesceEvent(message, null));
    }


    /**
     * HandleEvictionEvent with valid event should not throw.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleEvictionEvent_WithValidEvent_ShouldNotThrow() throws Exception {
        EvictionEventDto event = new EvictionEventDto("testKey", LocalDateTime.now());

        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doReturn(event).when(serializer).deserialize(any());

        assertDoesNotThrow(() -> invokeHandleEvictionEvent(message, null));
    }

    /**
     * HandleEvictionEvent with null event should not throw.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleEvictionEvent_WithNullEvent_ShouldNotThrow() throws Exception {
        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doReturn(null).when(serializer).deserialize(any());

        assertDoesNotThrow(() -> invokeHandleEvictionEvent(message, null));
    }

    /**
     * HandleEvictionEvent with serialization exception should not throw.
     */
    @Test
    @SuppressWarnings("unchecked")
    void handleEvictionEvent_WithSerializationException_ShouldNotThrow() throws Exception {
        Message message = mock(Message.class);
        RedisSerializer<Object> serializer = mock(RedisSerializer.class);
        doReturn(serializer).when(redisTemplate).getValueSerializer();
        doThrow(new SerializationException("Test")).when(serializer).deserialize(any());

        assertDoesNotThrow(() -> invokeHandleEvictionEvent(message, null));
    }

    /**
     * Gets pending requests map via reflection.
     */
    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, CompletableFuture<Object>> getPendingRequests() throws Exception {
        Field field = CoalesceCacheManager.class.getDeclaredField("pendingRequests");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, CompletableFuture<Object>>) field.get(cacheManager);
    }

    /**
     * Invokes handleCoalesceEvent via reflection.
     */
    private void invokeHandleCoalesceEvent(Message message, byte[] pattern) throws Exception {
        Method method = CoalesceCacheManager.class.getDeclaredMethod(
                "handleCoalesceEvent", Message.class, byte[].class);
        method.setAccessible(true);
        method.invoke(cacheManager, message, pattern);
    }

    /**
     * Invokes handleEvictionEvent via reflection.
     */
    private void invokeHandleEvictionEvent(Message message, byte[] pattern) throws Exception {
        Method method = CoalesceCacheManager.class.getDeclaredMethod(
                "handleEvictionEvent", Message.class, byte[].class);
        method.setAccessible(true);
        method.invoke(cacheManager, message, pattern);
    }

}
