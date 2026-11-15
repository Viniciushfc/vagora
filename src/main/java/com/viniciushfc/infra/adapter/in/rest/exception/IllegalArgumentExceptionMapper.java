package com.viniciushfc.infra.adapter.in.rest.exception;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.Map;

public class IllegalArgumentExceptionMapper {

    @ServerExceptionMapper
    public Response map(IllegalArgumentException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", e.getMessage()))
                .build();
    }
}
