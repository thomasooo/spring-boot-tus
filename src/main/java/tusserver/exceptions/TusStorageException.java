package tusserver.exceptions;

public class TusStorageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TusStorageException(String name) {
        super("File with name " + name + " not found");
    }

    public TusStorageException(String name, boolean bool) {
        super("Directory with name " + name + " not found");
    }
}
