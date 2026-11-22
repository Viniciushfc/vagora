package com.viniciushfc.infra.adapter.in.rest.resource;

import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.usecases.contracts.DeleteGupySearchUseCase;
import com.viniciushfc.domain.usecases.contracts.ListGupySearchesUseCase;
import com.viniciushfc.domain.usecases.contracts.SaveGupySearchUseCase;
import com.viniciushfc.infra.adapter.in.rest.dto.GupySearchRequest;
import com.viniciushfc.infra.adapter.in.rest.dto.GupySearchResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/gupy/searches")
@Produces(MediaType.APPLICATION_JSON)
public class GupySearchResource {

    private final ListGupySearchesUseCase listGupySearchesUseCase;
    private final SaveGupySearchUseCase saveGupySearchUseCase;
    private final DeleteGupySearchUseCase deleteGupySearchUseCase;

    public GupySearchResource(ListGupySearchesUseCase listGupySearchesUseCase, SaveGupySearchUseCase saveGupySearchUseCase,
                              DeleteGupySearchUseCase deleteGupySearchUseCase) {
        this.listGupySearchesUseCase = listGupySearchesUseCase;
        this.saveGupySearchUseCase = saveGupySearchUseCase;
        this.deleteGupySearchUseCase = deleteGupySearchUseCase;
    }

    @GET
    public List<GupySearchResponse> list() {
        return listGupySearchesUseCase.execute().stream().map(GupySearchResponse::from).toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public GupySearchResponse create(GupySearchRequest request) {
        return GupySearchResponse.from(saveGupySearchUseCase.execute(toDomain(null, request)));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public GupySearchResponse update(@PathParam("id") UUID id, GupySearchRequest request) {
        return GupySearchResponse.from(saveGupySearchUseCase.execute(toDomain(id, request)));
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") UUID id) {
        deleteGupySearchUseCase.execute(id);
    }

    private static GupySearch toDomain(UUID id, GupySearchRequest request) {
        if (request == null) {
            throw new ValidationException("Envie os dados da busca.");
        }
        return new GupySearch(id, null, request.jobName(), request.state(), request.city(), request.workplaces(),
                request.contracts(), request.everyHours() == null ? GupySearch.DEFAULT_EVERY_HOURS : request.everyHours(),
                request.enabled() == null || request.enabled(), null);
    }
}
