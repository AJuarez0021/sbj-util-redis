package io.github.ajuarez0021.redis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisCacheService.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        // Setup will be done in individual tests as needed
    }

    // ========== Parameter Validation Tests ==========

    @Test
    void cacheable_WithNullCacheName_ShouldThrowNullPointerException() {
        // Given
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> cacheService.cacheable(null, key, loader, ttl));
        assertEquals("cacheName cannot be null", exception.getMessage());
    }

    @Test
    void cacheable_WithNullKey_ShouldThrowNullPointerException() {
        // Given
        String cacheName = "testCache";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> cacheService.cacheable(cacheName, null, loader, ttl));
        assertEquals("key cannot be null", exception.getMessage());
    }

    @Test
    void cacheable_WithNullLoader_ShouldThrowNullPointerException() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> cacheService.cacheable(cacheName, key, null, ttl));
        assertEquals("loader cannot be null", exception.getMessage());
    }

    @Test
    void cacheable_WithNullTtl_ShouldThrowNullPointerException() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        Supplier<String> loader = () -> "value";

        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> cacheService.cacheable(cacheName, key, loader, null));
        assertEquals("ttl cannot be null", exception.getMessage());
    }

    // ========== Cache Hit/Miss Tests ==========

    @Test
    void cacheable_WhenCacheHit_ShouldReturnCachedValue() {
        // Given
        String cacheName = "users";
        String key = "user1";
        String cachedValue = "John Doe";
        Supplier<String> loader = () -> "Jane Doe"; // Should not be called

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(cachedValue);

        // When
        String result = cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10));

        // Then
        assertEquals(cachedValue, result);
        assertEquals("John Doe", result); // Cached value, not loader value
        verify(valueOperations).get("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void cacheable_WhenCacheMiss_ShouldLoadAndCache() {
        // Given
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "Jane Doe";
        Supplier<String> loader = () -> loadedValue;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(null);

        // When
        String result = cacheService.cacheable(cacheName, key, loader, ttl);

        // Then
        assertEquals(loadedValue, result);
        verify(valueOperations).get("users:user1");
        verify(valueOperations).set("users:user1", loadedValue, ttl);
    }

    @Test
    void cacheable_WhenLoaderReturnsNull_ShouldNotCache() {
        // Given
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> null;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(null);

        // When
        String result = cacheService.cacheable(cacheName, key, loader, ttl);

        // Then
        assertNull(result);
        verify(valueOperations).get("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void cacheable_WithDefaultTtl_ShouldUse10Minutes() {
        // Given
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "Jane Doe";
        Supplier<String> loader = () -> loadedValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(null);

        // When
        String result = cacheService.cacheable(cacheName, key, loader);

        // Then
        assertEquals(loadedValue, result);
        verify(valueOperations).set("users:user1", loadedValue, Duration.ofMinutes(10));
    }

    @Test
    void cacheable_WhenRedisThrowsException_ShouldFallbackToLoader() {
        // Given
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "Jane Doe";
        Supplier<String> loader = () -> loadedValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenThrow(new RuntimeException("Redis error"));

        // When
        String result = cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10));

        // Then
        assertEquals(loadedValue, result);
        verify(valueOperations).get("users:user1");
    }

    // ========== Cache Put Tests ==========

    @Test
    void cachePut_ShouldAlwaysExecuteLoaderAndUpdateCache() {
        // Given
        String cacheName = "users";
        String key = "user1";
        String newValue = "Updated User";
        Supplier<String> loader = () -> newValue;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        String result = cacheService.cachePut(cacheName, key, loader, ttl);

        // Then
        assertEquals(newValue, result);
        verify(valueOperations).set("users:user1", newValue, ttl);
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void cachePut_WhenResultIsNull_ShouldEvictKey() {
        // Given
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> null;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.delete("users:user1")).thenReturn(true);

        // When
        String result = cacheService.cachePut(cacheName, key, loader, ttl);

        // Then
        assertNull(result);
        verify(redisTemplate).delete("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void cachePut_WithDefaultTtl_ShouldUse10Minutes() {
        // Given
        String cacheName = "users";
        String key = "user1";
        String newValue = "Updated User";
        Supplier<String> loader = () -> newValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        String result = cacheService.cachePut(cacheName, key, loader);

        // Then
        assertEquals(newValue, result);
        verify(valueOperations).set("users:user1", newValue, Duration.ofMinutes(10));
    }

    @Test
    void cachePut_WhenRedisThrowsException_ShouldStillReturnLoaderResult() {
        // Given
        String cacheName = "users";
        String key = "user1";
        String newValue = "Updated User";
        Supplier<String> loader = () -> newValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        // When
        String result = cacheService.cachePut(cacheName, key, loader, Duration.ofMinutes(10));

        // Then
        assertEquals(newValue, result);
    }

    // ========== Cache Evict Tests ==========

    @Test
    void cacheEvict_ShouldDeleteKey() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenReturn(true);

        // When
        cacheService.cacheEvict(cacheName, key);

        // Then
        verify(redisTemplate).delete("users:user1");
    }

    @Test
    void cacheEvict_WhenKeyDoesNotExist_ShouldNotThrowException() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> cacheService.cacheEvict(cacheName, key));
        verify(redisTemplate).delete("users:user1");
    }

    @Test
    void cacheEvict_WhenRedisThrowsException_ShouldNotThrowException() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertDoesNotThrow(() -> cacheService.cacheEvict(cacheName, key));
    }

    // ========== Cache Evict All Tests ==========

    @Test
    void cacheEvictAll_ShouldExecuteRedisCallback() {
        // Given
        String cacheName = "users";

        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        // When
        cacheService.cacheEvictAll(cacheName);

        // Then
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    void cacheEvictAll_WhenRedisThrowsException_ShouldNotThrowException() {
        // Given
        String cacheName = "users";

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertDoesNotThrow(() -> cacheService.cacheEvictAll(cacheName));
    }

    // ========== Cache Evict Multiple Tests ==========

    @Test
    void cacheEvictMultiple_ShouldDeleteMultipleKeys() {
        // Given
        String cacheName = "users";
        String[] keys = { "user1", "user2", "user3" };

        when(redisTemplate.delete(anyList())).thenReturn(3L);

        // When
        cacheService.cacheEvictMultiple(cacheName, keys);

        // Then
        verify(redisTemplate).delete(Arrays.asList("users:user1", "users:user2", "users:user3"));
    }

    // ========== Cache Evict By Pattern Tests ==========

    @Test
    void cacheEvictByPattern_ShouldExecuteRedisCallback() {
        // Given
        String pattern = "users:admin:*";

        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        // When
        cacheService.cacheEvictByPattern(pattern);

        // Then
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    void cacheEvictByPattern_WhenRedisThrowsException_ShouldNotThrowException() {
        // Given
        String pattern = "users:admin:*";

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertDoesNotThrow(() -> cacheService.cacheEvictByPattern(pattern));
    }

    @Test
    void cacheEvictByPattern_WithComplexPattern_ShouldExecuteRedisCallback() {
        // Given
        String pattern = "cache:user:*:session";

        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        // When
        cacheService.cacheEvictByPattern(pattern);

        // Then
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    // ========== Exists Tests ==========

    @Test
    void exists_WhenKeyExists_ShouldReturnTrue() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenReturn(true);

        // When
        boolean result = cacheService.exists(cacheName, key);

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey("users:user1");
    }

    @Test
    void exists_WhenKeyDoesNotExist_ShouldReturnFalse() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenReturn(false);

        // When
        boolean result = cacheService.exists(cacheName, key);

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey("users:user1");
    }

    @Test
    void exists_WhenRedisThrowsException_ShouldReturnFalse() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = cacheService.exists(cacheName, key);

        // Then
        assertFalse(result);
    }

    // ========== Validation Tests ==========

    @Test
    void cacheable_WithEmptyCacheName_ShouldThrowException() {
        // Given
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable("", key, loader, ttl));
    }

    @Test
    void cacheable_WithEmptyKey_ShouldThrowException() {
        // Given
        String cacheName = "testCache";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, "", loader, ttl));
    }

    @Test
    void cacheable_WithNegativeTTL_ShouldThrowException() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(-1);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
    }

    // ========== Get TTL Tests ==========

    @Test
    void getTTL_ShouldReturnRemainingSeconds() {
        // Given
        String cacheName = "users";
        String key = "user1";
        Long expectedTtl = 600L; // 10 minutes in seconds

        when(redisTemplate.getExpire("users:user1", TimeUnit.SECONDS)).thenReturn(expectedTtl);

        // When
        Long result = cacheService.getTTL(cacheName, key);

        // Then
        assertEquals(expectedTtl, result);
        verify(redisTemplate).getExpire("users:user1", TimeUnit.SECONDS);
    }

    @Test
    void getTTL_WhenKeyDoesNotExist_ShouldReturnNegative() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.getExpire("users:user1", TimeUnit.SECONDS)).thenReturn(-2L);

        // When
        Long result = cacheService.getTTL(cacheName, key);

        // Then
        assertEquals(-2L, result);
    }

    @Test
    void getTTL_WhenRedisThrowsException_ShouldReturnNull() {
        // Given
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.getExpire("users:user1", TimeUnit.SECONDS))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        Long result = cacheService.getTTL(cacheName, key);

        // Then
        assertNull(result);
    }

    // ========== Key Format Validation Tests ==========

    @Test
    void cacheable_WithCacheNameContainingColon_ShouldThrowException() {
        // Given
        String cacheName = "test:cache";
        String key = "key1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    @Test
    void cacheable_WithCacheNameContainingAsterisk_ShouldThrowException() {
        // Given
        String cacheName = "test*cache";
        String key = "key1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    @Test
    void cacheable_WithKeyContainingAsterisk_ShouldThrowException() {
        // Given
        String cacheName = "testcache";
        String key = "key*1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("key cannot contain '*' character", exception.getMessage());
    }

    @Test
    void cacheable_WithZeroTTL_ShouldThrowException() {
        // Given
        String cacheName = "testCache";
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ZERO;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("ttl must be positive", exception.getMessage());
    }

    // ========== Cache Evict Multiple Exception Handling Tests ==========

    @Test
    void cacheEvictMultiple_WhenRedisThrowsException_ShouldNotThrowException() {
        // Given
        String cacheName = "users";
        String[] keys = { "user1", "user2", "user3" };

        when(redisTemplate.delete(anyList())).thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertDoesNotThrow(() -> cacheService.cacheEvictMultiple(cacheName, keys));
    }

    @Test
    void cacheEvictMultiple_WithEmptyKeys_ShouldCallDeleteWithEmptyList() {
        // Given
        String cacheName = "users";
        String[] keys = {};

        when(redisTemplate.delete(anyList())).thenReturn(0L);

        // When
        cacheService.cacheEvictMultiple(cacheName, keys);

        // Then - The method still calls delete with an empty list
        verify(redisTemplate).delete(anyList());
    }
}
