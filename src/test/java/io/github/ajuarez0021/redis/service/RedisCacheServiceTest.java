package io.github.ajuarez0021.redis.service;

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

    /** The redis template. */
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    /** The value operations. */
    @Mock
    private ValueOperations<String, Object> valueOperations;

    /** The cache service. */
    @InjectMocks
    private RedisCacheService cacheService;

   
    /**
     * Cacheable with null cache name should throw illegal state exception.
     */
    @Test
    void cacheable_WithNullCacheName_ShouldThrowNullPointerException() {
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> cacheService.cacheable(null, key, loader, ttl));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Cacheable with null key should throw illegal state exception.
     */
    @Test
    void cacheable_WithNullKey_ShouldThrowNullPointerException() {
        String cacheName = "testCache";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> cacheService.cacheable(cacheName, null, loader, ttl));
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Cacheable with null loader should throw illegal state exception.
     */
    @Test
    void cacheable_WithNullLoader_ShouldThrowNullPointerException() {
        String cacheName = "testCache";
        String key = "testKey";
        Duration ttl = Duration.ofMinutes(10);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> cacheService.cacheable(cacheName, key, null, ttl));
        assertEquals("loader is required", exception.getMessage());
    }

    /**
     * Cacheable with null ttl should throw illegal state exception.
     */
    @Test
    void cacheable_WithNullTtl_ShouldThrowNullPointerException() {
        String cacheName = "testCache";
        String key = "testKey";
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> cacheService.cacheable(cacheName, key, loader, null));
        assertEquals("ttl is required", exception.getMessage());
    }

    /**
     * Cacheable when cache hit should return cached value.
     */
    @Test
    void cacheable_WhenCacheHit_ShouldReturnCachedValue() {
        String cacheName = "users";
        String key = "user1";
        String cachedValue = "John Doe";
        Supplier<String> loader = () -> "Jane Doe";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(cachedValue);

        String result = cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10));

        assertEquals(cachedValue, result);
        assertEquals("John Doe", result);
        verify(valueOperations).get("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    /**
     * Cacheable when cache miss should load and cache.
     */
    @Test
    void cacheable_WhenCacheMiss_ShouldLoadAndCache() {
        
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "Jane Doe";
        Supplier<String> loader = () -> loadedValue;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(null);

        String result = cacheService.cacheable(cacheName, key, loader, ttl);

        assertEquals(loadedValue, result);
        verify(valueOperations).get("users:user1");
        verify(valueOperations).set("users:user1", loadedValue, ttl);
    }

    /**
     * Cacheable when loader returns null should not cache.
     */
    @Test
    void cacheable_WhenLoaderReturnsNull_ShouldNotCache() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> null;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(null);

        String result = cacheService.cacheable(cacheName, key, loader, ttl);

        assertNull(result);
        verify(valueOperations).get("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    /**
     * Cacheable with default ttl should use 10 minutes.
     */
    @Test
    void cacheable_WithDefaultTtl_ShouldUse10Minutes() {
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "Jane Doe";
        Supplier<String> loader = () -> loadedValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenReturn(null);

        String result = cacheService.cacheable(cacheName, key, loader);

        assertEquals(loadedValue, result);
        verify(valueOperations).set("users:user1", loadedValue, Duration.ofMinutes(10));
    }

    /**
     * Cacheable when redis throws exception should fallback to loader.
     */
    @Test
    void cacheable_WhenRedisThrowsException_ShouldFallbackToLoader() {
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "Jane Doe";
        Supplier<String> loader = () -> loadedValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("users:user1")).thenThrow(new RuntimeException("Redis error"));

        String result = cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10));

        assertEquals(loadedValue, result);
        verify(valueOperations).get("users:user1");
    }


    /**
     * Cache put should always execute loader and update cache.
     */
    @Test
    void cachePut_ShouldAlwaysExecuteLoaderAndUpdateCache() {
        String cacheName = "users";
        String key = "user1";
        String newValue = "Updated User";
        Supplier<String> loader = () -> newValue;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String result = cacheService.cachePut(cacheName, key, loader, ttl);

        assertEquals(newValue, result);
        verify(valueOperations).set("users:user1", newValue, ttl);
        verify(valueOperations, never()).get(anyString());
    }

    /**
     * Cache put when result is null should evict key.
     */
    @Test
    void cachePut_WhenResultIsNull_ShouldEvictKey() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> null;
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.delete("users:user1")).thenReturn(true);

        String result = cacheService.cachePut(cacheName, key, loader, ttl);

        assertNull(result);
        verify(redisTemplate).delete("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    /**
     * Cache put with default ttl should use 10 minutes.
     */
    @Test
    void cachePut_WithDefaultTtl_ShouldUse10Minutes() {
        String cacheName = "users";
        String key = "user1";
        String newValue = "Updated User";
        Supplier<String> loader = () -> newValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String result = cacheService.cachePut(cacheName, key, loader);

        assertEquals(newValue, result);
        verify(valueOperations).set("users:user1", newValue, Duration.ofMinutes(10));
    }

    /**
     * Cache put when redis throws exception should still return loader result.
     */
    @Test
    void cachePut_WhenRedisThrowsException_ShouldStillReturnLoaderResult() {
        String cacheName = "users";
        String key = "user1";
        String newValue = "Updated User";
        Supplier<String> loader = () -> newValue;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        String result = cacheService.cachePut(cacheName, key, loader, Duration.ofMinutes(10));

        assertEquals(newValue, result);
    }

    /**
     * Cache evict should delete key.
     */
    @Test
    void cacheEvict_ShouldDeleteKey() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenReturn(true);

        cacheService.cacheEvict(cacheName, key);

        verify(redisTemplate).delete("users:user1");
    }

    /**
     * Cache evict when key does not exist should not throw exception.
     */
    @Test
    void cacheEvict_WhenKeyDoesNotExist_ShouldNotThrowException() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenReturn(false);

        assertDoesNotThrow(() -> cacheService.cacheEvict(cacheName, key));
        verify(redisTemplate).delete("users:user1");
    }

    /**
     * Cache evict when redis throws exception should not throw exception.
     */
    @Test
    void cacheEvict_WhenRedisThrowsException_ShouldNotThrowException() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> cacheService.cacheEvict(cacheName, key));
    }

    /**
     * Cache evict all should execute redis callback.
     */
    @SuppressWarnings("unchecked")
	@Test
    void cacheEvictAll_ShouldExecuteRedisCallback() {
        String cacheName = "users";

        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        cacheService.cacheEvictAll(cacheName);

        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    /**
     * Cache evict all when redis throws exception should not throw exception.
     */
    @SuppressWarnings("unchecked")
	@Test
    void cacheEvictAll_WhenRedisThrowsException_ShouldNotThrowException() {
        String cacheName = "users";

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> cacheService.cacheEvictAll(cacheName));
    }


    /**
     * Cache evict multiple should delete multiple keys.
     */
    @Test
    void cacheEvictMultiple_ShouldDeleteMultipleKeys() {
        String cacheName = "users";
        String[] keys = { "user1", "user2", "user3" };

        when(redisTemplate.delete(anyList())).thenReturn(3L);

        cacheService.cacheEvictMultiple(cacheName, keys);

        verify(redisTemplate).delete(Arrays.asList("users:user1", "users:user2", "users:user3"));
    }


    /**
     * Cache evict by pattern should execute redis callback.
     */
    @SuppressWarnings("unchecked")
	@Test
    void cacheEvictByPattern_ShouldExecuteRedisCallback() {
        String pattern = "users:admin:*";

        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        cacheService.cacheEvictByPattern(pattern);

        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    /**
     * Cache evict by pattern when redis throws exception should not throw exception.
     */
    @SuppressWarnings("unchecked")
	@Test
    void cacheEvictByPattern_WhenRedisThrowsException_ShouldNotThrowException() {
        String pattern = "users:admin:*";

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> cacheService.cacheEvictByPattern(pattern));
    }

    /**
     * Cache evict by pattern with complex pattern should execute redis callback.
     */
    @SuppressWarnings("unchecked")
	@Test
    void cacheEvictByPattern_WithComplexPattern_ShouldExecuteRedisCallback() {
        String pattern = "cache:user:*:session";

        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        cacheService.cacheEvictByPattern(pattern);

        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    /**
     * Exists when key exists should return true.
     */
    @Test
    void exists_WhenKeyExists_ShouldReturnTrue() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenReturn(true);

        boolean result = cacheService.exists(cacheName, key);

        assertTrue(result);
        verify(redisTemplate).hasKey("users:user1");
    }

    /**
     * Exists when key does not exist should return false.
     */
    @Test
    void exists_WhenKeyDoesNotExist_ShouldReturnFalse() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenReturn(false);

        boolean result = cacheService.exists(cacheName, key);

        assertFalse(result);
        verify(redisTemplate).hasKey("users:user1");
    }

    /**
     * Exists when redis throws exception should return false.
     */
    @Test
    void exists_WhenRedisThrowsException_ShouldReturnFalse() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenThrow(new RuntimeException("Redis error"));

        boolean result = cacheService.exists(cacheName, key);

        assertFalse(result);
    }


    /**
     * Cacheable with empty cache name should throw illegal state exception.
     */
    @Test
    void cacheable_WithEmptyCacheName_ShouldThrowException() {
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> cacheService.cacheable("", key, loader, ttl));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Cacheable with empty key should throw illegal state exception.
     */
    @Test
    void cacheable_WithEmptyKey_ShouldThrowException() {
        String cacheName = "testCache";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> cacheService.cacheable(cacheName, "", loader, ttl));
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Cacheable with negative TT L should throw exception.
     */
    @Test
    void cacheable_WithNegativeTTL_ShouldThrowException() {
        String cacheName = "testCache";
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(-1);

        assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
    }

    
    /**
     * Gets the TT L should return remaining seconds.
     *
     * @return the TT L should return remaining seconds
     */
    @Test
    void getTTL_ShouldReturnRemainingSeconds() {
        String cacheName = "users";
        String key = "user1";
        Long expectedTtl = 600L; // 10 minutes in seconds

        when(redisTemplate.getExpire("users:user1", TimeUnit.SECONDS)).thenReturn(expectedTtl);

        Long result = cacheService.getTTL(cacheName, key);

        assertEquals(expectedTtl, result);
        verify(redisTemplate).getExpire("users:user1", TimeUnit.SECONDS);
    }

    /**
     * Gets the TT L when key does not exist should return negative.
     *
     * @return the TT L when key does not exist should return negative
     */
    @Test
    void getTTL_WhenKeyDoesNotExist_ShouldReturnNegative() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.getExpire("users:user1", TimeUnit.SECONDS)).thenReturn(-2L);

        Long result = cacheService.getTTL(cacheName, key);

        assertEquals(-2L, result);
    }

    /**
     * Gets the TT L when redis throws exception should return null.
     *
     * @return the TT L when redis throws exception should return null
     */
    @Test
    void getTTL_WhenRedisThrowsException_ShouldReturnNull() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.getExpire("users:user1", TimeUnit.SECONDS))
                .thenThrow(new RuntimeException("Redis error"));

        Long result = cacheService.getTTL(cacheName, key);

        assertNull(result);
    }


    /**
     * Cacheable with cache name containing colon should throw exception.
     */
    @Test
    void cacheable_WithCacheNameContainingColon_ShouldThrowException() {
        String cacheName = "test:cache";
        String key = "key1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Cacheable with cache name containing asterisk should throw exception.
     */
    @Test
    void cacheable_WithCacheNameContainingAsterisk_ShouldThrowException() {
        String cacheName = "test*cache";
        String key = "key1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Cacheable with key containing asterisk should throw exception.
     */
    @Test
    void cacheable_WithKeyContainingAsterisk_ShouldThrowException() {
        String cacheName = "testcache";
        String key = "key*1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("key cannot contain '*' character", exception.getMessage());
    }

    /**
     * Cacheable with zero TT L should throw exception.
     */
    @Test
    void cacheable_WithZeroTTL_ShouldThrowException() {
        String cacheName = "testCache";
        String key = "testKey";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ZERO;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheService.cacheable(cacheName, key, loader, ttl));
        assertEquals("ttl must be positive", exception.getMessage());
    }


    /**
     * Cache evict multiple when redis throws exception should not throw exception.
     */
    @Test
    void cacheEvictMultiple_WhenRedisThrowsException_ShouldNotThrowException() {
        String cacheName = "users";
        String[] keys = { "user1", "user2", "user3" };

        when(redisTemplate.delete(anyList())).thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> cacheService.cacheEvictMultiple(cacheName, keys));
    }

    /**
     * Cache evict multiple with empty keys should call delete with empty list.
     */
    @Test
    void cacheEvictMultiple_WithEmptyKeys_ShouldCallDeleteWithEmptyList() {
        String cacheName = "users";
        String[] keys = {};

        when(redisTemplate.delete(anyList())).thenReturn(0L);

        cacheService.cacheEvictMultiple(cacheName, keys);

        verify(redisTemplate).delete(anyList());
    }
}
