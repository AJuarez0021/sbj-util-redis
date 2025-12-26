package io.github.ajuarez0021.redis.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * The Class RedisStatusDto.
 *
 * @author ajuar
 */

/**
 * The Class RedisStatusDtoBuilder.
 */
@Builder
@Getter
public class RedisStatusDto {

    /** The connected. */
    private boolean connected;
    
    /** The response time. */
    private long responseTime;
    
    /** The error message. */
    private String errorMessage;

    /** The used memory. */
    private Long usedMemory;

    /** The max memory. */
    private Long maxMemory;

    /** The connected clients. */
    private Integer connectedClients;

    /** The redis version. */
    private String redisVersion;
}
