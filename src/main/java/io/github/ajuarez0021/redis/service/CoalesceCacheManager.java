package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.dto.CoalescedResponseDto;
import io.github.ajuarez0021.redis.dto.EvictionEventDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.github.ajuarez0021.redis.exception.CoalesceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * The Class CoalesceCacheManager.
 *
 * @author ajuar
 */
@Slf4j
public class CoalesceCacheManager {

    /** The redis template. */
    private final RedisTemplate<String, Object> redisTemplate;

    /** The Constant CACHE_PREFIX. */
    private static final String CACHE_PREFIX = "coalesce:cache:";

    /** The Constant LOCK_PREFIX. */
    private static final String LOCK_PREFIX = "coalesce:lock:";

    /** The Constant EVICT_CHANNEL. */
    private static final String EVICT_CHANNEL = "coalesce:evict";

    /** The Constant COALESCE_CHANNEL. */
    private static final String COALESCE_CHANNEL = "coalesce:ready";

    /** The pending requests map for local waiting. */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingRequests =
            new ConcurrentHashMap<>();

    /**
     * Instantiates a new coalesce cache manager.
     *
     * @param redisTemplate the redis template
     * @param listenerContainer the listener container
     */
    public CoalesceCacheManager(
            RedisTemplate<String, Object> redisTemplate,
            RedisMessageListenerContainer listenerContainer) {
        this.redisTemplate = redisTemplate;

        listenerContainer.addMessageListener(
                this::handleEvictionEvent,
                new ChannelTopic(EVICT_CHANNEL)
        );

        listenerContainer.addMessageListener(
                this::handleCoalesceEvent,
                new ChannelTopic(COALESCE_CHANNEL)
        );
    }

    /**
     * Put.
     *
     * @param key the key
     * @param value the value
     * @param ttlSeconds the ttl seconds
     */
    public void put(String key, Object value, long ttlSeconds) {
        String fullKey = CACHE_PREFIX + key;
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(
                    fullKey,
                    value,
                    Duration.ofSeconds(ttlSeconds)
            );
        } else {
            redisTemplate.opsForValue().set(fullKey, value);
        }
        log.debug("Cached value for key: {}", fullKey);
    }

    /**
     * Gets the.
     *
     * @param key the key
     * @return the optional
     */
    public Optional<Object> get(String key) {
        String fullKey = CACHE_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(fullKey);
        if (value != null) {
            log.debug("Cache hit for key: {}", fullKey);
        } else {
            log.debug("Cache miss for key: {}", fullKey);
        }
        return Optional.ofNullable(value);
    }

    /**
     * Evict.
     *
     * @param key the key
     */
    public void evict(String key) {
        String fullKey = CACHE_PREFIX + key;
        redisTemplate.delete(fullKey);
        log.info("Evicted cache key: {}", fullKey);

        publishEviction(fullKey);
    }

    /**
     * Evict all.
     *
     * @param prefix the prefix
     */
    public void evictAll(String prefix) {
        String pattern = CACHE_PREFIX + prefix + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} keys matching pattern: {}", keys.size(), pattern);

            keys.forEach(this::publishEviction);
        }
    }

    /**
     * Evict pattern.
     *
     * @param pattern the pattern
     */
    public void evictPattern(String pattern) {
        String fullPattern = CACHE_PREFIX + pattern;
        Set<String> keys = redisTemplate.keys(fullPattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} keys matching pattern: {}", keys.size(), fullPattern);

            keys.forEach(this::publishEviction);
        }
    }

    /**
     * Evict multiple.
     *
     * @param keys the keys
     */
    public void evictMultiple(Collection<String> keys) {
        List<String> fullKeys = keys.stream()
                .map(key -> CACHE_PREFIX + key)
                .toList();

        if (!fullKeys.isEmpty()) {
            redisTemplate.delete(fullKeys);
            log.info("Evicted {} cache keys", fullKeys.size());

            fullKeys.forEach(this::publishEviction);
        }
    }

    /**
     * Exists.
     *
     * @param key the key
     * @return true, if successful
     */
    public boolean exists(String key) {
        String fullKey = CACHE_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    /**
     * Clear.
     */
    public void clear() {
        String pattern = CACHE_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.warn("Cleared all cache entries: {} keys", keys.size());
        }
    }

    /**
     * Gets the time to live.
     *
     * @param key the key
     * @return the time to live
     */
    public long getTimeToLive(String key) {
        String fullKey = CACHE_PREFIX + key;
        Long ttl = redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }

    /**
     * Expire.
     *
     * @param key the key
     * @param ttlSeconds the ttl seconds
     */
    public void expire(String key, long ttlSeconds) {
        String fullKey = CACHE_PREFIX + key;
        redisTemplate.expire(fullKey, Duration.ofSeconds(ttlSeconds));
        log.debug("Set expiration for key: {} to {} seconds", fullKey, ttlSeconds);
    }

    /**
     * Gets or loads a value with request coalescing using Redis pub/sub.
     * Only one instance will execute the loader, others wait for the result.
     *
     * @param key the cache key
     * @param loader the supplier to load the value if not cached
     * @param ttlSeconds the TTL in seconds
     * @param cacheNull whether to cache null results
     * @return the cached or loaded value
     */
    public Object getOrLoad(String key, Supplier<Object> loader, long ttlSeconds, boolean cacheNull) {
        String fullKey = CACHE_PREFIX + key;
        String lockKey = LOCK_PREFIX + key;
        String requestId = UUID.randomUUID().toString();

        Object cached = redisTemplate.opsForValue().get(fullKey);
        if (cached != null) {
            log.debug("Cache hit for coalesced key: {}", fullKey);
            return cached;
        }

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                requestId,
                Duration.ofSeconds(30)
        );

        if (Boolean.TRUE.equals(acquired)) {

            log.debug("Acquired lock for key: {}, executing loader", fullKey);
            try {
                Object result = loader.get();


                if (result != null || cacheNull) {
                    put(key, result, ttlSeconds);
                }

                publishCoalesceResult(key, result, null);

                return result;
            } catch (Exception e) {
                publishCoalesceResult(key, null, e);
                throw e;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            log.debug("Lock not acquired for key: {}, waiting for result", fullKey);
            return waitForResult(key, ttlSeconds);
        }
    }

    /**
     * Waits for a coalesced result from pub/sub.
     *
     * @param key the cache key
     * @param timeoutSeconds the maximum time to wait
     * @return the loaded value
     */
    private Object waitForResult(String key, long timeoutSeconds) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingRequests.put(key, future);

        try {
            long timeout = timeoutSeconds > 0 ? timeoutSeconds : 30;
            return future.get(timeout, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoalesceException("Failed to get coalesced result for key: " + key, e);
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Timeout or error waiting for coalesced result for key {} {}", key, e.getMessage());
            String fullKey = CACHE_PREFIX + key;
            Object cached = redisTemplate.opsForValue().get(fullKey);

            if (cached != null) {
                return cached;
            }
            throw new CoalesceException("Failed to get coalesced result for key: " + key, e);
        } finally {
            pendingRequests.remove(key);
        }
    }

    /**
     * Publishes the coalesced result to all waiting subscribers.
     *
     * @param key the cache key
     * @param result the result
     * @param error the error if any
     */
    private void publishCoalesceResult(String key, Object result, Throwable error) {
        CoalescedResponseDto response = new CoalescedResponseDto(
                UUID.randomUUID().toString(),
                key,
                result,
                error,
                LocalDateTime.now()
        );
        redisTemplate.convertAndSend(COALESCE_CHANNEL, response);
        log.debug("Published coalesce result for key: {}", key);
    }

    /**
     * Handles coalesce events from pub/sub.
     *
     * @param message the message
     * @param pattern the pattern
     */
    private void handleCoalesceEvent(Message message, byte[] pattern) {
        try {
            CoalescedResponseDto response = (CoalescedResponseDto) redisTemplate
                    .getValueSerializer().deserialize(message.getBody());

            if (response != null) {
                String key = response.getCoalescingKey();
                CompletableFuture<Object> future = pendingRequests.get(key);

                if (future != null) {
                    if (response.getError() != null) {
                        future.completeExceptionally(response.getError());
                    } else {
                        future.complete(response.getResult());
                    }
                    log.debug("Completed pending request for key: {}", key);
                }
            }
        } catch (SerializationException e) {
            log.error("Error handling coalesce event {}", e.getMessage());
        }
    }

    /**
     * Publish eviction.
     *
     * @param key the key
     */
    private void publishEviction(String key) {
        EvictionEventDto event = new EvictionEventDto(key, LocalDateTime.now());
        redisTemplate.convertAndSend(EVICT_CHANNEL, event);
    }

    /**
     * Handle eviction event.
     *
     * @param message the message
     * @param pattern the pattern
     */
    private void handleEvictionEvent(Message message, byte[] pattern) {
        try {
            EvictionEventDto event = (EvictionEventDto) redisTemplate
                    .getValueSerializer().deserialize(message.getBody());

            if (event != null) {
                log.debug("Received eviction event for key: {}", event.getKey());
            }
        } catch (SerializationException e) {
            log.error("Error handling eviction event {}", e.getMessage());
        }
    }
}
