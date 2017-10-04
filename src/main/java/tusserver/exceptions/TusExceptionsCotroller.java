package tusserver.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
class TusExceptionsController {

    @ExceptionHandler(value = { TusDBFileNotFoundException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public TusException VideoNotFoundExceptionHandler(TusDBFileNotFoundException e, WebRequest req) {
        e.printStackTrace();

        TusException err = new TusException("not found", e);
        return err;
    }

    @ExceptionHandler(value = { TusStorageException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public TusException TusStorageExceptionHandler(TusStorageException e, WebRequest req) {
        e.printStackTrace();

        TusException err = new TusException("not found", e);
        return err;
    }

    @ExceptionHandler(value = { TusPermissionDeniedException.class })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public TusException TusPermissionDeniedExceptionHandler(TusPermissionDeniedException e, WebRequest req) {
        e.printStackTrace();

        TusException err = new TusException("permission denied", e);
        return err;
    }

    @ExceptionHandler(value = { TusBadRequestException.class })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public TusException TusBadRequestExceptionHandler(TusBadRequestException e, WebRequest req) {
        e.printStackTrace();

        TusException err = new TusException("bad request", e);
        return err;
    }
}
