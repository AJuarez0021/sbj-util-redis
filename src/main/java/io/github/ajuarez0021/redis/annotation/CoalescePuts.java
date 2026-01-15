package io.github.ajuarez0021.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface CoalescePuts.
 *
 * @author ajuar
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CoalescePuts {

    /**
     * Value.
     *
     * @return the coalesce put[]
     */
    CoalescePut[] value();
}
