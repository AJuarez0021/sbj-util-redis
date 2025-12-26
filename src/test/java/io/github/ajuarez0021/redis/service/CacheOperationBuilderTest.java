package io.github.ajuarez0021.redis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

        @Mock
        private RedisCacheService cacheService;

        private CacheOperationBuilder<String> builder;

        @BeforeEach
        void setUp() {
                // Create builder using Factory
                CacheOperationBuilder.Factory factory = new CacheOperationBuilder.Factory(cacheService);
                builder = factory.create();
        }

        @Test
        void cacheable_WithoutCacheName_ShouldThrowIllegalStateException() {
                builder.key("testKey")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("cacheName is required", exception.getMessage());
        }

        @Test
        void cacheable_WithoutKey_ShouldThrowIllegalStateException() {
                builder.cacheName("testCache")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("key is required", exception.getMessage());
        }

        @Test
        void cacheable_WithoutLoader_ShouldThrowIllegalStateException() {
                builder.cacheName("testCache")
                                .key("testKey");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("loader is required", exception.getMessage());
        }

        @Test
        void cacheable_WithEmptyCacheName_ShouldThrowIllegalStateException() {
                builder.cacheName("")
                                .key("testKey")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("cacheName is required", exception.getMessage());
        }

        @Test
        void cacheable_WithEmptyKey_ShouldThrowIllegalStateException() {
                builder.cacheName("testCache")
                                .key("")
                                .loader(() -> "value");

                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.cacheable());
                assertEquals("key is required", exception.getMessage());
        }

        @Test
        void cacheable_WithAllRequiredFields_ShouldExecute() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";
                String expectedValue = "testValue";
                Supplier<String> loader = () -> expectedValue;

                when(cacheService.cacheable(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(expectedValue);
                when(cacheService.exists(cacheName, key)).thenReturn(false);

                // When
                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(loader)
                                .cacheable();

                // Then
                assertEquals(expectedValue, result);
                verify(cacheService).cacheable(eq(cacheName), eq(key), any(), eq(Duration.ofMinutes(10)));
        }

        @Test
        void cacheable_WithCustomTtl_ShouldUseCustomTtl() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";
                String expectedValue = "testValue";
                Duration customTtl = Duration.ofHours(2);

                when(cacheService.cacheable(eq(cacheName), eq(key), any(), eq(customTtl)))
                                .thenReturn(expectedValue);
                when(cacheService.exists(cacheName, key)).thenReturn(false);

                // When
                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> expectedValue)
                                .ttl(customTtl)
                                .cacheable();

                // Then
                assertEquals(expectedValue, result);
                verify(cacheService).cacheable(eq(cacheName), eq(key), any(), eq(customTtl));
        }

        @Test
        void builder_ShouldSupportFluentAPI() {
                // Given
                String expectedValue = "testValue";
                when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(expectedValue);
                when(cacheService.exists(anyString(), anyString())).thenReturn(false);

                // When
                String result = builder
                                .cacheName("cache")
                                .key("key")
                                .loader(() -> expectedValue)
                                .ttl(Duration.ofMinutes(5))
                                .condition(true)
                                .cacheable();

                // Then
                assertEquals(expectedValue, result);
        }


        @Test
        void cacheable_WithConditionFalse_ShouldBypassCache() {
                // Given
                String expectedValue = "testValue";
                Supplier<String> loader = () -> expectedValue;

                // When
                String result = builder.cacheName("testCache")
                                .key("testKey")
                                .loader(loader)
                                .condition(false)
                                .cacheable();

                // Then
                assertEquals(expectedValue, result);
                verify(cacheService, never()).cacheable(anyString(), anyString(), any(), any(Duration.class));
        }

        @Test
        void cacheable_WithConditionTrue_ShouldUseCache() {
                String expectedValue = "testValue";
                when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(expectedValue);
                when(cacheService.exists(anyString(), anyString())).thenReturn(false);

                String result = builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .condition(true)
                                .cacheable();

                assertEquals(expectedValue, result);
                verify(cacheService).cacheable(anyString(), anyString(), any(), any(Duration.class));
        }



        @Test
        void cacheable_OnCacheHit_ShouldInvokeOnHitCallback() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";
                String cachedValue = "cachedValue";
                AtomicBoolean hitCallbackInvoked = new AtomicBoolean(false);
                Consumer<String> onHitCallback = value -> {
                        hitCallbackInvoked.set(true);
                        assertEquals(cachedValue, value);
                };

                when(cacheService.cacheable(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(cachedValue);
                when(cacheService.exists(cacheName, key)).thenReturn(true);

                // When
                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> "newValue")
                                .onCacheHit(onHitCallback)
                                .cacheable();

                // Then
                assertEquals(cachedValue, result);
                assertTrue(hitCallbackInvoked.get(), "onHit callback should have been invoked");
        }

        @Test
        void cacheable_OnCacheMiss_ShouldInvokeOnMissCallback() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";
                String loadedValue = "loadedValue";
                AtomicBoolean missCallbackInvoked = new AtomicBoolean(false);
                Consumer<String> onMissCallback = value -> {
                        missCallbackInvoked.set(true);
                        assertEquals(loadedValue, value);
                };

                when(cacheService.cacheable(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(loadedValue);
                when(cacheService.exists(cacheName, key)).thenReturn(false);

                // When
                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> loadedValue)
                                .onCacheMiss(onMissCallback)
                                .cacheable();

                // Then
                assertEquals(loadedValue, result);
                assertTrue(missCallbackInvoked.get(), "onMiss callback should have been invoked");
        }

        @Test
        void cacheable_WithBothCallbacks_ShouldInvokeOnlyRelevantCallback() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";
                String cachedValue = "cachedValue";
                AtomicBoolean hitCallbackInvoked = new AtomicBoolean(false);
                AtomicBoolean missCallbackInvoked = new AtomicBoolean(false);

                when(cacheService.cacheable(eq(cacheName), eq(key), any(), any(Duration.class)))
                                .thenReturn(cachedValue);
                when(cacheService.exists(cacheName, key)).thenReturn(true);

                // When
                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> "newValue")
                                .onCacheHit(value -> hitCallbackInvoked.set(true))
                                .onCacheMiss(value -> missCallbackInvoked.set(true))
                                .cacheable();

                // Then
                assertEquals(cachedValue, result);
                assertTrue(hitCallbackInvoked.get(), "onHit callback should have been invoked");
                assertFalse(missCallbackInvoked.get(), "onMiss callback should NOT have been invoked");
        }

        @Test
        void cacheable_WithoutCallbacks_ShouldNotThrowException() {
                // Given
                String expectedValue = "testValue";
                when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(expectedValue);
                when(cacheService.exists(anyString(), anyString())).thenReturn(false);

                // When & Then
                assertDoesNotThrow(() -> builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cacheable());
        }

        // ========== Cache Put Tests ==========

        @Test
        void cachePut_ShouldDelegateToService() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";
                String expectedValue = "testValue";
                Duration ttl = Duration.ofMinutes(10);

                when(cacheService.cachePut(eq(cacheName), eq(key), any(), eq(ttl)))
                                .thenReturn(expectedValue);

                // When
                String result = builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> expectedValue)
                                .ttl(ttl)
                                .cachePut();

                // Then
                assertEquals(expectedValue, result);
                verify(cacheService).cachePut(eq(cacheName), eq(key), any(), eq(ttl));
        }

        @Test
        void cachePut_WithDefaultTtl_ShouldUseDefault() {
                // Given
                String expectedValue = "testValue";
                when(cacheService.cachePut(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(expectedValue);

                // When
                String result = builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cachePut();

                // Then
                assertEquals(expectedValue, result);
                verify(cacheService).cachePut(anyString(), anyString(), any(), eq(Duration.ofMinutes(10)));
        }

        // ========== Cache Evict Tests ==========

        @Test
        void cacheEvict_ShouldDelegateToService() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";

                // When
                builder.cacheName(cacheName)
                                .key(key)
                                .cacheEvict();

                // Then
                verify(cacheService).cacheEvict(cacheName, key);
        }

        @Test
        void cacheEvict_WithLoaderSet_ShouldStillWork() {
                // Given
                String cacheName = "testCache";
                String key = "testKey";

                // When
                builder.cacheName(cacheName)
                                .key(key)
                                .loader(() -> "value") // Loader not needed for evict
                                .cacheEvict();

                // Then
                verify(cacheService).cacheEvict(cacheName, key);
        }

        // ========== Default Values Tests ==========

        @Test
        void builder_ShouldHaveDefaultTtlOf10Minutes() {
                // Given
                String expectedValue = "testValue";
                when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(expectedValue);
                when(cacheService.exists(anyString(), anyString())).thenReturn(false);

                // When
                builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cacheable();

                // Then
                verify(cacheService).cacheable(anyString(), anyString(), any(), eq(Duration.ofMinutes(10)));
        }

        @Test
        void builder_ShouldHaveDefaultConditionTrue() {
                // Given
                String expectedValue = "testValue";
                when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                                .thenReturn(expectedValue);
                when(cacheService.exists(anyString(), anyString())).thenReturn(false);

                // When
                builder.cacheName("testCache")
                                .key("testKey")
                                .loader(() -> expectedValue)
                                .cacheable();

                // Then
                verify(cacheService).cacheable(anyString(), anyString(), any(), any(Duration.class));
        }
}
