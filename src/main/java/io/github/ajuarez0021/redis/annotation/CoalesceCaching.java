package io.github.ajuarez0021.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface CoalesceCaching.
 *
 * @author ajuar
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CoalesceCaching {

    /**
     * Array de anotaciones CoalesceEvict.
     *
     * @return the coalesce evict[]
     */
    CoalesceEvict[] evict() default {};

    /**
     * Array de anotaciones CoalescePut.
     *
     * @return the coalesce put[]
     */
    CoalescePut[] put() default {};

    /**
     * Array de anotaciones CoalesceCacheable.
     *
     * @return the coalesce cacheable[]
     */
    CoalesceCacheable[] cacheable() default {};
}
