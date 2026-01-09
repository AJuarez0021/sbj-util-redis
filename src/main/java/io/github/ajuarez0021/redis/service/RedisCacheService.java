package io.github.ajuarez0021.redis.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.github.ajuarez0021.redis.dto.CacheResult;
import io.github.ajuarez0021.redis.util.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

/**
 * The Class RedisCacheService.
 *
 * @author ajuar
 */
@Slf4j
public class RedisCacheService {

    /**
     * The redis template.
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Instantiates a new redis cache service.
     *
     * @param redisTemplate the redis template
     */
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Equivalent to @Cacheable Searches the cache, if it does not exist,
     * execute the loader and save the result.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @param ttl the ttl
     * @return the object
     */
    public <T> T cacheable(String cacheName, String key, Supplier<T> loader, Duration ttl) {
        return cacheableWithResult(cacheName, key, loader, ttl).getValue();
    }


    
    /**
     * Overhead with default TTL of 10 minutes.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @return the object
     */
    public <T> T cacheable(String cacheName, String key, Supplier<T> loader) {
        return cacheable(cacheName, key, loader, Duration.ofMinutes(10));
    }

    /**
     * Cacheable operation that returns detailed result including cache hit/miss information.
     *
     * <p>This method provides the same functionality as {@link #cacheable(String, String, Supplier, Duration)}
     * but includes metadata about whether the value came from cache (hit) or was loaded from the source (miss).</p>
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader function to execute on cache miss
     * @param ttl the time to live
     * @return CacheResult containing the value and hit/miss information
     */
    @SuppressWarnings("unchecked")
    public <T> CacheResult<T> cacheableWithResult(String cacheName, String key,
                                                    Supplier<T> loader, Duration ttl) {
        Validator.validateCacheable(cacheName, key, loader, ttl);

        String fullKey = buildKey(cacheName, key);
        T result = null;
        boolean wasLoaded = false;
        try {
            Object cached = redisTemplate.opsForValue().get(fullKey);

            if (cached != null) {
                log.debug("Cache HIT - Key: {}", fullKey);
                return CacheResult.hit((T) cached);
            }

            result = loader.get();
            wasLoaded = true;

            if (result != null) {
                redisTemplate.opsForValue().set(fullKey, result, ttl);
                log.debug("Cached data - Key: {}", fullKey);
            }

            return CacheResult.miss(result);

        } catch (Exception e) {
            log.error("Error in cacheable operation for key {}: {}", fullKey, e.getMessage());
            if (!wasLoaded) {
                result = loader.get();
            }
            return CacheResult.miss(result);
        }
    }

    /**
     * Cacheable operation with result and default TTL of 10 minutes.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @return CacheResult containing the value and hit/miss information
     */
    public <T> CacheResult<T> cacheableWithResult(String cacheName, String key, Supplier<T> loader) {
        return cacheableWithResult(cacheName, key, loader, Duration.ofMinutes(10));
    }

    /**
     * Equivalent to @CachePut.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @param ttl the ttl
     * @return the object
     */
    public <T> T cachePut(String cacheName, String key, Supplier<T> loader, Duration ttl) {
        Validator.validateCacheable(cacheName, key, loader, ttl);

        String fullKey = buildKey(cacheName, key);
        boolean wasLoaded = false;
        T result = null;
        try {
            result = loader.get();
            wasLoaded = true;

            if (result != null) {
                redisTemplate.opsForValue().set(fullKey, result, ttl);
                log.debug("Cache UPDATED - Key: {}", fullKey);
            } else {
                redisTemplate.delete(fullKey);
                log.debug("Cache EVICTED (null result) - Key: {}", fullKey);
            }

            return result;

        } catch (Exception e) {
            log.error("Error in cachePut operation for key {}: {}", fullKey, e.getMessage());
            if (!wasLoaded) {
                result = loader.get();
            }
            return result;
        }
    }

    /**
     * Overloading with TTL by default.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @return the object
     */
    public <T> T cachePut(String cacheName, String key, Supplier<T> loader) {
        return cachePut(cacheName, key, loader, Duration.ofMinutes(10));
    }

    /**
     * Equivalent to @CacheEvict.
     *
     * @param cacheName the cache name
     * @param key the key
     */
    public void cacheEvict(String cacheName, String key) {
        Validator.validateCacheEvict(cacheName, key);

        String fullKey = buildKey(cacheName, key);

        try {
            Boolean deleted = redisTemplate.delete(fullKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Cache EVICTED - Key: {}", fullKey);
            } else {
                log.debug("Cache key not found - Key: {}", fullKey);
            }
        } catch (Exception e) {
            log.error("Error evicting cache key {}: {}", fullKey, e.getMessage());
        }
    }

    /**
     * Equivalent to @CacheEvict(allEntries = true).
     *
     * <p>This method uses SCAN to iterate through keys and deletes them in batches
     * during the scan to avoid loading all keys into memory at once.</p>
     *
     * @param cacheName the cache name
     */
    public void cacheEvictAll(String cacheName) {
        Validator.validateCacheEvictAll(cacheName);

        String pattern = cacheName + ":*";
        try {
            AtomicLong deletedCount = new AtomicLong(0);

            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build();

                List<byte[]> batch = new ArrayList<>();
                try(Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        batch.add(cursor.next());

                        if (batch.size() >= 100) {
                            Long deleted = connection.keyCommands().del(
                                batch.toArray(new byte[0][])
                            );
                            deletedCount.addAndGet(deleted != null ? deleted : 0);
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        Long deleted = connection.keyCommands().del(
                            batch.toArray(new byte[0][])
                        );
                        deletedCount.addAndGet(deleted != null ? deleted : 0);
                    }
                }
                return null;
            });

            log.debug("Cache EVICTED ALL - Pattern: {}, Count: {}", pattern, deletedCount.get());
        } catch (Exception e) {
            log.error("Error evicting all entries for cache {}: {}", cacheName, e.getMessage());
        }
    }

    /**
     * Evict multiple specific keys.
     *
     * @param cacheName the cache name
     * @param keys the keys
     */
    public void cacheEvictMultiple(String cacheName, String... keys) {
        Validator.validateCacheEvictMultiple(cacheName);

        if (keys == null || keys.length == 0) {
            log.debug("No keys to evict");
            return;
        }

        if (Arrays.stream(keys).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("keys array cannot contain null elements");
        }

        List<String> fullKeys = Arrays.stream(keys)
                .map(key -> buildKey(cacheName, key))
                .toList();

        try {
            Long deleted = redisTemplate.delete(fullKeys);
            log.debug("Cache EVICTED MULTIPLE - Count: {}", deleted);
        } catch (Exception e) {
            log.error("Error evicting multiple keys: {}", e.getMessage());
        }
    }

    /**
     * Evict by custom pattern.
     *
     * <p>This method uses SCAN to iterate through keys and deletes them in batches
     * during the scan to avoid loading all keys into memory at once.</p>
     *
     * @param pattern the pattern
     */
    public void cacheEvictByPattern(String pattern) {
        Validator.validatePattern(pattern);

        try {
            AtomicLong deletedCount = new AtomicLong(0);

            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build();

                List<byte[]> batch = new ArrayList<>();
                try(Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        batch.add(cursor.next());

                        if (batch.size() >= 100) {
                            Long deleted = connection.keyCommands().del(
                                batch.toArray(new byte[0][])
                            );
                            deletedCount.addAndGet(deleted != null ? deleted : 0);
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        Long deleted = connection.keyCommands().del(
                            batch.toArray(new byte[0][])
                        );
                        deletedCount.addAndGet(deleted != null ? deleted : 0);
                    }
                }
                return null;
            });

            log.debug("Cache EVICTED BY PATTERN - Pattern: {}, Count: {}", pattern, deletedCount.get());
        } catch (Exception e) {
            log.error("Error evicting by pattern {}: {}", pattern, e.getMessage());
        }
    }

    /**
     * Check if a cached key exists.
     *
     * @param cacheName the cache name
     * @param key the key
     * @return true, if successful
     */
    public boolean exists(String cacheName, String key) {
        Validator.validateCacheEvict(cacheName, key);

        String fullKey = buildKey(cacheName, key);

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
        } catch (Exception e) {
            log.error("Error checking key existence {}: {}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * Gets the remaining TTL of a key.
     *
     * @param cacheName the cache name
     * @param key the key
     * @return the ttl
     */
    public Long getTTL(String cacheName, String key) {
        Validator.validateCacheEvict(cacheName, key);

        String fullKey = buildKey(cacheName, key);

        try {
            return redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting TTL for key {}: {}", fullKey, e.getMessage());
            return null;
        }
    }

    /**
     * Build the complete key.
     *
     * @param cacheName the cache name
     * @param key the key
     * @return the string
     */
    private String buildKey(String cacheName, String key) {
        return cacheName + ":" + key;
    }
}
