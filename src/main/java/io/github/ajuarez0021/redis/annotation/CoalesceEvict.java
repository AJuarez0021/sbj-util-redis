package io.github.ajuarez0021.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface CoalesceEvict.
 *
 * @author ajuar
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CoalesceEvicts.class)
public @interface CoalesceEvict {

    /**
     * Prefijo de las claves a eliminar.
     *
     * @return the string[]
     */
    String[] value() default {};

    /**
     * Expresión SpEL para generar la clave dinámica.
     *
     * @return the string
     */
    String key() default "";

    /**
     * Si debe eliminar todas las claves que coincidan con el patrón.
     *
     * @return true, if successful
     */
    boolean allEntries() default false;

    /**
     * Patrón para eliminar múltiples claves.
     *
     * @return the string
     */
    String pattern() default "";

    /**
     * Si la evicción debe ocurrir antes de la ejecución del método.
     *
     * @return true, if successful
     */
    boolean beforeInvocation() default false;

    /**
     * Condición SpEL para ejecutar la evicción.
     *
     * @return the string
     */
    String condition() default "";
}
