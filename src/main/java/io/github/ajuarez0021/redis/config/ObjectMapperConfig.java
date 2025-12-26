package io.github.ajuarez0021.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * The Interface ObjectMapperConfig.
 *
 * @author ajuar
 */
public interface ObjectMapperConfig {

    /**
     * Configure.
     *
     * @return the object mapper
     */
    ObjectMapper configure();
}
