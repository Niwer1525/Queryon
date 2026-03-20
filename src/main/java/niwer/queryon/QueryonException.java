package niwer.queryon;

public class QueryonException extends RuntimeException {
    public QueryonException(String message) {
        super(message);
    }

    public QueryonException(String message, Throwable cause) {
        super(message, cause);
    }
}
