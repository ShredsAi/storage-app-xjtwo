package ai.shreds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class ApplicationStartupTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14.5")
            .withDatabaseName("image_ingestion_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2023-09-04T19-57-37Z"))
            .withExposedPorts(9000, 9001)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data", "--console-address", ":9001");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // Security configuration - disable JWT validation for startup test
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/.well-known/jwks.json");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/dummy");
        
        // Storage configuration
        registry.add("storage.profile", () -> "minio");
        registry.add("storage.minio.endpoint", () -> "http://localhost:" + MINIO.getMappedPort(9000));
        registry.add("storage.minio.access-key", () -> "minioadmin");
        registry.add("storage.minio.secret-key", () -> "minioadmin");
        registry.add("storage.minio.bucket-name", () -> "image-storage");
        
        // Server configuration
        registry.add("server.port", () -> 0);
        
        // Disable scheduling for tests
        registry.add("cleanup.cron", () -> "-");
        
        // Logging configuration
        registry.add("logging.level.ai.shreds", () -> "DEBUG");
        registry.add("logging.level.org.springframework.boot", () -> "INFO");
        registry.add("logging.level.org.testcontainers", () -> "INFO");
    }

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class TestSecurityOverrides {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> { 
                throw new UnsupportedOperationException("JWT decoding not needed in startup test"); 
            };
        }
    }

    @Test
    void contextStarts_andLogsContainStartedMessage(CapturedOutput output) {
        // Verify application context is properly initialized
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.isActive()).isTrue();
        
        // Verify server is running on a random port
        assertThat(port).isGreaterThan(0);
        
        // Capture and analyze logs
        String logs = output.getOut() + "\n" + output.getErr();
        
        // Check for successful startup indicators
        boolean startedIndicator = logs.contains("Tomcat started on port(s):")
                || logs.contains("Started ImageIngestionManagementApplication")
                || logs.contains("Started ApplicationStartupTest");
        
        assertThat(startedIndicator)
                .as("Startup logs should indicate the application has started (Tomcat started or Started <class> message). Logs were: %s", logs)
                .isTrue();
        
        // Verify no critical errors in logs
        assertThat(logs)
                .as("Logs should not contain critical errors. Logs were: %s", logs)
                .doesNotContain("ERROR", "FATAL", "Exception in thread", "Failed to start");
        
        // Verify database connection was established
        boolean dbConnectionEstablished = logs.contains("HikariPool") 
                || logs.contains("Database connection")
                || logs.contains("Initialized JPA EntityManagerFactory");
        
        assertThat(dbConnectionEstablished)
                .as("Database connection should be established. Logs were: %s", logs)
                .isTrue();
        
        // Log the full output for debugging purposes
        System.out.println("=== APPLICATION STARTUP TEST LOGS ===");
        System.out.println(logs);
        System.out.println("=== END OF LOGS ===");
        System.out.println("Application started successfully on port: " + port);
        System.out.println("PostgreSQL container running on: " + POSTGRES.getJdbcUrl());
        System.out.println("MinIO container running on: http://localhost:" + MINIO.getMappedPort(9000));
    }
}
