package fit.iuh.kh3tshopbe.exception;

public class GeminiRetryableException extends RuntimeException {

    public GeminiRetryableException(String message) {
        super(message);
    }

    public GeminiRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}