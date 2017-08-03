package vn.ssdc.vnpt.umpexception;

/**
 * Created by THANHLX on 5/11/2017.
 */
public class ForgotPasswordTokenExpiredException extends RuntimeException {
    public ForgotPasswordTokenExpiredException(String message) {
        super(message);
    }
}
