package ai.shreds.infrastructure.external_services.storage;

import ai.shreds.domain.ports.DomainOutputPortObjectStorage;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionObjectStorage;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "storage", name = "profile", havingValue = "minio")
public class InfrastructureClientMinioObjectStorage implements DomainOutputPortObjectStorage {

    private final MinioClient minio;
    private final String bucket;

    public InfrastructureClientMinioObjectStorage(MinioClient minioClient,
                                                  @Value("${storage.minio.bucket-name}") String bucketName) {
        this.minio = minioClient;
        this.bucket = bucketName;
    }

    @Override
    public void putOriginal(String key, byte[] bytes, String contentType) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(bais, bytes.length, -1)
                    .contentType(contentType)
                    .build();
            minio.putObject(args);
        } catch (Exception e) {
            throw new InfrastructureExceptionObjectStorage("MinIO putObject failed", e);
        }
    }

    @Override
    public Optional<Long> headContentLength(String key) {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            return Optional.of(minio.statObject(args).size());
        } catch (ErrorResponseException e) {
            if (e.response() != null && e.response().code() == 404) {
                return Optional.empty();
            }
            throw new InfrastructureExceptionObjectStorage("MinIO statObject failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new InfrastructureExceptionObjectStorage("MinIO statObject failed", e);
        }
    }

    @Override
    public void deleteObject(String key) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            minio.removeObject(args);
        } catch (Exception e) {
            throw new InfrastructureExceptionObjectStorage("MinIO removeObject failed", e);
        }
    }
}
