package ai.shreds.adapters.exceptions;

public class AdapterBadRequestException extends RuntimeException {
    private final String errorCode;

    public AdapterBadRequestException(String message) {
        super(message);
        this.errorCode = "BAD_REQUEST";
    }

    public AdapterBadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BAD_REQUEST";
    }

    public AdapterBadRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "BAD_REQUEST";
    }

    public AdapterBadRequestException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "BAD_REQUEST";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
