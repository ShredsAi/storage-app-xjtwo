package ai.shreds.shared.dtos;

import java.util.Objects;

public class SharedDTOErrorResponse {
    private String errorCode;
    private String message;

    public SharedDTOErrorResponse() {
    }

    public SharedDTOErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedDTOErrorResponse that = (SharedDTOErrorResponse) o;
        return Objects.equals(errorCode, that.errorCode) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, message);
    }

    @Override
    public String toString() {
        return "SharedDTOErrorResponse{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
