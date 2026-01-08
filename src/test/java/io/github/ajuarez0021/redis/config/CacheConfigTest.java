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

    /** The annotation metadata. */
    @Mock
    private AnnotationMetadata annotationMetadata;

    /** The cache config. */
    private CacheConfig cacheConfig;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
    }

    /**
     * Redis health checker should create instance.
     */
    @SuppressWarnings("unchecked")
	@Test
    void redisHealthChecker_ShouldCreateInstance() {
        RedisTemplate<String, Object> mockRedisTemplate = mock(RedisTemplate.class);

        RedisHealthChecker result = cacheConfig.redisHealthChecker(mockRedisTemplate);

        assertNotNull(result);
        assertInstanceOf(RedisHealthChecker.class, result);
    }

    /**
     * Redis cache service should create instance.
     */
    @SuppressWarnings("unchecked")
	@Test
    void redisCacheService_ShouldCreateInstance() {
        RedisTemplate<String, Object> mockRedisTemplate = mock(RedisTemplate.class);

        RedisCacheService result = cacheConfig.redisCacheService(mockRedisTemplate);

        assertNotNull(result);
        assertInstanceOf(RedisCacheService.class, result);
    }

    /**
     * Cache operation builder factory should create instance.
     */
    @Test
    void cacheOperationBuilderFactory_ShouldCreateInstance() {
        RedisCacheService mockCacheService = mock(RedisCacheService.class);

        CacheOperationBuilder.Factory result = cacheConfig.cacheOperationBuilderFactory(mockCacheService);

        assertNotNull(result);
        assertInstanceOf(CacheOperationBuilder.Factory.class, result);
    }

    /**
     * Cache operation builder factory should create new builder instances.
     */
    @Test
    void cacheOperationBuilderFactory_ShouldCreateNewBuilderInstances() {
        RedisCacheService mockCacheService = mock(RedisCacheService.class);
        CacheOperationBuilder.Factory factory = cacheConfig.cacheOperationBuilderFactory(mockCacheService);

        CacheOperationBuilder<String> builder1 = factory.create();
        CacheOperationBuilder<String> builder2 = factory.create();

        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2, "Factory should create new instances");
    }



    /**
     * Sets the import metadata should set attributes.
     */
    @Test
    void setImportMetadata_ShouldSetAttributes() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        assertDoesNotThrow(() -> cacheConfig.setImportMetadata(annotationMetadata));
        verify(annotationMetadata).getAnnotationAttributes(EnableRedisLibrary.class.getName());
    }

    

    /**
     * Creates the redis connection factory with standalone mode should create standalone factory.
     */
    @Test
    void createRedisConnectionFactory_WithStandaloneMode_ShouldCreateStandaloneFactory() {
        setupStandaloneConfiguration();

        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        assertNotNull(factory);
    }

    /**
     * Creates the redis connection factory with cluster mode should create cluster factory.
     */
    @Test
    void createRedisConnectionFactory_WithClusterMode_ShouldCreateClusterFactory() {
        setupClusterConfiguration();

        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        assertNotNull(factory);
    }

    /**
     * Creates the redis connection factory with credentials should configure auth.
     */
    @Test
    void createRedisConnectionFactory_WithCredentials_ShouldConfigureAuth() {
        setupStandaloneConfigurationWithCredentials();

        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        assertNotNull(factory);
    }

    /**
     * Creates the cluster config with credentials should configure auth.
     */
    @Test
    void createClusterConfig_WithCredentials_ShouldConfigureAuth() {
        setupClusterConfigurationWithCredentials();

        RedisConnectionFactory factory = cacheConfig.createRedisConnectionFactory();

        assertNotNull(factory);
    }

    

    /**
     * Creates the redis template should create configured template.
     */
    @Test
    void createRedisTemplate_ShouldCreateConfiguredTemplate() {
        setupStandaloneConfiguration();

        RedisTemplate<String, ?> template = cacheConfig.createRedisTemplate(cacheConfig.createRedisConnectionFactory());

        assertNotNull(template);
        assertNotNull(template.getConnectionFactory());
    }



    /**
     * Cache manager should create cache manager.
     */
    @Test
    void cacheManager_ShouldCreateCacheManager() {
        setupStandaloneConfigurationWithTTL();

        CacheManager cacheManager = cacheConfig.cacheManager(cacheConfig.createRedisConnectionFactory());

        assertNotNull(cacheManager);
    }

   

    /**
     * Validate hosts with null list should throw exception.
     */
    @Test
    void validateHosts_WithNullList_ShouldThrowException() {
        setupConfigurationWithNullHosts();

        Exception exception = assertThrows(Exception.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertNotNull(exception);
    }

    /**
     * Validate hosts with empty list should throw exception.
     */
    @Test
    void validateHosts_WithEmptyList_ShouldThrowException() {
        setupConfigurationWithEmptyHosts();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("At least one host must be configured"));
    }

    /**
     * Validate hosts with empty host name should throw exception.
     */
    @Test
    void validateHosts_WithEmptyHostName_ShouldThrowException() {
        setupConfigurationWithEmptyHostName();

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Host name cannot be empty"));
    }

    /**
     * Validate hosts with invalid port low should throw exception.
     */
    @Test
    void validateHosts_WithInvalidPortLow_ShouldThrowException() {
        setupConfigurationWithInvalidPort(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Invalid port"));
    }

    /**
     * Validate hosts with invalid port high should throw exception.
     */
    @Test
    void validateHosts_WithInvalidPortHigh_ShouldThrowException() {
        setupConfigurationWithInvalidPort(65536);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Invalid port"));
    }

    /**
     * Validate standalone hosts with multiple hosts should throw exception.
     */
    @Test
    void validateStandaloneHosts_WithMultipleHosts_ShouldThrowException() {
        setupConfigurationWithMultipleHostsStandalone();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Standalone mode requires exactly one host entry"));
    }

    /**
     * Validate cluster hosts with single host should throw exception.
     */
    @Test
    void validateClusterHosts_WithSingleHost_ShouldThrowException() {
        setupConfigurationWithSingleHostCluster();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());
        assertTrue(exception.getMessage().contains("Cluster mode requires host entries for proper redundancy"));
    }

    

    /**
     * Creates the basic attributes map.
     *
     * @return the map
     */
    private Map<String, Object> createBasicAttributesMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("mode", Mode.STANDALONE);
        map.put("useSsl", Boolean.FALSE);
        map.put("connectionTimeout", 5000L);
        map.put("readTimeout", 3000L);
        map.put("userName", "");
        map.put("pwd", "");
        map.put("database", 0);
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

    /**
     * Setup standalone configuration.
     */
    private void setupStandaloneConfiguration() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.putAll(attributesMap);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(metadataMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    /**
     * Setup cluster configuration.
     */
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

    /**
     * Setup standalone configuration with TTL.
     */
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

    /**
     * Setup configuration with null hosts.
     */
    private void setupConfigurationWithNullHosts() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("hostEntries", null);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    /**
     * Setup configuration with empty hosts.
     */
    private void setupConfigurationWithEmptyHosts() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        attributesMap.put("hostEntries", new AnnotationAttributes[0]);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    /**
     * Setup configuration with empty host name.
     */
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

    /**
     * Sets the up configuration with invalid port.
     *
     * @param port the new up configuration with invalid port
     */
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

    /**
     * Setup configuration with multiple hosts standalone.
     */
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

    /**
     * Setup configuration with single host cluster.
     */
    private void setupConfigurationWithSingleHostCluster() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.CLUSTER);

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    /**
     * Setup standalone configuration with credentials.
     */
    private void setupStandaloneConfigurationWithCredentials() {
        Map<String, Object> attributesMap = createBasicAttributesMap();

        attributesMap.put("userName", "testuser");
        attributesMap.put("pwd", "testpassword");

        when(annotationMetadata.getAnnotationAttributes(EnableRedisLibrary.class.getName()))
                .thenReturn(attributesMap);

        cacheConfig.setImportMetadata(annotationMetadata);
    }

    /**
     * Setup cluster configuration with credentials.
     */
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

    

    /**
     * Creates the redis connection factory with sentinel mode should create sentinel configuration.
     */
    @Test
    void createRedisConnectionFactory_WithSentinelMode_ShouldCreateSentinelConfiguration() {
        setupSentinelConfiguration();

        RedisConnectionFactory result = cacheConfig.createRedisConnectionFactory();

        assertNotNull(result);
    }

    /**
     * Creates the redis connection factory with sentinel mode and credentials should configure credentials.
     */
    @Test
    void createRedisConnectionFactory_WithSentinelModeAndCredentials_ShouldConfigureCredentials() {
        setupSentinelConfigurationWithCredentials();

        RedisConnectionFactory result = cacheConfig.createRedisConnectionFactory();

        assertNotNull(result);
    }

    /**
     * Creates the sentinel config without sentinel master should throw exception.
     */
    @Test
    void createSentinelConfig_WithoutSentinelMaster_ShouldThrowException() {
        setupSentinelConfigurationWithoutMaster();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("sentinelMaster"));
    }

    /**
     * Creates the sentinel config with empty sentinel master should throw exception.
     */
    @Test
    void createSentinelConfig_WithEmptySentinelMaster_ShouldThrowException() {
        setupSentinelConfigurationWithEmptyMaster();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cacheConfig.createRedisConnectionFactory());

        assertEquals("sentinelMaster must be configured when using SENTINEL mode",
                exception.getMessage());
    }

    /**
     * Creates the sentinel config with valid configuration should create configuration.
     */
    @Test
    void createSentinelConfig_WithValidConfiguration_ShouldCreateConfiguration() {
        setupSentinelConfiguration();

        RedisConnectionFactory result = cacheConfig.createRedisConnectionFactory();

        assertNotNull(result);
    }

    /**
     * Setup sentinel configuration.
     */
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

    /**
     * Setup sentinel configuration with credentials.
     */
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

    /**
     * Setup sentinel configuration without master.
     */
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

    /**
     * Setup sentinel configuration with empty master.
     */
    private void setupSentinelConfigurationWithEmptyMaster() {
        Map<String, Object> attributesMap = createBasicAttributesMap();
        attributesMap.put("mode", Mode.SENTINEL);
        attributesMap.put("sentinelMaster", "");

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
