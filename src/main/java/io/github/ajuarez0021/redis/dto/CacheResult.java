package io.github.ajuarez0021.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Result wrapper for cache operations that includes metadata about cache hit/miss.
 *
 * <p>This class is used to safely determine whether a value came from cache (hit)
 * or from the data source (miss) without race conditions.</p>
 *
 * @param <T> the type of the cached value
 * @author ajuar
 */
@Getter
@ToString
@AllArgsConstructor
public class CacheResult<T> {

    /** The cached or loaded value. */
    private final T value;

    /** Indicates whether this was a cache hit (true) or miss (false). */
    private final boolean cacheHit;


    /**
     * Checks if this was a cache miss.
     *
     * @return true if the value was loaded from source, false if came from cache
     */
    public boolean isCacheMiss() {
        return !cacheHit;
    }

    /**
     * Creates a cache hit result.
     *
     * @param <T> the value type
     * @param value the cached value
     * @return a CacheResult indicating a cache hit
     */
    public static <T> CacheResult<T> hit(T value) {
        return new CacheResult<>(value, true);
    }

    /**
     * Creates a cache miss result.
     *
     * @param <T> the value type
     * @param value the loaded value
     * @return a CacheResult indicating a cache miss
     */
    public static <T> CacheResult<T> miss(T value) {
        return new CacheResult<>(value, false);
    }


}
