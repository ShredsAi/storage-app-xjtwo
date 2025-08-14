package ai.shreds.adapters.exceptions;

import ai.shreds.shared.dtos.SharedDTOErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdapterRestExceptionHandler {

    @ExceptionHandler(AdapterBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SharedDTOErrorResponse handleBadRequest(AdapterBadRequestException ex) {
        return new SharedDTOErrorResponse(ex.getErrorCode(), ex.getMessage());
        
    }

    @ExceptionHandler(AdapterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SharedDTOErrorResponse handleNotFound(AdapterNotFoundException ex) {
        return new SharedDTOErrorResponse(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(AdapterConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public SharedDTOErrorResponse handleConflict(AdapterConflictException ex) {
        return new SharedDTOErrorResponse(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(AdapterUnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public SharedDTOErrorResponse handleUnauthorized(AdapterUnauthorizedException ex) {
        return new SharedDTOErrorResponse(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(AdapterForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public SharedDTOErrorResponse handleForbidden(AdapterForbiddenException ex) {
        return new SharedDTOErrorResponse(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(AdapterInternalServerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public SharedDTOErrorResponse handleInternal(AdapterInternalServerException ex) {
        return new SharedDTOErrorResponse(ex.getErrorCode(), ex.getMessage());
    }
}
