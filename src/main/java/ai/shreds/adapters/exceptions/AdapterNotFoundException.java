package ai.shreds.adapters.exceptions;

public class AdapterNotFoundException extends RuntimeException {
    private final String errorCode;

    public AdapterNotFoundException(String message) {
        super(message);
        this.errorCode = "NOT_FOUND";
    }

    public AdapterNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NOT_FOUND";
    }

    public AdapterNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "NOT_FOUND";
    }

    public AdapterNotFoundException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "NOT_FOUND";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
