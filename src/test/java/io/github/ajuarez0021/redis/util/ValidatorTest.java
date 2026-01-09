package io.github.ajuarez0021.redis.util;

import io.github.ajuarez0021.redis.dto.HostsDto;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Validator.
 *
 * @author ajuar
 */
class ValidatorTest {

    /**
     * Validate hosts with null list should throw exception.
     */
    @Test
    void validateHosts_WithNullList_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateHosts(null));
        assertEquals("At least one host must be configured", exception.getMessage());
    }

    /**
     * Validate hosts with empty list should throw exception.
     */
    @Test
    void validateHosts_WithEmptyList_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateHosts(Collections.emptyList()));
        assertEquals("At least one host must be configured", exception.getMessage());
    }

    /**
     * Validate hosts with null hostname should throw exception.
     */
    @Test
    void validateHosts_WithNullHostname_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName(null).port(6379).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts));
        assertEquals("Host name cannot be empty", exception.getMessage());
    }

    /**
     * Validate hosts with empty hostname should throw exception.
     */
    @Test
    void validateHosts_WithEmptyHostname_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("").port(6379).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts));
        assertEquals("Host name cannot be empty", exception.getMessage());
    }

    /**
     * Validate hosts with whitespace hostname should throw exception.
     */
    @Test
    void validateHosts_WithWhitespaceHostname_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("   ").port(6379).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts));
        assertEquals("Host name cannot be empty", exception.getMessage());
    }

    /**
     * Validate hosts with port below range should throw exception.
     */
    @Test
    void validateHosts_WithPortBelowRange_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(0).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts));
        assertEquals("Invalid port 0. Port must be between 1 and 65535", exception.getMessage());
    }

    /**
     * Validate hosts with port above range should throw exception.
     */
    @Test
    void validateHosts_WithPortAboveRange_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(65536).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts));
        assertEquals("Invalid port 65536. Port must be between 1 and 65535", exception.getMessage());
    }

    /**
     * Validate hosts with valid host should not throw exception.
     */
    @Test
    void validateHosts_WithValidHost_ShouldNotThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(6379).build()
        );

        assertDoesNotThrow(() -> Validator.validateHosts(hosts));
    }

    /**
     * Validate timeout with null connection timeout should throw exception.
     */
    @Test
    void validateTimeout_WithNullConnectionTimeout_ShouldThrowException() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> Validator.validateTimeout(null, 5000L));
        assertEquals("Connection timeout cannot be null", exception.getMessage());
    }

    /**
     * Validate timeout with null read timeout should throw exception.
     */
    @Test
    void validateTimeout_WithNullReadTimeout_ShouldThrowException() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> Validator.validateTimeout(5000L, null));
        assertEquals("Read timeout cannot be null", exception.getMessage());
    }

    /**
     * Validate timeout with negative connection timeout should throw exception.
     */
    @Test
    void validateTimeout_WithNegativeConnectionTimeout_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateTimeout(-1L, 5000L));
        assertEquals("Invalid connectionTimeout -1.", exception.getMessage());
    }

    /**
     * Validate timeout with zero connection timeout should throw exception.
     */
    @Test
    void validateTimeout_WithZeroConnectionTimeout_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateTimeout(0L, 5000L));
        assertEquals("Invalid connectionTimeout 0.", exception.getMessage());
    }

    /**
     * Validate timeout with negative read timeout should throw exception.
     */
    @Test
    void validateTimeout_WithNegativeReadTimeout_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateTimeout(5000L, -1L));
        assertEquals("Invalid readTimeout -1.", exception.getMessage());
    }

    /**
     * Validate timeout with zero read timeout should throw exception.
     */
    @Test
    void validateTimeout_WithZeroReadTimeout_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateTimeout(5000L, 0L));
        assertEquals("Invalid readTimeout 0.", exception.getMessage());
    }

    /**
     * Validate timeout with valid timeouts should not throw exception.
     */
    @Test
    void validateTimeout_WithValidTimeouts_ShouldNotThrowException() {
        assertDoesNotThrow(() -> Validator.validateTimeout(5000L, 3000L));
    }

    /**
     * Validate standalone hosts with multiple hosts should throw exception.
     */
    @Test
    void validateStandaloneHosts_WithMultipleHosts_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(6379).build(),
                HostsDto.builder().hostName("localhost").port(6380).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateStandaloneHosts(hosts));
        assertEquals("Standalone mode requires exactly one host entry", exception.getMessage());
    }

    /**
     * Validate standalone hosts with single host should not throw exception.
     */
    @Test
    void validateStandaloneHosts_WithSingleHost_ShouldNotThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(6379).build()
        );

        assertDoesNotThrow(() -> Validator.validateStandaloneHosts(hosts));
    }

    /**
     * Validate cluster hosts with single host should throw exception.
     */
    @Test
    void validateClusterHosts_WithSingleHost_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(6379).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateClusterHosts(hosts));
        assertEquals("Cluster mode requires host entries for proper redundancy",
                exception.getMessage());
    }

    /**
     * Validate cluster hosts with multiple hosts should not throw exception.
     */
    @Test
    void validateClusterHosts_WithMultipleHosts_ShouldNotThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(6379).build(),
                HostsDto.builder().hostName("localhost").port(6380).build()
        );

        assertDoesNotThrow(() -> Validator.validateClusterHosts(hosts));
    }

    /**
     * Validate sentinel hosts with null sentinel master should throw exception.
     */
    @Test
    void validateSentinelHosts_WithNullSentinelMaster_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(26379).build(),
                HostsDto.builder().hostName("localhost").port(26380).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateSentinelHosts(hosts, null));
        assertEquals("sentinelMaster must be configured when using SENTINEL mode",
                exception.getMessage());
    }

    /**
     * Validate sentinel hosts with empty sentinel master should throw exception.
     */
    @Test
    void validateSentinelHosts_WithEmptySentinelMaster_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(26379).build(),
                HostsDto.builder().hostName("localhost").port(26380).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateSentinelHosts(hosts, ""));
        assertEquals("sentinelMaster must be configured when using SENTINEL mode",
                exception.getMessage());
    }

    /**
     * Validate sentinel hosts with single host should throw exception.
     */
    @Test
    void validateSentinelHosts_WithSingleHost_ShouldThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(26379).build()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateSentinelHosts(hosts, "mymaster"));
        assertEquals("Sentinel mode requires host entries for proper redundancy",
                exception.getMessage());
    }

    /**
     * Validate sentinel hosts with valid config should not throw exception.
     */
    @Test
    void validateSentinelHosts_WithValidConfig_ShouldNotThrowException() {
        List<HostsDto> hosts = Arrays.asList(
                HostsDto.builder().hostName("localhost").port(26379).build(),
                HostsDto.builder().hostName("localhost").port(26380).build()
        );

        assertDoesNotThrow(() -> Validator.validateSentinelHosts(hosts, "mymaster"));
    }

    /**
     * Validate required fields with null cache name should throw exception.
     */
    @Test
    void validateRequiredFields_WithNullCacheName_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateRequiredFields(null, "key", loader));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate required fields with empty cache name should throw exception.
     */
    @Test
    void validateRequiredFields_WithEmptyCacheName_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateRequiredFields("", "key", loader));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate required fields with null key should throw exception.
     */
    @Test
    void validateRequiredFields_WithNullKey_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateRequiredFields("cache", null, loader));
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate required fields with empty key should throw exception.
     */
    @Test
    void validateRequiredFields_WithEmptyKey_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateRequiredFields("cache", "", loader));
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate required fields with null loader should throw exception.
     */
    @Test
    void validateRequiredFields_WithNullLoader_ShouldThrowException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateRequiredFields("cache", "key", null));
        assertEquals("loader is required", exception.getMessage());
    }

    /**
     * Validate required fields with valid params should not throw exception.
     */
    @Test
    void validateRequiredFields_WithValidParams_ShouldNotThrowException() {
        Supplier<String> loader = () -> "value";

        assertDoesNotThrow(() -> Validator.validateRequiredFields("cache", "key", loader));
    }

    /**
     * Validate TT L with null cache name should throw exception.
     */
    @Test
    void validateTTL_WithNullCacheName_ShouldThrowException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateTTL(null, 60L));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate TT L with empty cache name should throw exception.
     */
    @Test
    void validateTTL_WithEmptyCacheName_ShouldThrowException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateTTL("", 60L));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate TT L with null ttl should throw exception.
     */
    @Test
    void validateTTL_WithNullTtl_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateTTL("cache", null));
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate TT L with zero ttl should throw exception.
     */
    @Test
    void validateTTL_WithZeroTtl_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateTTL("cache", 0L));
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate TT L with negative ttl should throw exception.
     */
    @Test
    void validateTTL_WithNegativeTtl_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateTTL("cache", -1L));
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate TT L with valid params should not throw exception.
     */
    @Test
    void validateTTL_WithValidParams_ShouldNotThrowException() {
        assertDoesNotThrow(() -> Validator.validateTTL("cache", 60L));
    }

    /**
     * Validate cacheable with null ttl should throw exception.
     */
    @Test
    void validateCacheable_WithNullTtl_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateCacheable("cache", "key", loader, null));
        assertEquals("ttl is required", exception.getMessage());
    }

    /**
     * Validate cacheable with negative ttl should throw exception.
     */
    @Test
    void validateCacheable_WithNegativeTtl_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheable("cache", "key", loader, Duration.ofMinutes(-1)));
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate cacheable with zero ttl should throw exception.
     */
    @Test
    void validateCacheable_WithZeroTtl_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheable("cache", "key", loader, Duration.ZERO));
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate cacheable with cache name containing colon should throw exception.
     */
    @Test
    void validateCacheable_WithCacheNameContainingColon_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheable("cache:name", "key", loader, Duration.ofMinutes(10)));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cacheable with cache name containing asterisk should throw exception.
     */
    @Test
    void validateCacheable_WithCacheNameContainingAsterisk_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheable("cache*name", "key", loader, Duration.ofMinutes(10)));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cacheable with key containing asterisk should throw exception.
     */
    @Test
    void validateCacheable_WithKeyContainingAsterisk_ShouldThrowException() {
        Supplier<String> loader = () -> "value";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheable("cache", "key*value", loader, Duration.ofMinutes(10)));
        assertEquals("key cannot contain '*' character", exception.getMessage());
    }

    /**
     * Validate cacheable with valid params should not throw exception.
     */
    @Test
    void validateCacheable_WithValidParams_ShouldNotThrowException() {
        Supplier<String> loader = () -> "value";

        assertDoesNotThrow(() -> Validator.validateCacheable("cache", "key", loader,
                Duration.ofMinutes(10)));
    }

    /**
     * Validate cache evict with null cache name should throw exception.
     */
    @Test
    void validateCacheEvict_WithNullCacheName_ShouldThrowException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateCacheEvict(null, "key"));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache evict with empty cache name should throw exception.
     */
    @Test
    void validateCacheEvict_WithEmptyCacheName_ShouldThrowException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateCacheEvict("", "key"));
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache evict with null key should throw exception.
     */
    @Test
    void validateCacheEvict_WithNullKey_ShouldThrowException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateCacheEvict("cache", null));
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate cache evict with empty key should throw exception.
     */
    @Test
    void validateCacheEvict_WithEmptyKey_ShouldThrowException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> Validator.validateCacheEvict("cache", ""));
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate cache evict with cache name containing colon should throw exception.
     */
    @Test
    void validateCacheEvict_WithCacheNameContainingColon_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheEvict("cache:name", "key"));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cache evict with cache name containing asterisk should throw exception.
     */
    @Test
    void validateCacheEvict_WithCacheNameContainingAsterisk_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheEvict("cache*name", "key"));
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cache evict with key containing asterisk should throw exception.
     */
    @Test
    void validateCacheEvict_WithKeyContainingAsterisk_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Validator.validateCacheEvict("cache", "key*value"));
        assertEquals("key cannot contain '*' character", exception.getMessage());
    }

    /**
     * Validate cache evict with valid params should not throw exception.
     */
    @Test
    void validateCacheEvict_WithValidParams_ShouldNotThrowException() {
        assertDoesNotThrow(() -> Validator.validateCacheEvict("cache", "key"));
    }
}
