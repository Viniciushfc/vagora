package com.viniciushfc.infra.adapter.in.rest.filter;

import com.viniciushfc.infra.adapter.in.rest.dto.ErrorResponse;
import com.viniciushfc.infra.adapter.in.rest.dto.ErrorType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.net.URI;
import java.util.Set;

public class LocalOriginFilter {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    @ServerRequestFilter
    public Response filter(ContainerRequestContext request) {
        String origin = request.getHeaderString("Origin");
        if (isLocal(hostOf(request.getHeaderString("Host"))) && (origin == null || isLocal(originHost(origin)))) {
            return null;
        }
        int status = Response.Status.FORBIDDEN.getStatusCode();
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(ErrorType.HTTP_ERROR, "O Vagora só aceita pedidos da própria máquina.",
                        status, request.getUriInfo().getPath()))
                .build();
    }

    private static boolean isLocal(String host) {
        return host != null && LOCAL_HOSTS.contains(host.toLowerCase());
    }

    static String hostOf(String hostHeader) {
        if (hostHeader == null) {
            return null;
        }
        int end = hostHeader.startsWith("[") ? hostHeader.indexOf(']') + 1 : hostHeader.indexOf(':');
        return end > 0 ? hostHeader.substring(0, end) : hostHeader;
    }

    private static String originHost(String origin) {
        try {
            return URI.create(origin).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
