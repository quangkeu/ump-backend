package vn.ssdc.vnpt.umpexception.model;

/**
 * Created by Huy Hieu on 11/29/2016.
 */
public class ExceptionResponse {

    public int errorCode;
    public String errorMessage;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
