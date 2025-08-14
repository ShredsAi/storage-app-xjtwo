package ai.shreds.infrastructure.external_services.storage;

import ai.shreds.domain.ports.DomainOutputPortObjectStorage;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionObjectStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "storage", name = "profile", havingValue = "s3", matchIfMissing = true)
public class InfrastructureClientS3ObjectStorage implements DomainOutputPortObjectStorage {

    private final S3Client s3;
    private final String bucket;

    public InfrastructureClientS3ObjectStorage(S3Client s3,
                                               @Value("${storage.s3.bucket-name}") String bucketName) {
        this.s3 = s3;
        this.bucket = bucketName;
    }

    @Override
    public void putOriginal(String key, byte[] bytes, String contentType) {
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3.putObject(req, RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            throw new InfrastructureExceptionObjectStorage("S3 putObject failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new InfrastructureExceptionObjectStorage("S3 putObject failed", e);
        }
    }

    @Override
    public Optional<Long> headContentLength(String key) {
        try {
            HeadObjectRequest req = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return Optional.ofNullable(s3.headObject(req).contentLength());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw new InfrastructureExceptionObjectStorage("S3 headObject failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new InfrastructureExceptionObjectStorage("S3 headObject failed", e);
        }
    }

    @Override
    public void deleteObject(String key) {
        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3.deleteObject(req);
        } catch (S3Exception e) {
            throw new InfrastructureExceptionObjectStorage("S3 deleteObject failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new InfrastructureExceptionObjectStorage("S3 deleteObject failed", e);
        }
    }
}
