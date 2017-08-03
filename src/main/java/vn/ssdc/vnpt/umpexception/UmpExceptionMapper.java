package vn.ssdc.vnpt.umpexception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.umpexception.model.ExceptionResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by Huy Hieu on 11/29/2016.
 */
@Provider
public class UmpExceptionMapper implements ExceptionMapper<UmpException> {
    private int code = 100;

    @Override
    public Response toResponse(UmpException e) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setErrorCode(code);
        exceptionResponse.setErrorMessage(e.getMessage().toString());
        return Response.status(code).entity(exceptionResponse).type("application/json").build();
    }
}
