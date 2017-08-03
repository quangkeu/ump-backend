package vn.ssdc.vnpt.umpexception;

/**
 * Created by kiendt on 2/10/2017.
 */
public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String message) {
        super(message);
    }
}
