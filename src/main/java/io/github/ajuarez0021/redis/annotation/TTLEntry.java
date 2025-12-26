package io.github.ajuarez0021.redis.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface TTLEntry.
 *
 * @author ajuar
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface TTLEntry {

    /**
     * Name.
     *
     * @return the string
     */
    String name();

    /**
     * Ttl.
     *
     * @return the long
     */
    long ttl();
}
