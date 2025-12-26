package io.github.ajuarez0021.redis.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * The Class HostsDto.
 *
 * @author ajuar
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class HostsDto {

    /** The host name. */
    private String hostName;

    /** The port. */
    private int port;
}
