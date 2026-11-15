package com.viniciushfc.infra.adapter.in.rest.resource;

import com.viniciushfc.domain.usecases.contracts.*;
import com.viniciushfc.infra.adapter.in.rest.dto.JobMatchResponse;
import com.viniciushfc.infra.adapter.in.rest.dto.LinkedAccountRequest;
import com.viniciushfc.infra.adapter.in.rest.dto.UserAccountForm;
import com.viniciushfc.infra.adapter.in.rest.dto.UserAccountResponse;
import com.viniciushfc.infra.adapter.in.rest.mapper.JobMatchRestMapper;
import com.viniciushfc.infra.adapter.in.rest.mapper.UserAccountRestMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserAccountResource {

    private final CreateUserAccountUseCase createUserAccountUseCase;
    private final UpdateUserAccountUseCase updateUserAccountUseCase;
    private final DeleteUserAccountUseCase deleteUserAccountUseCase;
    private final LinkAccountUseCase linkAccountUseCase;
    private final ListJobMatchesUseCase listJobMatchesUseCase;

    public UserAccountResource(CreateUserAccountUseCase createUserAccountUseCase,
                               UpdateUserAccountUseCase updateUserAccountUseCase,
                               DeleteUserAccountUseCase deleteUserAccountUseCase,
                               LinkAccountUseCase linkAccountUseCase,
                               ListJobMatchesUseCase listJobMatchesUseCase) {
        this.createUserAccountUseCase = createUserAccountUseCase;
        this.updateUserAccountUseCase = updateUserAccountUseCase;
        this.deleteUserAccountUseCase = deleteUserAccountUseCase;
        this.linkAccountUseCase = linkAccountUseCase;
        this.listJobMatchesUseCase = listJobMatchesUseCase;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response create(UserAccountForm form) {
        var created = createUserAccountUseCase.execute(UserAccountRestMapper.toDomain(form));
        return Response.status(Response.Status.CREATED)
                .entity(UserAccountRestMapper.toResponse(created))
                .build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public UserAccountResponse update(@PathParam("id") UUID id, UserAccountForm form) {
        return UserAccountRestMapper.toResponse(updateUserAccountUseCase.execute(id, UserAccountRestMapper.toDomain(form)));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        deleteUserAccountUseCase.execute(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/linked-accounts")
    @Consumes(MediaType.APPLICATION_JSON)
    public UserAccountResponse linkAccount(@PathParam("id") UUID id, LinkedAccountRequest request) {
        return UserAccountRestMapper.toResponse(linkAccountUseCase.execute(id, UserAccountRestMapper.toDomain(request)));
    }

    @GET
    @Path("/{id}/job-matches")
    public List<JobMatchResponse> listJobMatches(@PathParam("id") UUID id) {
        return listJobMatchesUseCase.execute(id).stream()
                .map(JobMatchRestMapper::toResponse)
                .toList();
    }
}
