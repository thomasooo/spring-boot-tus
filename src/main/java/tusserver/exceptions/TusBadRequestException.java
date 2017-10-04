package tusserver.exceptions;

public class TusBadRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TusBadRequestException() {
        super("Bad request" );
    }

    public TusBadRequestException(String message) {
        super("Bad request: " + message);
    }
}
