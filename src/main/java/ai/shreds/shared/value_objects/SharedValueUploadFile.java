package ai.shreds.shared.value_objects;

import java.util.Arrays;
import java.util.Objects;

public final class SharedValueUploadFile {
    private final String originalFileName;
    private final String contentType;
    private final long contentLength;
    private final byte[] bytes;

    public SharedValueUploadFile(String originalFileName, String contentType, long contentLength, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes must not be null or empty");
        }
        if (contentLength <= 0) {
            throw new IllegalArgumentException("contentLength must be positive");
        }
        if (contentLength != bytes.length) {
            // keep strict validation to avoid inconsistencies during checksum/metadata extraction
            throw new IllegalArgumentException("contentLength does not match bytes length");
        }
        this.originalFileName = originalFileName; // may be null
        this.contentType = contentType; // may be null; validated in higher layers
        this.contentLength = contentLength;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedValueUploadFile that = (SharedValueUploadFile) o;
        return contentLength == that.contentLength &&
                Objects.equals(originalFileName, that.originalFileName) &&
                Objects.equals(contentType, that.contentType) &&
                Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(originalFileName, contentType, contentLength);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }

    @Override
    public String toString() {
        return "SharedValueUploadFile{" +
                "originalFileName='" + originalFileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", contentLength=" + contentLength +
                ", bytesLength=" + bytes.length +
                '}';
    }
}
