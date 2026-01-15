package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.dto.EvictionEventDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    
    /** The Constant EVICT_CHANNEL. */
    private static final String EVICT_CHANNEL = "coalesce:evict";

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
                .collect(Collectors.toList());

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
            log.error("Error handling eviction event", e);
        }
    }
}
