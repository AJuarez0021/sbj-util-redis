package io.github.ajuarez0021.redis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface CoalesceCacheable.
 *
 * @author ajuar
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CoalesceCacheables.class)
public @interface CoalesceCacheable {
	
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
     * Tiempo de espera máximo en milisegundos para coalescing.
     *
     * @return the long
     */
    long timeout() default 5000;
    
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
     * Si null debe ser cacheado.
     *
     * @return true, if successful
     */
    boolean cacheNull() default false;
    
    /**
     * Si debe usar coalescing de requests.
     *
     * @return true, if successful
     */
    boolean coalesce() default true;
}
