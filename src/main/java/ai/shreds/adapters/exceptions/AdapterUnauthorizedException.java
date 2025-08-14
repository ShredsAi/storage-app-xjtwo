package ai.shreds.adapters.exceptions;

public class AdapterUnauthorizedException extends RuntimeException {
    private final String errorCode;

    public AdapterUnauthorizedException(String message) {
        super(message);
        this.errorCode = "UNAUTHORIZED";
    }

    public AdapterUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNAUTHORIZED";
    }

    public AdapterUnauthorizedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "UNAUTHORIZED";
    }

    public AdapterUnauthorizedException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "UNAUTHORIZED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
