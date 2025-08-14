package ai.shreds.adapters.exceptions;

public class AdapterInternalServerException extends RuntimeException {
    private final String errorCode;

    public AdapterInternalServerException(String message) {
        super(message);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public AdapterInternalServerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public AdapterInternalServerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "INTERNAL_SERVER_ERROR";
    }

    public AdapterInternalServerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "INTERNAL_SERVER_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
