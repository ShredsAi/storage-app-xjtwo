package ai.shreds.adapters.exceptions;

public class AdapterForbiddenException extends RuntimeException {
    private final String errorCode;

    public AdapterForbiddenException(String message) {
        super(message);
        this.errorCode = "FORBIDDEN";
    }

    public AdapterForbiddenException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FORBIDDEN";
    }

    public AdapterForbiddenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "FORBIDDEN";
    }

    public AdapterForbiddenException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "FORBIDDEN";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
