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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
     * Constructor should register message listener.
     */
    @Test
    void constructor_ShouldRegisterMessageListener() {
        verify(listenerContainer).addMessageListener(
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

}
