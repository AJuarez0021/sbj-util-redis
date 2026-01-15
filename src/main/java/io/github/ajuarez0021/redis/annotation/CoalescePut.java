package io.github.ajuarez0021.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface CoalescePut.
 *
 * @author ajuar
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CoalescePuts.class)
public @interface CoalescePut {

    /**
     * Prefijo para la clave de caché.
     *
     * @return the string
     */
    String value() default "";

    /**
     * Expresión SpEL para generar la clave dinámica.
     *
     * @return the string
     */
    String key() default "";

    /**
     * Tiempo de expiración en segundos.
     *
     * @return the long
     */
    long ttl() default 300;

    /**
     * Condición SpEL para ejecutar el cacheo.
     *
     * @return the string
     */
    String condition() default "";

    /**
     * Condición SpEL para decidir si cachear el resultado.
     *
     * @return the string
     */
    String unless() default "";
}
