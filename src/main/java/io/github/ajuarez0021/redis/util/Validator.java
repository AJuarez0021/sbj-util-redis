package io.github.ajuarez0021.redis.util;

import io.github.ajuarez0021.redis.dto.HostsDto;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
     * Validate sentinel hosts.
     *
     * @param hosts the hosts
     * @param sentinelMaster the sentinel master
     */
    public static void validateSentinelHosts(List<HostsDto> hosts, String sentinelMaster) {
        validateHosts(hosts);
        if (!StringUtils.hasText(sentinelMaster)) {
            throw new IllegalArgumentException(
                    "sentinelMaster must be configured when using SENTINEL mode"
            );
        }
        if (hosts.size() > 1) {
            return;
        }
        throw new IllegalArgumentException(
                "Sentinel mode requires host entries for proper redundancy"
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


    /**
     * Validate cacheable.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @param ttl the ttl
     */
    public static <T> void validateCacheable(String cacheName, String key, Supplier<T> loader, Duration ttl) {
        Objects.requireNonNull(cacheName, "cacheName cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(ttl, "ttl cannot be null");

        if (cacheName.trim().isEmpty()) {
            throw new IllegalArgumentException("cacheName cannot be empty");
        }

        if (key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }

        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        validateKeyFormat(cacheName, key);
    }

    /**
     * Validate key format.
     *
     * @param cacheName the cache name
     * @param key the key
     */
    private static void validateKeyFormat(String cacheName, String key) {
        if (cacheName.contains(":") || cacheName.contains("*")) {
            throw new IllegalArgumentException(
                    "cacheName cannot contain ':' or '*' characters"
            );
        }
        if (key.contains("*")) {
            throw new IllegalArgumentException(
                    "key cannot contain '*' character"
            );
        }
    }
}
