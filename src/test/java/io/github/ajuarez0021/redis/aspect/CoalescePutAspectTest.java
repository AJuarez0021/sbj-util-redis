package io.github.ajuarez0021.redis.aspect;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for CoalescePutAspect.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoalescePutAspectTest {

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
    private CoalescePutAspect aspect;

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        aspect = new CoalescePutAspect(cacheManager);
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
     * Handle single put with condition false should not cache.
     */
    @Test
    void handleSinglePut_WithConditionFalse_ShouldNotCache() throws Throwable {
        CoalescePut put = createMockPut("cache", "#id", 300, "#id == '999'", "");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSinglePut(joinPoint, put);

        assertEquals("result", result);
        verify(cacheManager, never()).put(anyString(), any(), anyLong());
    }


    /**
     * Handle single put with unless true should not cache.
     */
    @Test
    void handleSinglePut_WithUnlessTrue_ShouldNotCache() throws Throwable {
        CoalescePut put = createMockPut("cache", "#id", 300, "", "#result == 'result'");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSinglePut(joinPoint, put);

        assertEquals("result", result);
        verify(cacheManager, never()).put(anyString(), any(), anyLong());
    }

    /**
     * Handle single put with unless false should cache.
     */
    @Test
    void handleSinglePut_WithUnlessFalse_ShouldCache() throws Throwable {
        CoalescePut put = createMockPut("cache", "#id", 300, "", "#result == 'other'");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSinglePut(joinPoint, put);

        assertEquals("result", result);
        verify(cacheManager).put("cache:123", "result", 300);
    }


    /**
     * Handle single put with unless null check should not cache null.
     */
    @Test
    void handleSinglePut_WithUnlessNullCheck_ShouldNotCacheNull() throws Throwable {
        CoalescePut put = createMockPut("cache", "#id", 300, "", "#result == null");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn(null);

        Object result = aspect.handleSinglePut(joinPoint, put);

        assertEquals(null, result);
        verify(cacheManager, never()).put(anyString(), any(), anyLong());
    }

    /**
     * Handle single put with condition and unless both true should not cache.
     */
    @Test
    void handleSinglePut_WithConditionAndUnlessBothTrue_ShouldNotCache() throws Throwable {
        CoalescePut put = createMockPut(
                "cache", "#id", 300,
                "#id == '123'",
                "#result == 'result'"
        );
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSinglePut(joinPoint, put);

        assertEquals("result", result);
        verify(cacheManager, never()).put(anyString(), any(), anyLong());
    }


    /**
     * Setup join point with default values.
     */
    private void setupJoinPoint() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
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
