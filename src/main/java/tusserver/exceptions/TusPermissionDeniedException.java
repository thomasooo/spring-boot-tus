package tusserver.exceptions;

public class TusPermissionDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TusPermissionDeniedException(String message) {
        super("Permission denied: " + message);
    }

}
