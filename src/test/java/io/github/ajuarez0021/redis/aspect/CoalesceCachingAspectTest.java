package io.github.ajuarez0021.redis.aspect;

import io.github.ajuarez0021.redis.annotation.CoalesceCacheable;
import io.github.ajuarez0021.redis.annotation.CoalesceCaching;
import io.github.ajuarez0021.redis.annotation.CoalesceEvict;
import io.github.ajuarez0021.redis.annotation.CoalescePut;
import io.github.ajuarez0021.redis.service.CoalesceCacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for CoalesceCachingAspect.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoalesceCachingAspectTest {

    /** The cache manager. */
    @Mock
    private CoalesceCacheManager cacheManager;

    /** The join point. */
    @Mock
    private ProceedingJoinPoint joinPoint;

    /** The method signature. */
    @Mock
    private MethodSignature methodSignature;

    /** The aspect. */
    private CoalesceCachingAspect aspect;

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        aspect = new CoalesceCachingAspect(cacheManager);
    }

    /**
     * Test method for mocking.
     *
     * @param id the id
     * @return the string
     */
    public String testMethod(String id) {
        return "result";
    }

    /**
     * Handle caching with cache hit should return cached value.
     */
    @Test
    void handleCaching_WithCacheHit_ShouldReturnCachedValue() throws Throwable {
        CoalesceCacheable cacheable = createMockCacheable("cache", "#id", 300, "", false, true);
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{cacheable},
                new CoalesceEvict[]{},
                new CoalescePut[]{}
        );

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(cacheManager.get("cache:123")).thenReturn(Optional.of("cachedValue"));

        Object result = aspect.handleCaching(joinPoint, caching);

        assertEquals("cachedValue", result);
        verify(cacheManager).get("cache:123");
        verify(joinPoint, never()).proceed();
    }

    /**
     * Handle caching with cache miss should execute method and cache result.
     */
    @Test
    void handleCaching_WithCacheMiss_ShouldExecuteMethodAndCacheResult() throws Throwable {
        CoalesceCacheable cacheable = createMockCacheable("cache", "#id", 300, "", false, true);
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{cacheable},
                new CoalesceEvict[]{},
                new CoalescePut[]{}
        );

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(cacheManager.get("cache:123")).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn("newValue");

        Object result = aspect.handleCaching(joinPoint, caching);

        assertEquals("newValue", result);
        verify(joinPoint).proceed();
        verify(cacheManager).put("cache:123", "newValue", 300);
    }

    /**
     * Handle caching with null result and cache null false should not cache.
     */
    @Test
    void handleCaching_WithNullResultAndCacheNullFalse_ShouldNotCache() throws Throwable {
        CoalesceCacheable cacheable = createMockCacheable("cache", "#id", 300, "", false, true);
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{cacheable},
                new CoalesceEvict[]{},
                new CoalescePut[]{}
        );

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(cacheManager.get("cache:123")).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn(null);

        Object result = aspect.handleCaching(joinPoint, caching);

        assertNull(result);
        verify(cacheManager, never()).put(anyString(), any(), anyLong());
    }

    /**
     * Handle caching with null result and cache null true should cache.
     */
    @Test
    void handleCaching_WithNullResultAndCacheNullTrue_ShouldCache() throws Throwable {
        CoalesceCacheable cacheable = createMockCacheable("cache", "#id", 300, "", true, true);
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{cacheable},
                new CoalesceEvict[]{},
                new CoalescePut[]{}
        );

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(cacheManager.get("cache:123")).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn(null);

        Object result = aspect.handleCaching(joinPoint, caching);

        assertNull(result);
        verify(cacheManager).put("cache:123", null, 300);
    }


    /**
     * Handle caching with after eviction should evict after execution.
     */
    @Test
    void handleCaching_WithAfterEviction_ShouldEvictAfterExecution() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[]{"cache"}, "", "", false, false, "");
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{},
                new CoalesceEvict[]{evict},
                new CoalescePut[]{}
        );

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleCaching(joinPoint, caching);

        assertEquals("result", result);
    }


    /**
     * Handle caching with put and unless condition true should not cache.
     */
    @Test
    void handleCaching_WithPutAndUnlessConditionTrue_ShouldNotCache() throws Throwable {
        CoalescePut put = createMockPut("cache", "#id", 300, "", "#result == null");
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{},
                new CoalesceEvict[]{},
                new CoalescePut[]{put}
        );

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.proceed()).thenReturn(null);

        Object result = aspect.handleCaching(joinPoint, caching);

        assertNull(result);
        verify(cacheManager, never()).put(anyString(), any(), anyLong());
    }

    /**
     * Handle caching with empty key should use args hashcode.
     */
    @Test
    void handleCaching_WithEmptyKey_ShouldUseArgsHashcode() throws Throwable {
        CoalesceCacheable cacheable = createMockCacheable("cache", "", 300, "", false, true);
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{cacheable},
                new CoalesceEvict[]{},
                new CoalescePut[]{}
        );

        Object[] args = new Object[]{"123"};
        String expectedKey = "cache:" + java.util.Arrays.hashCode(args);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(cacheManager.get(expectedKey)).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleCaching(joinPoint, caching);

        assertEquals("result", result);
        verify(cacheManager).put(expectedKey, "result", 300L);
    }


    /**
     * Handle caching with evict key should evict specific key.
     */
    @Test
    void handleCaching_WithEvictKey_ShouldEvictSpecificKey() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[]{"cache"}, "#id", "", false, false, "");
        CoalesceCaching caching = createMockCaching(
                new CoalesceCacheable[]{},
                new CoalesceEvict[]{evict},
                new CoalescePut[]{}
        );

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleCaching(joinPoint, caching);

        assertEquals("result", result);
        verify(cacheManager).evict("cache:123");
    }

    /**
     * Creates mock cacheable annotation.
     *
     * @param value the value
     * @param key the key
     * @param ttl the ttl
     * @param condition the condition
     * @param cacheNull the cache null
     * @param coalesce the coalesce
     * @return the coalesce cacheable
     */
    private CoalesceCacheable createMockCacheable(
            String value, String key, long ttl, String condition,
            boolean cacheNull, boolean coalesce) {
        CoalesceCacheable cacheable = mock(CoalesceCacheable.class);
        when(cacheable.value()).thenReturn(value);
        when(cacheable.key()).thenReturn(key);
        when(cacheable.ttl()).thenReturn(ttl);
        when(cacheable.condition()).thenReturn(condition);
        when(cacheable.cacheNull()).thenReturn(cacheNull);
        when(cacheable.coalesce()).thenReturn(coalesce);
        return cacheable;
    }

    /**
     * Creates mock evict annotation.
     *
     * @param value the value
     * @param key the key
     * @param pattern the pattern
     * @param beforeInvocation the before invocation
     * @param allEntries the all entries
     * @param condition the condition
     * @return the coalesce evict
     */
    private CoalesceEvict createMockEvict(
            String[] value, String key, String pattern,
            boolean beforeInvocation, boolean allEntries, String condition) {
        CoalesceEvict evict = mock(CoalesceEvict.class);
        when(evict.value()).thenReturn(value);
        when(evict.key()).thenReturn(key);
        when(evict.pattern()).thenReturn(pattern);
        when(evict.beforeInvocation()).thenReturn(beforeInvocation);
        when(evict.allEntries()).thenReturn(allEntries);
        when(evict.condition()).thenReturn(condition);
        return evict;
    }

    /**
     * Creates mock put annotation.
     *
     * @param value the value
     * @param key the key
     * @param ttl the ttl
     * @param condition the condition
     * @param unless the unless
     * @return the coalesce put
     */
    private CoalescePut createMockPut(
            String value, String key, long ttl, String condition, String unless) {
        CoalescePut put = mock(CoalescePut.class);
        when(put.value()).thenReturn(value);
        when(put.key()).thenReturn(key);
        when(put.ttl()).thenReturn(ttl);
        when(put.condition()).thenReturn(condition);
        when(put.unless()).thenReturn(unless);
        return put;
    }

    /**
     * Creates mock caching annotation.
     *
     * @param cacheables the cacheables
     * @param evicts the evicts
     * @param puts the puts
     * @return the coalesce caching
     */
    private CoalesceCaching createMockCaching(
            CoalesceCacheable[] cacheables,
            CoalesceEvict[] evicts,
            CoalescePut[] puts) {
        CoalesceCaching caching = mock(CoalesceCaching.class);
        when(caching.cacheable()).thenReturn(cacheables);
        when(caching.evict()).thenReturn(evicts);
        when(caching.put()).thenReturn(puts);
        return caching;
    }

    /**
     * Gets the test method.
     *
     * @return the test method
     */
    private Method getTestMethod() {
        try {
            return this.getClass().getMethod("testMethod", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
