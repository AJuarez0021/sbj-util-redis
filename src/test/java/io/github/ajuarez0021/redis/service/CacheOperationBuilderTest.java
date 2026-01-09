package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.dto.CacheResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheOperationBuilder.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
class CacheOperationBuilderTest {

        /** The cache service. */
        @Mock
        private RedisCacheService cacheService;

        /** The builder. */
        private CacheOperationBuilder<String> builder;

        /**
         * Sets the up.
         */
        @BeforeEach
        void setUp() {
                CacheOperationBuilder.Factory factory = new CacheOperationBuilder.Factory(cacheService);
                builder = factory.create();
        }

        /**
         * Cacheable without cache name should throw illegal state exception.
         */
        @Test
        void cacheable_WithoutCacheName_ShouldThrowIllegalStateException() {
                builder.key("testKey")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("cacheName is required", exception.getMessage());
        }

        /**
         * Cacheable without key should throw illegal state exception.
         */
        @Test
        void cacheable_WithoutKey_ShouldThrowIllegalStateException() {
                builder.cacheName("testCache")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("key is required", exception.getMessage());
        }

        /**
         * Cacheable without loader should throw illegal state exception.
         */
        @Test
        void cacheable_WithoutLoader_ShouldThrowIllegalStateException() {
                builder.cacheName("testCache")
                                .key("testKey");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("loader is required", exception.getMessage());
        }

        /**
         * Cacheable with empty cache name should throw illegal state exception.
         */
        @Test
        void cacheable_WithEmptyCacheName_ShouldThrowIllegalStateException() {
                builder.cacheName("")
                                .key("testKey")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("cacheName is required", exception.getMessage());
        }

        /**
         * Cacheable with empty key should throw illegal state exception.
         */
        @Test
        void cacheable_WithEmptyKey_ShouldThrowIllegalStateException() {
                builder.cacheName("testCache")
                                .key("")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("key is required", exception.getMessage());
        }

        /**
         * Cacheable with all required fields should execute.
         */
        @Test
        void cacheable_WithAllRequiredFields_ShouldExecute() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";
                String expectedValue = "testValue";
                Supplier<String> loader = () -> expectedValue;

                when(cacheService.cacheableWithResult(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(CacheResult.miss(expectedValue));

                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(loader)
                                .cacheable();

                assertEquals(expectedValue, result);
                verify(cacheService).cacheableWithResult(eq(cacheName), eq(key), any(),
                                eq(Duration.ofMinutes(10)));
        }

        /**
         * Cacheable with custom ttl should use custom ttl.
         */
        @Test
        void cacheable_WithCustomTtl_ShouldUseCustomTtl() {
                String cacheName = "testCache";
                String key = "testKey";
                String expectedValue = "testValue";
                Duration customTtl = Duration.ofHours(2);

                when(cacheService.cacheableWithResult(eq(cacheName), eq(key), any(), eq(customTtl)))
                                .thenReturn(CacheResult.miss(expectedValue));

                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> expectedValue)
                                .ttl(customTtl)
                                .cacheable();

                assertEquals(expectedValue, result);
                verify(cacheService).cacheableWithResult(eq(cacheName), eq(key), any(), eq(customTtl));
        }

        /**
         * Builder should support fluent API.
         */
        @Test
        void builder_ShouldSupportFluentAPI() {
                String expectedValue = "testValue";
                when(cacheService.cacheableWithResult(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(CacheResult.miss(expectedValue));

                String result = builder
                                .cacheName("cache")
                                .key("key")
                                .loader(() -> expectedValue)
                                .ttl(Duration.ofMinutes(5))
                                .condition(true)
                                .cacheable();

                assertEquals(expectedValue, result);
        }


        /**
         * Cacheable with condition false should bypass cache.
         */
        @Test
        void cacheable_WithConditionFalse_ShouldBypassCache() {
                String expectedValue = "testValue";
                Supplier<String> loader = () -> expectedValue;

                String result = builder.cacheName("testCache")
                                .key("testKey")
                                .loader(loader)
                                .condition(false)
                                .cacheable();

                assertEquals(expectedValue, result);
                verify(cacheService, never()).cacheable(anyString(), anyString(), any(), any(Duration.class));
        }

        /**
         * Cacheable with condition true should use cache.
         */
        @Test
        void cacheable_WithConditionTrue_ShouldUseCache() {
                String expectedValue = "testValue";
                when(cacheService.cacheableWithResult(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(CacheResult.miss(expectedValue));

                String result = builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .condition(true)
                                .cacheable();

                assertEquals(expectedValue, result);
                verify(cacheService).cacheableWithResult(anyString(), anyString(), any(), any(Duration.class));
        }



        /**
         * Cacheable on cache hit should invoke on hit callback.
         */
        @Test
        void cacheable_OnCacheHit_ShouldInvokeOnHitCallback() {
                String cacheName = "testCache";
                String key = "testKey";
                String cachedValue = "cachedValue";
                AtomicBoolean hitCallbackInvoked = new AtomicBoolean(false);
                Consumer<String> onHitCallback = value -> {
                        hitCallbackInvoked.set(true);
                        assertEquals(cachedValue, value);
                };

                when(cacheService.cacheableWithResult(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(CacheResult.hit(cachedValue));

                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> "newValue")
                                .onCacheHit(onHitCallback)
                                .cacheable();

                assertEquals(cachedValue, result);
                assertTrue(hitCallbackInvoked.get(), "onHit callback should have been invoked");
        }

        /**
         * Cacheable on cache miss should invoke on miss callback.
         */
        @Test
        void cacheable_OnCacheMiss_ShouldInvokeOnMissCallback() {
                String cacheName = "testCache";
                String key = "testKey";
                String loadedValue = "loadedValue";
                AtomicBoolean missCallbackInvoked = new AtomicBoolean(false);
                Consumer<String> onMissCallback = value -> {
                        missCallbackInvoked.set(true);
                        assertEquals(loadedValue, value);
                };

                when(cacheService.cacheableWithResult(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(CacheResult.miss(loadedValue));

                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> loadedValue)
                                .onCacheMiss(onMissCallback)
                                .cacheable();

                assertEquals(loadedValue, result);
                assertTrue(missCallbackInvoked.get(), "onMiss callback should have been invoked");
        }

        /**
         * Cacheable with both callbacks should invoke only relevant callback.
         */
        @Test
        void cacheable_WithBothCallbacks_ShouldInvokeOnlyRelevantCallback() {
                String cacheName = "testCache";
                String key = "testKey";
                String cachedValue = "cachedValue";
                AtomicBoolean hitCallbackInvoked = new AtomicBoolean(false);
                AtomicBoolean missCallbackInvoked = new AtomicBoolean(false);

                when(cacheService.cacheableWithResult(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(CacheResult.hit(cachedValue));

                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> "newValue")
                                .onCacheHit(value -> hitCallbackInvoked.set(true))
                                .onCacheMiss(value -> missCallbackInvoked.set(true))
                                .cacheable();

                assertEquals(cachedValue, result);
                assertTrue(hitCallbackInvoked.get(), "onHit callback should have been invoked");
                assertFalse(missCallbackInvoked.get(), "onMiss callback should NOT have been invoked");
        }

        /**
         * Cacheable without callbacks should not throw exception.
         */
        @Test
        void cacheable_WithoutCallbacks_ShouldNotThrowException() {
                String expectedValue = "testValue";
                when(cacheService.cacheableWithResult(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(CacheResult.miss(expectedValue));

                assertDoesNotThrow(() -> builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cacheable());
        }


        /**
         * Cache put should delegate to service.
         */
        @Test
        void cachePut_ShouldDelegateToService() {
                
                String cacheName = "testCache";
                String key = "testKey";
                String expectedValue = "testValue";
                Duration ttl = Duration.ofMinutes(10);

                when(cacheService.cachePut(eq(cacheName), eq(key), any(), eq(ttl)))
                                .thenReturn(expectedValue);

                
                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> expectedValue)
                                .ttl(ttl)
                                .cachePut();

                
                assertEquals(expectedValue, result);
                verify(cacheService).cachePut(eq(cacheName), eq(key), any(), eq(ttl));
        }

        /**
         * Cache put with default ttl should use default.
         */
        @Test
        void cachePut_WithDefaultTtl_ShouldUseDefault() {
                
                String expectedValue = "testValue";
                when(cacheService.cachePut(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(expectedValue);

                
                String result = builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cachePut();

                
                assertEquals(expectedValue, result);
                verify(cacheService).cachePut(anyString(), anyString(), any(), eq(Duration.ofMinutes(10)));
        }


        /**
         * Cache evict should delegate to service.
         */
        @Test
        void cacheEvict_ShouldDelegateToService() {
                String cacheName = "testCache";
                String key = "testKey";

                builder.cacheName(cacheName)
                                .key(key)
                                .cacheEvict();

                verify(cacheService).cacheEvict(cacheName, key);
        }

        /**
         * Cache evict with loader set should still work.
         */
        @Test
        void cacheEvict_WithLoaderSet_ShouldStillWork() {
                String cacheName = "testCache";
                String key = "testKey";

                builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> "value") // Loader not needed for evict
                                .cacheEvict();

                verify(cacheService).cacheEvict(cacheName, key);
        }

        /**
         * Builder should have default ttl of 10 minutes.
         */
        @Test
        void builder_ShouldHaveDefaultTtlOf10Minutes() {

                String expectedValue = "testValue";
                when(cacheService.cacheableWithResult(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(CacheResult.miss(expectedValue));

                builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cacheable();

                verify(cacheService).cacheableWithResult(anyString(), anyString(), any(),
                                eq(Duration.ofMinutes(10)));
        }

        /**
         * Builder should have default condition true.
         */
        @Test
        void builder_ShouldHaveDefaultConditionTrue() {
                String expectedValue = "testValue";
                when(cacheService.cacheableWithResult(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(CacheResult.miss(expectedValue));

                builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cacheable();

                verify(cacheService).cacheableWithResult(anyString(), anyString(), any(), any(Duration.class));
        }

        /**
         * Cache evict without cache name should throw exception.
         */
        @Test
        void cacheEvict_WithoutCacheName_ShouldThrowException() {
                IllegalStateException exception = assertThrows(IllegalStateException.class,
                        () -> builder.key("key1").cacheEvict());

                assertEquals("cacheName is required", exception.getMessage());
                verify(cacheService, never()).cacheEvict(anyString(), anyString());
        }

        /**
         * Cache evict without key should throw exception.
         */
        @Test
        void cacheEvict_WithoutKey_ShouldThrowException() {
                IllegalStateException exception = assertThrows(IllegalStateException.class,
                        () -> builder.cacheName("cache").cacheEvict());

                assertEquals("key is required", exception.getMessage());
                verify(cacheService, never()).cacheEvict(anyString(), anyString());
        }

        /**
         * Cache evict with valid params should succeed.
         */
        @Test
        void cacheEvict_WithValidParams_ShouldSucceed() {
                assertDoesNotThrow(() -> builder.cacheName("cache").key("key1").cacheEvict());

                verify(cacheService).cacheEvict("cache", "key1");
        }
}
