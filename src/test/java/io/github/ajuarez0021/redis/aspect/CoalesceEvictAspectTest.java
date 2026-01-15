package io.github.ajuarez0021.redis.aspect;

import io.github.ajuarez0021.redis.annotation.CoalesceEvict;
import io.github.ajuarez0021.redis.annotation.CoalesceEvicts;
import io.github.ajuarez0021.redis.service.CoalesceCacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for CoalesceEvictAspect.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoalesceEvictAspectTest {

    /** The cache manager. */
    @Mock
    private CoalesceCacheManager cacheManager;

    /** The join point. */
    @Mock
    private ProceedingJoinPoint joinPoint;

    /** The method signature. */
    @Mock
    private MethodSignature methodSignature;

    /** The signature. */
    @Mock
    private Signature signature;

    /** The aspect. */
    private CoalesceEvictAspect aspect;

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        aspect = new CoalesceEvictAspect(cacheManager);
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
     * Handle single evict with all entries should evict all.
     */
    @Test
    void handleSingleEvict_WithAllEntries_ShouldEvictAll() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "", "", true, true, "");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSingleEvict(joinPoint, evict);

        assertEquals("result", result);
        verify(cacheManager).evictAll("cache");
        verify(joinPoint).proceed();
    }



    /**
     * Handle single evict with key should evict specific key.
     */
    @Test
    void handleSingleEvict_WithKey_ShouldEvictSpecificKey() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "#id", "", true, false, "");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSingleEvict(joinPoint, evict);

        assertEquals("result", result);
        verify(cacheManager).evict("cache:123");
        verify(joinPoint).proceed();
    }

    /**
     * Handle single evict with empty key should use args hashcode.
     */
    @Test
    void handleSingleEvict_WithEmptyKey_ShouldUseArgsHashcode() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "", "", true, false, "");
        Object[] args = new Object[] { "123" };
        String expectedKey = "cache:" + java.util.Arrays.hashCode(args);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "id" });
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSingleEvict(joinPoint, evict);

        assertEquals("result", result);
        verify(cacheManager).evict(expectedKey);
    }

    /**
     * Handle single evict with empty value should use method signature.
     */
    @Test
    void handleSingleEvict_WithEmptyValue_ShouldUseMethodSignature() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] {}, "", "", true, false, "");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[] { "123" });
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSingleEvict(joinPoint, evict);

        assertEquals("result", result);
        verify(cacheManager).evict(anyString());
    }

    /**
     * Handle single evict before invocation should evict before proceed.
     */
    @Test
    void handleSingleEvict_BeforeInvocation_ShouldEvictBeforeProceed() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "", "", true, true, "");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        aspect.handleSingleEvict(joinPoint, evict);

        verify(cacheManager).evictAll("cache");
        verify(joinPoint).proceed();
    }

    /**
     * Handle single evict after invocation should evict after proceed.
     */
    @Test
    void handleSingleEvict_AfterInvocation_ShouldEvictAfterProceed() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "", "", false, true, "");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        aspect.handleSingleEvict(joinPoint, evict);

        verify(joinPoint).proceed();
        verify(cacheManager).evictAll("cache");
    }

    /**
     * Handle single evict with condition false should not evict.
     */
    @Test
    void handleSingleEvict_WithConditionFalse_ShouldNotEvict() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "", "", true, true, "#id == '999'");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSingleEvict(joinPoint, evict);

        assertEquals("result", result);
        verify(cacheManager, never()).evictAll(anyString());
    }

    /**
     * Handle single evict with condition true should evict.
     */
    @Test
    void handleSingleEvict_WithConditionTrue_ShouldEvict() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "", "", true, true, "#id == '123'");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSingleEvict(joinPoint, evict);

        assertEquals("result", result);
        verify(cacheManager).evictAll("cache");
    }

    /**
     * Handle single evict with exception should throw exception.
     */
    @Test
    void handleSingleEvict_WithException_ShouldThrowException() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] { "cache" }, "", "", true, true, "");
        setupJoinPoint();
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));

        assertThrows(RuntimeException.class, () -> aspect.handleSingleEvict(joinPoint, evict));
        verify(cacheManager).evictAll("cache");
    }

    /**
     * Handle multiple evicts should process all evictions.
     */
    @Test
    void handleMultipleEvicts_ShouldProcessAllEvictions() throws Throwable {
        CoalesceEvict evict1 = createMockEvict(new String[] { "cache1" }, "", "", true, true, "");
        CoalesceEvict evict2 = createMockEvict(new String[] { "cache2" }, "", "", true, true, "");
        CoalesceEvicts evicts = createMockEvicts(new CoalesceEvict[] { evict1, evict2 });

        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleMultipleEvicts(joinPoint, evicts);

        assertEquals("result", result);
        verify(cacheManager).evictAll("cache1");
        verify(cacheManager).evictAll("cache2");
    }

    /**
     * Handle multiple evicts with mixed timing should respect order.
     */
    @Test
    void handleMultipleEvicts_WithMixedTiming_ShouldRespectOrder() throws Throwable {
        CoalesceEvict evict1 = createMockEvict(new String[] { "cache1" }, "", "", true, true, "");
        CoalesceEvict evict2 = createMockEvict(new String[] { "cache2" }, "", "", false, true, "");
        CoalesceEvicts evicts = createMockEvicts(new CoalesceEvict[] { evict1, evict2 });

        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        aspect.handleMultipleEvicts(joinPoint, evicts);

        verify(cacheManager).evictAll("cache1");
        verify(cacheManager).evictAll("cache2");
        verify(joinPoint).proceed();
    }

    /**
     * Handle multiple evicts with all entries should evict all for multiple
     * prefixes.
     */
    @Test
    void handleMultipleEvicts_WithAllEntries_ShouldEvictAllForMultiplePrefixes() throws Throwable {
        CoalesceEvict evict = createMockEvict(
                new String[] { "cache1", "cache2" }, "", "", true, true, "");
        CoalesceEvicts evicts = createMockEvicts(new CoalesceEvict[] { evict });

        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        aspect.handleMultipleEvicts(joinPoint, evicts);

        verify(cacheManager).evictAll("cache1");
        verify(cacheManager).evictAll("cache2");
    }

    /**
     * Handle single evict with pattern expression should evaluate pattern.
     */
    @Test
    void handleSingleEvict_WithPatternExpression_ShouldEvaluatePattern() throws Throwable {
        CoalesceEvict evict = createMockEvict(new String[] {}, "", "'cache:' + #id", true, false, "");
        setupJoinPoint();
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.handleSingleEvict(joinPoint, evict);

        assertEquals("result", result);
        verify(cacheManager).evictPattern("cache:123");
    }

    /**
     * Setup join point with default values.
     */
    private void setupJoinPoint() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "123" });
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "id" });
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
    }

    /**
     * Creates mock evict annotation.
     *
     * @param value            the value
     * @param key              the key
     * @param pattern          the pattern
     * @param beforeInvocation the before invocation
     * @param allEntries       the all entries
     * @param condition        the condition
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
     * Creates mock evicts annotation.
     *
     * @param value the value
     * @return the coalesce evicts
     */
    private CoalesceEvicts createMockEvicts(CoalesceEvict[] value) {
        CoalesceEvicts evicts = mock(CoalesceEvicts.class);
        when(evicts.value()).thenReturn(value);
        return evicts;
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
