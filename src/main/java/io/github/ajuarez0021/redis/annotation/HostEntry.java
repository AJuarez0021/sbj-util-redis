package io.github.ajuarez0021.redis.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface HostEntry.
 *
 * @author ajuar
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface HostEntry {

    /**
     * Host.
     *
     * @return the string
     */
    String host();

    /**
     * Port.
     *
     * @return the int
     */
    int port() default 6379;
}
