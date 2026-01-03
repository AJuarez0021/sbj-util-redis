package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.util.Validator;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder for cache operations with fluent API.
 *
 * <p><b>Thread Safety:</b> This class is NOT thread-safe and should not be
 * shared across threads. Each builder instance is designed for single-threaded
 * use. Use {@link Factory#create()} to obtain a new builder instance for each
 * cache operation.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * @Autowired
 * private CacheOperationBuilder.Factory builderFactory;
 *
 * public User getUser(String userId) {
 *     // Get a new builder instance for each operation
 *     CacheOperationBuilder<User> builder = builderFactory.create();
 *     return builder
 *         .cacheName("users")
 *         .key(userId)
 *         .loader(() -> userRepository.findById(userId))
 *         .ttl(Duration.ofMinutes(30))
 *         .cacheable();
 * }
 * }</pre>
 *
 * @author ajuar
 * @param <T> the generic type
 * @see Factory
 */
public final class CacheOperationBuilder<T> {

    /** The cache service. */
    private final RedisCacheService cacheService;

    /** The cache name. */
    private String cacheName;

    /** The key. */
    private String key;

    /** The loader. */
    private Supplier<T> loader;

    /** The ttl. */
    private Duration ttl = Duration.ofMinutes(10);

    /** The condition. */
    private boolean condition = true;

    /** The on hit. */
    private Consumer<T> onHit;

    /** The on miss. */
    private Consumer<T> onMiss;

    /**
     * Private constructor - use {@link Factory#create()} to create instances.
     *
     * @param cacheService the cache service
     */
    private CacheOperationBuilder(RedisCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Cache name.
     *
     * @param cacheName the cache name
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> cacheName(String cacheName) {
        this.cacheName = cacheName;
        return this;
    }

    /**
     * Key.
     *
     * @param key the key
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Loader.
     *
     * @param loader the loader
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> loader(Supplier<T> loader) {
        this.loader = loader;
        return this;
    }

    /**
     * Ttl.
     *
     * @param ttl the ttl
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> ttl(Duration ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * Condition.
     *
     * @param condition the condition
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> condition(boolean condition) {
        this.condition = condition;
        return this;
    }

    /**
     * On cache hit.
     *
     * @param callback the callback
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> onCacheHit(Consumer<T> callback) {
        this.onHit = callback;
        return this;
    }

    /**
     * On cache miss.
     *
     * @param callback the callback
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> onCacheMiss(Consumer<T> callback) {
        this.onMiss = callback;
        return this;
    }

    /**
     * Cacheable.
     *
     * @return the t
     */
    public T cacheable() {
        Validator.validateRequiredFields(cacheName, key, loader);

        if (!condition) {
            return loader.get();
        }

        T result = cacheService.cacheable(cacheName, key, loader, ttl);

        if (cacheService.exists(cacheName, key) && onHit != null) {
            onHit.accept(result);
        } else if (onMiss != null) {
            onMiss.accept(result);
        }

        return result;
    }


    /**
     * Cache put.
     *
     * @return the t
     */
    public T cachePut() {
        return cacheService.cachePut(cacheName, key, loader, ttl);
    }

    /**
     * Cache evict.
     */
    public void cacheEvict() {
        cacheService.cacheEvict(cacheName, key);
    }

    /**
     * Factory for creating {@link CacheOperationBuilder} instances.
     *
     * <p>This factory provides a thread-safe way to obtain new builder instances.
     * The factory itself is a singleton bean and can be safely injected and shared
     * across threads. Each call to {@link #create()} returns a new, independent
     * builder instance.</p>
     *
     * <p><b>Thread Safety:</b> This class is fully thread-safe. Multiple threads
     * can safely call {@link #create()} concurrently.</p>
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * @Service
     * public class UserService {
     *     @Autowired
     *     private CacheOperationBuilder.Factory builderFactory;
     *
     *     public User getUser(String userId) {
     *         return builderFactory.create()
     *             .cacheName("users")
     *             .key(userId)
     *             .loader(() -> userRepository.findById(userId))
     *             .cacheable();
     *     }
     * }
     * }</pre>
     *
     * @author ajuar
     */
    public static class Factory {

        /** The cache service. */
        private final RedisCacheService cacheService;

        /**
         * Instantiates a new cache operation builder factory.
         *
         * @param cacheService the cache service
         */
        public Factory(RedisCacheService cacheService) {
            this.cacheService = cacheService;
        }

        /**
         * Creates a new {@link CacheOperationBuilder} instance.
         *
         * <p>Each invocation returns a new, independent builder instance that
         * should be used for a single cache operation and then discarded.</p>
         *
         * @param <T> the type of cached value
         * @return a new cache operation builder
         */
        public <T> CacheOperationBuilder<T> create() {
            return new CacheOperationBuilder<>(cacheService);
        }
    }
}
