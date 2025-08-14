package ai.shreds.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import io.minio.MinioClient;

import java.net.URI;

@Configuration
public class InfrastructureStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "profile", havingValue = "s3")
    public S3Client s3Client(
            @Value("${storage.s3.region:us-east-1}") String region,
            @Value("${storage.s3.endpoint-override:}") String endpointOverride,
            @Value("${awsAccessKey:}") String accessKey,
            @Value("${awsSecretKey:}") String secretKey
    ) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey))
                                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                                : DefaultCredentialsProvider.create()
                );
        if (StringUtils.hasText(endpointOverride)) {
            builder = builder.endpointOverride(URI.create(endpointOverride));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "profile", havingValue = "minio")
    public MinioClient minioClient(
            @Value("${storage.minio.endpoint}") String endpoint,
            @Value("${storage.minio.access-key}") String accessKey,
            @Value("${storage.minio.secret-key}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean(name = "objectStorageCircuitBreaker")
    @ConditionalOnMissingBean(name = "objectStorageCircuitBreaker")
    public Object circuitBreaker() {
        return new Object();
    }

    @Bean(name = "objectStorageBucketName")
    @ConditionalOnProperty(prefix = "storage", name = "profile", havingValue = "s3")
    public String s3BucketName(@Value("${storage.s3.bucket-name}") String bucket) {
        return bucket;
    }

    @Bean(name = "objectStorageBucketName")
    @ConditionalOnProperty(prefix = "storage", name = "profile", havingValue = "minio")
    public String minioBucketName(@Value("${storage.minio.bucket-name}") String bucket) {
        return bucket;
    }
}
