package io.github.ajuarez0021.redis.aspect;

import io.github.ajuarez0021.redis.annotation.CoalesceCacheable;
import io.github.ajuarez0021.redis.annotation.CoalesceCaching;
import io.github.ajuarez0021.redis.annotation.CoalesceEvict;
import io.github.ajuarez0021.redis.annotation.CoalescePut;
import io.github.ajuarez0021.redis.service.CoalesceCacheManager;
import java.util.Arrays;
import org.springframework.expression.Expression;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * The Class CoalesceCachingAspect.
 *
 * @author ajuar
 */
@Aspect
@Component
@Slf4j
@Order(1)
public class CoalesceCachingAspect {

    /** The cache manager. */
    private final CoalesceCacheManager cacheManager;
    
    /** The parser. */
    private final SpelExpressionParser parser;

    /**
     * Instantiates a new coalesce caching aspect.
     *
     * @param cacheManager the cache manager
     */
    public CoalesceCachingAspect(
            CoalesceCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.parser = new SpelExpressionParser();
    }

    /**
     * Handle caching.
     *
     * @param joinPoint the join point
     * @param caching the caching
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(caching)")
    public Object handleCaching(
            ProceedingJoinPoint joinPoint,
            CoalesceCaching caching) throws Throwable {

        CoalesceCacheable[] cacheables = caching.cacheable();
        if (cacheables.length > 0) {
            for (CoalesceCacheable cacheable : cacheables) {
                String cacheKey = generateCacheKey(joinPoint, cacheable);
                Optional<Object> cached = cacheManager.get(cacheKey);
                if (cached.isPresent()) {
                    log.debug("Returning cached value from @CoalesceCaching for key: {}", cacheKey);
                    return cached.get();
                }
            }
        }

        CoalesceEvict[] evicts = caching.evict();
        for (CoalesceEvict evict : evicts) {
            if (evict.beforeInvocation() && evaluateCondition(joinPoint, evict.condition())) {
                performEviction(joinPoint, evict);
            }
        }

        Object result = joinPoint.proceed();

        CoalescePut[] puts = caching.put();
        for (CoalescePut put : puts) {
            if (evaluateCondition(joinPoint, put.condition(), result)
                    && !evaluateCondition(joinPoint, put.unless(), result)) {

                String cacheKey = generateCacheKey(joinPoint, put);
                cacheManager.put(cacheKey, result, put.ttl());
                log.info("Cached result from @CoalesceCaching for key: {}", cacheKey);
            }
        }

        for (CoalesceEvict evict : evicts) {
            if (!evict.beforeInvocation() && evaluateCondition(joinPoint, evict.condition())) {
                performEviction(joinPoint, evict);
            }
        }

        for (CoalesceCacheable cacheable : cacheables) {
            String cacheKey = generateCacheKey(joinPoint, cacheable);
            if (result != null || cacheable.cacheNull()) {
                cacheManager.put(cacheKey, result, cacheable.ttl());
            }
        }

        return result;
    }

    /**
     * Generate cache key.
     *
     * @param joinPoint the join point
     * @param cacheable the cacheable
     * @return the string
     */
    private String generateCacheKey(ProceedingJoinPoint joinPoint, CoalesceCacheable cacheable) {
        String baseKey = cacheable.value().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : cacheable.value();

        if (!cacheable.key().isEmpty()) {
            String dynamicKey = evaluateExpression(joinPoint, cacheable.key(), null, String.class);
            return baseKey + ":" + dynamicKey;
        }

        return baseKey + ":" + Arrays.hashCode(joinPoint.getArgs());
    }

    /**
     * Generate cache key.
     *
     * @param joinPoint the join point
     * @param put the put
     * @return the string
     */
    private String generateCacheKey(ProceedingJoinPoint joinPoint, CoalescePut put) {
        String baseKey = put.value().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : put.value();

        if (!put.key().isEmpty()) {
            String dynamicKey = evaluateExpression(joinPoint, put.key(), null, String.class);
            return baseKey + ":" + dynamicKey;
        }

        return baseKey + ":" + Arrays.hashCode(joinPoint.getArgs());
    }

    /**
     * Perform eviction.
     *
     * @param joinPoint the join point
     * @param evict the evict
     */
    private void performEviction(ProceedingJoinPoint joinPoint, CoalesceEvict evict) {
        if (evict.allEntries()) {
            for (String prefix : evict.value()) {
                cacheManager.evictAll(prefix);
                log.info("Evicted all entries for prefix: {}", prefix);
            }
        } else if (!evict.pattern().isEmpty()) {
            String pattern = evaluateExpression(joinPoint, evict.pattern(), null, String.class);
            cacheManager.evictPattern(pattern);
            log.info("Evicted entries matching pattern: {}", pattern);
        } else {
            String cacheKey = generateCacheKeyForEvict(joinPoint, evict);
            cacheManager.evict(cacheKey);
            log.info("Evicted cache key: {}", cacheKey);
        }
    }

    /**
     * Generate cache key for evict.
     *
     * @param joinPoint the join point
     * @param evict the evict
     * @return the string
     */
    private String generateCacheKeyForEvict(ProceedingJoinPoint joinPoint, CoalesceEvict evict) {
        String baseKey = evict.value().length > 0
                ? evict.value()[0]
                : joinPoint.getSignature().toShortString();

        if (!evict.key().isEmpty()) {
            String dynamicKey = evaluateExpression(joinPoint, evict.key(), null, String.class);
            return baseKey + ":" + dynamicKey;
        }

        return baseKey + ":" + Arrays.hashCode(joinPoint.getArgs());
    }

    /**
     * Evaluate condition.
     *
     * @param joinPoint the join point
     * @param condition the condition
     * @return true, if successful
     */
    private boolean evaluateCondition(ProceedingJoinPoint joinPoint, String condition) {
        return evaluateCondition(joinPoint, condition, null);
    }

    /**
     * Evaluate condition.
     *
     * @param joinPoint the join point
     * @param condition the condition
     * @param result the result
     * @return true, if successful
     */
    private boolean evaluateCondition(
            ProceedingJoinPoint joinPoint,
            String condition,
            Object result) {
        if (condition.isEmpty()) {
            return true;
        }

        return evaluateExpression(joinPoint, condition, result, Boolean.class);
    }

    /**
     * Evaluate expression.
     *
     * @param <T> the generic type
     * @param joinPoint the join point
     * @param expression the expression
     * @param result the result
     * @param resultType the result type
     * @return the t
     */
    private <T> T evaluateExpression(
            ProceedingJoinPoint joinPoint,
            String expression,
            Object result,
            Class<T> resultType) {

        StandardEvaluationContext context = new StandardEvaluationContext();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        if (result != null) {
            context.setVariable("result", result);
        }

        context.setVariable("target", joinPoint.getTarget());
        context.setVariable("method", signature.getMethod());

        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, resultType);
    }
}
