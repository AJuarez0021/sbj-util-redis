package io.github.ajuarez0021.redis.aspect;

import io.github.ajuarez0021.redis.annotation.CoalesceEvict;
import io.github.ajuarez0021.redis.annotation.CoalesceEvicts;
import io.github.ajuarez0021.redis.service.CoalesceCacheManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * The Class CoalesceEvictAspect.
 *
 * @author ajuar
 */
@Aspect
@Component
@Slf4j
@Order(3)
public class CoalesceEvictAspect {

    /** The cache manager. */
    private final CoalesceCacheManager cacheManager;
    
    /** The parser. */
    private final SpelExpressionParser parser;

    /**
     * Instantiates a new coalesce evict aspect.
     *
     * @param cacheManager the cache manager
     */
    public CoalesceEvictAspect(CoalesceCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.parser = new SpelExpressionParser();
    }

    /**
     * Handle single evict.
     *
     * @param joinPoint the join point
     * @param evict the evict
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(evict)")
    public Object handleSingleEvict(
            ProceedingJoinPoint joinPoint,
            CoalesceEvict evict) throws Throwable {

        return processEviction(joinPoint, new CoalesceEvict[]{evict});
    }

    /**
     * Handle multiple evicts.
     *
     * @param joinPoint the join point
     * @param evicts the evicts
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(evicts)")
    public Object handleMultipleEvicts(
            ProceedingJoinPoint joinPoint,
            CoalesceEvicts evicts) throws Throwable {

        return processEviction(joinPoint, evicts.value());
    }

    /**
     * Process eviction.
     *
     * @param joinPoint the join point
     * @param evicts the evicts
     * @return the object
     * @throws Throwable the throwable
     */
    private Object processEviction(
            ProceedingJoinPoint joinPoint,
            CoalesceEvict[] evicts) throws Throwable {

        List<CoalesceEvict> beforeEvictions = new ArrayList<>();
        List<CoalesceEvict> afterEvictions = new ArrayList<>();

        for (CoalesceEvict evict : evicts) {
            if (evaluateCondition(joinPoint, evict.condition())) {
                if (evict.beforeInvocation()) {
                    beforeEvictions.add(evict);
                } else {
                    afterEvictions.add(evict);
                }
            }
        }

        for (CoalesceEvict evict : beforeEvictions) {
            performEviction(joinPoint, evict);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {

            if (!beforeEvictions.isEmpty()) {
                throw t;
            }
            throw t;
        }

        for (CoalesceEvict evict : afterEvictions) {
            performEviction(joinPoint, evict);
        }

        return result;
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

            String pattern = evaluateExpression(joinPoint, evict.pattern(), String.class);
            cacheManager.evictPattern(pattern);
            log.info("Evicted entries matching pattern: {}", pattern);
        } else {

            String cacheKey = generateCacheKey(joinPoint, evict);
            cacheManager.evict(cacheKey);
            log.info("Evicted cache key: {}", cacheKey);
        }
    }

    /**
     * Generate cache key.
     *
     * @param joinPoint the join point
     * @param evict the evict
     * @return the string
     */
    private String generateCacheKey(ProceedingJoinPoint joinPoint, CoalesceEvict evict) {
        String baseKey = evict.value().length > 0
                ? evict.value()[0]
                : joinPoint.getSignature().toShortString();

        if (!evict.key().isEmpty()) {
            String dynamicKey = evaluateExpression(joinPoint, evict.key(), String.class);
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
        if (condition.isEmpty()) {
            return true;
        }

        return evaluateExpression(joinPoint, condition, Boolean.class);
    }

    /**
     * Evaluate expression.
     *
     * @param <T> the generic type
     * @param joinPoint the join point
     * @param expression the expression
     * @param resultType the result type
     * @return the t
     */
    private <T> T evaluateExpression(
            ProceedingJoinPoint joinPoint,
            String expression,
            Class<T> resultType) {

        StandardEvaluationContext context = createEvaluationContext(joinPoint);
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, resultType);
    }

    /**
     * Creates the evaluation context.
     *
     * @param joinPoint the join point
     * @return the standard evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        context.setVariable("target", joinPoint.getTarget());
        context.setVariable("method", signature.getMethod());

        return context;
    }
}
