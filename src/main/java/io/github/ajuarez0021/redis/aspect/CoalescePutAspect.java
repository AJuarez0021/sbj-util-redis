package io.github.ajuarez0021.redis.aspect;

import io.github.ajuarez0021.redis.annotation.CoalescePut;
import io.github.ajuarez0021.redis.annotation.CoalescePuts;
import io.github.ajuarez0021.redis.service.CoalesceCacheManager;
import java.util.Arrays;
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
 * The Class CoalescePutAspect.
 *
 * @author ajuar
 */
@Aspect
@Component
@Slf4j
@Order(2)
public class CoalescePutAspect {

    /** The cache manager. */
    private final CoalesceCacheManager cacheManager;
    
    /** The parser. */
    private final SpelExpressionParser parser;

    /**
     * Instantiates a new coalesce put aspect.
     *
     * @param cacheManager the cache manager
     */
    public CoalescePutAspect(CoalesceCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.parser = new SpelExpressionParser();
    }

    /**
     * Handle single put.
     *
     * @param joinPoint the join point
     * @param put the put
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(put)")
    public Object handleSinglePut(
            ProceedingJoinPoint joinPoint,
            CoalescePut put) throws Throwable {

        return processPut(joinPoint, new CoalescePut[]{put});
    }

    /**
     * Handle multiple puts.
     *
     * @param joinPoint the join point
     * @param puts the puts
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(puts)")
    public Object handleMultiplePuts(
            ProceedingJoinPoint joinPoint,
            CoalescePuts puts) throws Throwable {

        return processPut(joinPoint, puts.value());
    }

    /**
     * Process put.
     *
     * @param joinPoint the join point
     * @param puts the puts
     * @return the object
     * @throws Throwable the throwable
     */
    private Object processPut(
            ProceedingJoinPoint joinPoint,
            CoalescePut[] puts) throws Throwable {

        Object result = joinPoint.proceed();

        for (CoalescePut put : puts) {

            if (shouldCacheResult(joinPoint, put, result)) {
                String cacheKey = generateCacheKey(joinPoint, put);
                cacheManager.put(cacheKey, result, put.ttl());
                log.info("Cached result for key: {} with TTL: {} seconds", cacheKey, put.ttl());
            }
        }

        return result;
    }

    /**
     * Should cache result.
     *
     * @param joinPoint the join point
     * @param put the put
     * @param result the result
     * @return true, if successful
     */
    private boolean shouldCacheResult(
            ProceedingJoinPoint joinPoint,
            CoalescePut put,
            Object result) {

        boolean conditionPassed = evaluateCondition(joinPoint, put.condition(), result);
        if (!conditionPassed) {
            return false;
        }

        boolean unlessPassed = evaluateCondition(joinPoint, put.unless(), result);
        return !unlessPassed;
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

        StandardEvaluationContext context = createEvaluationContext(joinPoint, result);
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, resultType);
    }

    /**
     * Creates the evaluation context.
     *
     * @param joinPoint the join point
     * @param result the result
     * @return the standard evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(
            ProceedingJoinPoint joinPoint,
            Object result) {
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

        return context;
    }
}
