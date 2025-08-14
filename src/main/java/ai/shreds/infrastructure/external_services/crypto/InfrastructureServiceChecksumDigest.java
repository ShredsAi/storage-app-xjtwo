package ai.shreds.infrastructure.external_services.crypto;

import ai.shreds.domain.ports.DomainServiceChecksum;
import ai.shreds.shared.enums.SharedEnumChecksumAlgorithm;
import ai.shreds.shared.value_objects.SharedValueChecksum;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class InfrastructureServiceChecksumDigest implements DomainServiceChecksum {

    @Override
    public SharedValueChecksum compute(byte[] bytes, SharedEnumChecksumAlgorithm algorithm) {
        String algoName;
        switch (algorithm) {
            case MD5:
                algoName = "MD5";
                break;
            case SHA_1:
                algoName = "SHA-1";
                break;
            case SHA_256:
                algoName = "SHA-256";
                break;
            case SHA_512:
                algoName = "SHA-512";
                break;
            default:
                algoName = algorithm.name();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algoName);
            byte[] hash = digest.digest(bytes);
            String hex = Hex.encodeHexString(hash);
            return new SharedValueChecksum(algorithm.name(), hex);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unsupported checksum algorithm: " + algorithm, e);
        }
    }
}
