package tusserver.exceptions;

public class TusDBFileNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TusDBFileNotFoundException(String uuid) {
        super("File with uuid " + uuid + " not found");
    }

}
