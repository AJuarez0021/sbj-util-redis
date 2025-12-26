package io.github.ajuarez0021.redis.config;

import io.github.ajuarez0021.redis.annotation.EnableRedisLibrary;
import io.github.ajuarez0021.redis.service.CacheOperationBuilder;
import io.github.ajuarez0021.redis.service.RedisCacheService;
import io.github.ajuarez0021.redis.service.RedisHealthChecker;
import io.github.ajuarez0021.redis.util.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheConfig.
 *
 * @author ajuar
 */
@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

    @Mock
    private AnnotationMetadata annotationMetadata;

    private CacheConfig cacheConfig;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
    }

    // ========== Bean Creation Tests ==========

    @Test
    void redisHealthChecker_ShouldCreateInstance() {
        // Given
        RedisTemplate<String, Object> mockRedisTemplate = mock(RedisTemplate.class);

        // When
        RedisHealthChecker result = cacheConfig.redisHealthChecker(mockRedisTemplate);

        // Then
        assertNotNull(result);
        assertInstanceOf(RedisHealthChecker.class, result);
    }

    @Test
    void redisCacheService_ShouldCreateInstance() {
        // Given
        RedisTemplate<String, Object> mockRedisTemplate = mock(RedisTemplate.class);

        // When
        RedisCacheService result = cacheConfig.redisCacheService(mockRedisTemplate);

        // Then
        assertNotNull(result);
        assertInstanceOf(RedisCacheService.class, result);
    }

    @Test
    void cacheOperationBuilderFactory_ShouldCreateInstance() {
        // Given
        RedisCacheService mockCacheService = mock(RedisCacheService.class);

        // When
        CacheOperationBuilder.Factory result = cacheConfig.cacheOperationBuilderFactory(mockCacheService);

        // Then
        assertNotNull(result);
        assertInstanceOf(CacheOperationBuilder.Factory.class, result);
    }

    @Test
    void cacheOperationBuilderFactory_ShouldCreateNewBuilderInstances() {
        // Given
        RedisCacheService mockCacheService = mock(RedisCacheService.class);
        CacheOperationBuilder.Factory factory = cacheConfig.cacheOperationBuilderFactory(mockCacheService);

        // When
        CacheOperationBuilder<String> builder1 = factory.create();
        CacheOperationBuilder<String> builder2 = factory.create();

        // Then
        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2, "Factory should create new instances");
    }

    // ========== SetImportMetadata Tests ==========

    @Test
    void setImportMetadata_ShouldSetAttributes() {
        // Given
        Map<String, Object> attributesMap = createBasicAttributesMap();
        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        // When & Then
        assertDoesNotThrow(() -> cacheConfig.setImportMetadata(annotationMetadata));
        verify(annotationMetadata).getAnnotationAttributes(EnableRedisLibrary.class.getName());
    }

    // ========== Connection Factory Tests ==========

    @Test
    void createRedisConnectionFactory_WithStandaloneMode_ShouldCreateStandaloneFactory() {
        // Given
        setupStandaloneConfiguration();

        // When
        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        // Then
        assertNotNull(factory);
    }

    @Test
    void createRedisConnectionFactory_WithClusterMode_ShouldCreateClusterFactory() {
        // Given
        setupClusterConfiguration();

        // When
        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        // Then
        assertNotNull(factory);
    }

    @Test
    void createRedisConnectionFactory_WithCredentials_ShouldConfigureAuth() {
        // Given
        setupStandaloneConfigurationWithCredentials();

        // When
        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        // Then
        assertNotNull(factory);
    }

    @Test
    void createClusterConfig_WithCredentials_ShouldConfigureAuth() {
        // Given
        setupClusterConfigurationWithCredentials();

        // When
        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        // Then
        assertNotNull(factory);
    }

    // ========== Redis Template Tests ==========

    @Test
    void createRedisTemplate_ShouldCreateConfiguredTemplate() {
        // Given
        setupStandaloneConfiguration();

        // When
        RedisTemplate<String, ?> template = cacheConfig.createRedisTemplate();

        // Then
        assertNotNull(template);
        assertNotNull(template.getConnectionFactory());
    }

    // ========== Cache Manager Tests ==========

    @Test
    void cacheManager_ShouldCreateCacheManager() {
        // Given
        setupStandaloneConfigurationWithTTL();

        // When
        CacheManager cacheManager = cacheConfig.cacheManager();

        // Then
        assertNotNull(cacheManager);
    }

    // ========== Validation Tests ==========

    @Test
    void validateHosts_WithNullList_ShouldThrowException() {
        // Given
        setupConfigurationWithNullHosts();

        // When & Then
        Exception exception = assertThrows(Exception.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertNotNull(exception);
    }

    @Test
    void validateHosts_WithEmptyList_ShouldThrowException() {
        // Given
        setupConfigurationWithEmptyHosts();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("At least one host must be configured"));
    }

    @Test
    void validateHosts_WithEmptyHostName_ShouldThrowException() {
        // Given
        setupConfigurationWithEmptyHostName();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Host name cannot be empty"));
    }

    @Test
    void validateHosts_WithInvalidPortLow_ShouldThrowException() {
        // Given
        setupConfigurationWithInvalidPort(0);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Invalid port"));
    }

    @Test
    void validateHosts_WithInvalidPortHigh_ShouldThrowException() {
        // Given
        setupConfigurationWithInvalidPort(65536);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Invalid port"));
    }

    @Test
    void validateStandaloneHosts_WithMultipleHosts_ShouldThrowException() {
        // Given
        setupConfigurationWithMultipleHostsStandalone();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Standalone mode requires exactly one host entry"));
    }

    @Test
    void validateClusterHosts_WithSingleHost_ShouldThrowException() {
        // Given
        setupConfigurationWithSingleHostCluster();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Cluster mode requires host entries for proper redundancy"));
    }

    // ========== Helper Methods ==========

    private Map<String, Object> createBasicAttributesMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("mode", Mode.STANDALONE);
        map.put("useSsl", Boolean.FALSE);
        map.put("connectionTimeout", 5000L);
        map.put("readTimeout", 3000L);
        map.put("userName", "");
        map.put("pwd", "");
        map.put("mapperConfig", DefaultObjectMapperConfig.class);

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[1];
        AnnotationAttributes hostEntry = new AnnotationAttributes();
        hostEntry.put("host", "localhost");
        hostEntry.put("port", 6379);
        hostEntries[0] = hostEntry;
        map.put("hostEntries", hostEntries);

        map.put("ttlEntries", new AnnotationAttributes[0]);

        return map;
    }

    private void setupStandaloneConfiguration() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.putAll(attributesMap);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(metadataMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupClusterConfiguration() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.CLUSTER);

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[2];
        AnnotationAttributes hostEntry1 = new AnnotationAttributes();
        hostEntry1.put("host", "localhost");
        hostEntry1.put("port", 6379);
        hostEntries[0] = hostEntry1;

        AnnotationAttributes hostEntry2 = new AnnotationAttributes();
        hostEntry2.put("host", "localhost");
        hostEntry2.put("port", 6380);
        hostEntries[1] = hostEntry2;

        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupStandaloneConfigurationWithTTL() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        AnnotationAttributes[] ttlEntries = new AnnotationAttributes[1];
        AnnotationAttributes ttlEntry = new AnnotationAttributes();
        ttlEntry.put("name", "testCache");
        ttlEntry.put("ttl", 60000L);
        ttlEntries[0] = ttlEntry;
        attributesMap.put("ttlEntries", ttlEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupConfigurationWithNullHosts() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("hostEntries", null);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupConfigurationWithEmptyHosts() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        attributesMap.put("hostEntries", new AnnotationAttributes[0]);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupConfigurationWithEmptyHostName() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[1];
        AnnotationAttributes hostEntry = new AnnotationAttributes();
        hostEntry.put("host", "");
        hostEntry.put("port", 6379);
        hostEntries[0] = hostEntry;
        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupConfigurationWithInvalidPort(int port) {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[1];
        AnnotationAttributes hostEntry = new AnnotationAttributes();
        hostEntry.put("host", "localhost");
        hostEntry.put("port", port);

        hostEntries[0] = hostEntry;
        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupConfigurationWithMultipleHostsStandalone() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.STANDALONE);

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[2];
        AnnotationAttributes hostEntry1 = new AnnotationAttributes();
        hostEntry1.put("host", "localhost");
        hostEntry1.put("port", 6379);
        hostEntries[0] = hostEntry1;

        AnnotationAttributes hostEntry2 = new AnnotationAttributes();
        hostEntry2.put("host", "localhost");
        hostEntry2.put("port", 6380);
        hostEntries[1] = hostEntry2;

        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupConfigurationWithSingleHostCluster() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.CLUSTER);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupStandaloneConfigurationWithCredentials() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        attributesMap.put("userName", "testuser");
        attributesMap.put("pwd", "testpassword");

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupClusterConfigurationWithCredentials() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.CLUSTER);
        attributesMap.put("userName", "testuser");
        attributesMap.put("pwd", "testpassword");

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[2];
        AnnotationAttributes hostEntry1 = new AnnotationAttributes();
        hostEntry1.put("host", "localhost");
        hostEntry1.put("port", 6379);
        hostEntries[0] = hostEntry1;

        AnnotationAttributes hostEntry2 = new AnnotationAttributes();
        hostEntry2.put("host", "localhost");
        hostEntry2.put("port", 6380);
        hostEntries[1] = hostEntry2;

        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    // ========== Sentinel Mode Tests ==========

    @Test
    void createRedisConnectionFactory_WithSentinelMode_ShouldCreateSentinelConfiguration() {
        // Given
        setupSentinelConfiguration();

        // When
        RedisConnectionFactory result = cacheConfig.createRedisConnectionFactory();

        // Then
        assertNotNull(result);
    }

    @Test
    void createRedisConnectionFactory_WithSentinelModeAndCredentials_ShouldConfigureCredentials() {
        // Given
        setupSentinelConfigurationWithCredentials();

        // When
        RedisConnectionFactory result = cacheConfig.createRedisConnectionFactory();

        // Then
        assertNotNull(result);
    }

    @Test
    void createSentinelConfig_WithoutSentinelMaster_ShouldThrowException() {
        // Given
        setupSentinelConfigurationWithoutMaster();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());

        // Verify exception is thrown (message may vary depending on Spring internals)
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("sentinelMaster"));
    }

    @Test
    void createSentinelConfig_WithEmptySentinelMaster_ShouldThrowException() {
        // Given
        setupSentinelConfigurationWithEmptyMaster();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());

        assertEquals("sentinelMaster must be configured when using SENTINEL mode",
                exception.getMessage());
    }

    @Test
    void createSentinelConfig_WithValidConfiguration_ShouldCreateConfiguration() {
        // Given
        setupSentinelConfiguration();

        // When
        RedisConnectionFactory result = cacheConfig.createRedisConnectionFactory();

        // Then
        assertNotNull(result);
    }

    // ========== Sentinel Helper Methods ==========

    private void setupSentinelConfiguration() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.SENTINEL);
        attributesMap.put("sentinelMaster", "mymaster");

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[3];
        AnnotationAttributes hostEntry1 = new AnnotationAttributes();
        hostEntry1.put("host", "sentinel1");
        hostEntry1.put("port", 26379);
        hostEntries[0] = hostEntry1;

        AnnotationAttributes hostEntry2 = new AnnotationAttributes();
        hostEntry2.put("host", "sentinel2");
        hostEntry2.put("port", 26379);
        hostEntries[1] = hostEntry2;

        AnnotationAttributes hostEntry3 = new AnnotationAttributes();
        hostEntry3.put("host", "sentinel3");
        hostEntry3.put("port", 26379);
        hostEntries[2] = hostEntry3;

        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupSentinelConfigurationWithCredentials() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.SENTINEL);
        attributesMap.put("sentinelMaster", "mymaster");
        attributesMap.put("userName", "testuser");
        attributesMap.put("pwd", "testpassword");

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[3];
        AnnotationAttributes hostEntry1 = new AnnotationAttributes();
        hostEntry1.put("host", "sentinel1");
        hostEntry1.put("port", 26379);
        hostEntries[0] = hostEntry1;

        AnnotationAttributes hostEntry2 = new AnnotationAttributes();
        hostEntry2.put("host", "sentinel2");
        hostEntry2.put("port", 26379);
        hostEntries[1] = hostEntry2;

        AnnotationAttributes hostEntry3 = new AnnotationAttributes();
        hostEntry3.put("host", "sentinel3");
        hostEntry3.put("port", 26379);
        hostEntries[2] = hostEntry3;

        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupSentinelConfigurationWithoutMaster() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.SENTINEL);
        attributesMap.put("sentinelMaster", null);  // Explicitly set to null

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[2];
        AnnotationAttributes hostEntry1 = new AnnotationAttributes();
        hostEntry1.put("host", "sentinel1");
        hostEntry1.put("port", 26379);
        hostEntries[0] = hostEntry1;

        AnnotationAttributes hostEntry2 = new AnnotationAttributes();
        hostEntry2.put("host", "sentinel2");
        hostEntry2.put("port", 26379);
        hostEntries[1] = hostEntry2;

        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    private void setupSentinelConfigurationWithEmptyMaster() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.SENTINEL);
        attributesMap.put("sentinelMaster", "");  // Empty string

        AnnotationAttributes[] hostEntries = new AnnotationAttributes[2];
        AnnotationAttributes hostEntry1 = new AnnotationAttributes();
        hostEntry1.put("host", "sentinel1");
        hostEntry1.put("port", 26379);
        hostEntries[0] = hostEntry1;

        AnnotationAttributes hostEntry2 = new AnnotationAttributes();
        hostEntry2.put("host", "sentinel2");
        hostEntry2.put("port", 26379);
        hostEntries[1] = hostEntry2;

        attributesMap.put("hostEntries", hostEntries);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }
}
