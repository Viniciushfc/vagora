package com.viniciushfc.infra.adapter.in.rest.exception;

import com.viniciushfc.domain.exceptions.*;
import com.viniciushfc.infra.adapter.in.rest.dto.ErrorResponse;
import com.viniciushfc.infra.adapter.in.rest.dto.ErrorType;
import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.UUID;

public class DomainExceptionMapper {

    @ServerExceptionMapper
    public Response map(DomainException e, UriInfo uriInfo) {
        ErrorType type = switch (e) {
            case NotFoundException ignored -> ErrorType.NOT_FOUND;
            case ValidationException ignored -> ErrorType.VALIDATION_ERROR;
            case ConflictException ignored -> ErrorType.CONFLICT;
            case BusinessException ignored -> ErrorType.BUSINESS_RULE;
            case ExternalServiceException ignored -> ErrorType.EXTERNAL_SERVICE_ERROR;
        };

        if (e instanceof ExternalServiceException) {
            Log.warnf(e, "Falha em serviço externo em %s", path(uriInfo));
        }

        return build(ErrorResponse.of(type, e.getMessage(), type.status, path(uriInfo), e.code()));
    }

    @ServerExceptionMapper
    public Response map(WebApplicationException e, UriInfo uriInfo) {
        int status = e.getResponse().getStatus();
        return build(ErrorResponse.of(ErrorType.HTTP_ERROR, Response.Status.fromStatusCode(status) != null
                ? Response.Status.fromStatusCode(status).getReasonPhrase() : e.getMessage(), status, path(uriInfo)));
    }

    @ServerExceptionMapper
    public Response map(Exception e, UriInfo uriInfo) {
        String errorId = UUID.randomUUID().toString();
        Log.errorf(e, "Erro inesperado [%s] em %s", errorId, path(uriInfo));
        return build(ErrorResponse.unexpected(path(uriInfo), errorId));
    }


    private String path(UriInfo uriInfo) {
        return uriInfo == null ? null : "/" + uriInfo.getPath().replaceFirst("^/", "");
    }

    private Response build(ErrorResponse body) {
        return Response.status(body.status()).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
