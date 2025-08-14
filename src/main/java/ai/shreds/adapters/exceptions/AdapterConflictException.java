package ai.shreds.adapters.exceptions;

public class AdapterConflictException extends RuntimeException {
    private final String errorCode;

    public AdapterConflictException(String message) {
        super(message);
        this.errorCode = "CONFLICT";
    }

    public AdapterConflictException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CONFLICT";
    }

    public AdapterConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "CONFLICT";
    }

    public AdapterConflictException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "CONFLICT";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
