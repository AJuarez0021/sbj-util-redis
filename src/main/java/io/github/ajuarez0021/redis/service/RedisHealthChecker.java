package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.dto.RedisStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Properties;

/**
 * The Class RedisHealthChecker.
 *
 * @author ajuar
 */
@Slf4j
public class RedisHealthChecker {

    /** The redis template. */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Instantiates a new redis health checker.
     *
     * @param redisTemplate the redis template
     */
    public RedisHealthChecker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Creates the status.
     *
     * @param message the message
     * @return the redis status dto
     */
    private RedisStatusDto createStatus(String message) {

        return RedisStatusDto.builder()
                .connected(false)
                .errorMessage(message)
                .responseTime(0L)
                .maxMemory(0L)
                .usedMemory(0L)
                .connectedClients(0)
                .redisVersion("")
                .build();
    }

    /**
     * Checks if is redis active.
     *
     * @return the redis status dto
     */
    public RedisStatusDto isRedisActive() {

        try {
            if (redisTemplate == null) {
                log.error("RedisTemplate is null");
                return createStatus("RedisTemplate is not configured");
            }
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                log.error("ConnectionFactory is null");
                return createStatus("ConnectionFactory is not available");
            }

            try (RedisConnection connection = connectionFactory.getConnection()) {

                long startTime = System.currentTimeMillis();
                String pong = connection.ping();
                long endTime = System.currentTimeMillis() - startTime;

                Properties properties = connection.serverCommands().info();

                if ("PONG".equalsIgnoreCase(pong)) {
                    log.debug("Redis is up and running and responding correctly");
                    return RedisStatusDto.builder()
                            .connected(true)
                            .errorMessage("Redis is up and running and responding correctly")
                            .responseTime(endTime)
                            .usedMemory(Long.parseLong(properties.getProperty("used_memory")))
                            .maxMemory(Long.parseLong(properties.getProperty("maxmemory")))
                            .connectedClients(Integer.parseInt(properties.getProperty("connected_clients")))
                            .redisVersion(properties.getProperty("redis_version"))
                            .build();
                } else {
                    log.warn("Redis is not responding as expected. Response: {}", pong);
                    return RedisStatusDto.builder()
                            .connected(false)
                            .errorMessage("Redis is not responding as expected. Response: " + pong)
                            .responseTime(endTime)
                            .usedMemory(Long.parseLong(properties.getProperty("used_memory")))
                            .maxMemory(Long.parseLong(properties.getProperty("maxmemory")))
                            .connectedClients(Integer.parseInt(properties.getProperty("connected_clients")))
                            .redisVersion(properties.getProperty("redis_version"))
                            .build();
                }
            }

        } catch (DataAccessException e) {
            log.error("Error checking Redis status: {}", e.getMessage());
            return createStatus(String.format("Error checking Redis status: %s", e.getMessage()));
        }
    }
}
