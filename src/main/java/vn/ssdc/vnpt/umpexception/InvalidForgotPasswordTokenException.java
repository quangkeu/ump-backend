package vn.ssdc.vnpt.umpexception;

/**
 * Created by THANHLX on 5/11/2017.
 */
public class InvalidForgotPasswordTokenException extends RuntimeException {
    public InvalidForgotPasswordTokenException(String message) {
        super(message);
    }
}
