package tusserver.exceptions;

import java.util.Date;

@SuppressWarnings("unused")
public class TusException {

    private long timestamp;
    private String error;
    private String message;
    private String exception;

    public TusException(String err, Exception ex){
        this.timestamp = new Date().getTime();

        this.error = err;
        this.message = ex.getMessage();
        this.exception = ex.getClass().getCanonicalName();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
