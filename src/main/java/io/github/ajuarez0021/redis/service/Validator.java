package io.github.ajuarez0021.redis.service;

import io.github.ajuarez0021.redis.dto.HostsDto;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class Validator {

    private Validator() {

    }
    /**
     * Validate hosts.
     *
     * @param hosts the hosts
     */
    public static void validateHosts(List<HostsDto> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            throw new IllegalArgumentException("At least one host must be configured");
        }
        for (HostsDto host : hosts) {
            if (host.getHostName() == null || host.getHostName().trim().isEmpty()) {
                throw new IllegalArgumentException("Host name cannot be empty");
            }
            if (host.getPort() < 1 || host.getPort() > 65535) {
                throw new IllegalArgumentException(
                        String.format("Invalid port %d. Port must be between 1 and 65535", host.getPort())
                );
            }
        }
    }

    /**
     * Validate timeout.
     *
     * @param connectionTimeout the connection timeout
     * @param readTimeout       the read timeout
     */
    public static void validateTimeout(Long connectionTimeout, Long readTimeout) {

        Objects.requireNonNull(connectionTimeout, "Connection timeout cannot be null");
        Objects.requireNonNull(readTimeout, "Read timeout cannot be null");

        if (connectionTimeout < 1) {
            throw new IllegalArgumentException(
                    String.format("Invalid connectionTimeout %d.", connectionTimeout)
            );
        }
        if (readTimeout < 1) {
            throw new IllegalArgumentException(
                    String.format("Invalid readTimeout %d.", readTimeout)
            );
        }
    }

    /**
     * Validate standalone hosts.
     *
     * @param hosts the hosts
     */
    public static void validateStandaloneHosts(List<HostsDto> hosts) {
        validateHosts(hosts);
        if (hosts.size() != 1) {
            throw new IllegalArgumentException(
                    "Standalone mode requires exactly one host entry"
            );
        }
    }

    /**
     * Validate cluster hosts.
     *
     * @param hosts the hosts
     */
    public static void validateClusterHosts(List<HostsDto> hosts) {
        validateHosts(hosts);
        if (hosts.size() > 1) {
            return;
        }
        throw new IllegalArgumentException(
                "Cluster mode requires host entries for proper redundancy"
        );
    }
    /**
     * Validate required fields.
     * @param cacheName The cacheName
     * @param key The key
     * @param loader The object
     * @param <T> The generic type
     */
    public static <T> void validateRequiredFields(String cacheName, String key, Supplier<T> loader) {
        if (!StringUtils.hasText(cacheName)) {
            throw new IllegalStateException("cacheName is required");
        }
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("key is required");
        }
        if (loader == null) {
            throw new IllegalStateException("loader is required");
        }
    }


}
