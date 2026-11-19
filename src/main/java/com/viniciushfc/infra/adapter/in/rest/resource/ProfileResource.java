package com.viniciushfc.infra.adapter.in.rest.resource;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.usecases.impl.AnalyzePendingJobsUseCaseImpl;
import com.viniciushfc.domain.usecases.contracts.*;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.ClaudeStatus;
import com.viniciushfc.domain.vo.MailFolder;
import com.viniciushfc.domain.vo.ProcessingStatus;
import com.viniciushfc.domain.vo.ScraperActivity;
import com.viniciushfc.infra.adapter.in.rest.dto.*;
import com.viniciushfc.infra.adapter.in.rest.mapper.JobMatchRestMapper;
import com.viniciushfc.infra.adapter.in.rest.mapper.ProfileRestMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    private final CreateProfileUseCase createProfileUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final DeleteProfileUseCase deleteProfileUseCase;
    private final LinkAccountUseCase linkAccountUseCase;
    private final UnlinkAccountUseCase unlinkAccountUseCase;
    private final ListEmailFoldersUseCase listEmailFoldersUseCase;
    private final UpdateEmailFolderUseCase updateEmailFolderUseCase;
    private final ListJobMatchesUseCase listJobMatchesUseCase;
    private final GetProcessingStatusUseCase getProcessingStatusUseCase;
    private final GetScraperActivityUseCase getScraperActivityUseCase;
    private final OpenLinkedinLoginUseCase openLinkedinLoginUseCase;
    private final GenerateCurriculumUseCase generateCurriculumUseCase;
    private final ListPendingJobsUseCase listPendingJobsUseCase;
    private final PrioritizePendingJobUseCase prioritizePendingJobUseCase;
    private final ConnectClaudeUseCase connectClaudeUseCase;

    public ProfileResource(CreateProfileUseCase createProfileUseCase,
                           GetProfileUseCase getProfileUseCase,
                           UpdateProfileUseCase updateProfileUseCase,
                           DeleteProfileUseCase deleteProfileUseCase,
                           LinkAccountUseCase linkAccountUseCase,
                           UnlinkAccountUseCase unlinkAccountUseCase,
                           ListEmailFoldersUseCase listEmailFoldersUseCase,
                           UpdateEmailFolderUseCase updateEmailFolderUseCase,
                           ListJobMatchesUseCase listJobMatchesUseCase,
                           GetProcessingStatusUseCase getProcessingStatusUseCase,
                           GetScraperActivityUseCase getScraperActivityUseCase,
                           OpenLinkedinLoginUseCase openLinkedinLoginUseCase,
                           GenerateCurriculumUseCase generateCurriculumUseCase,
                           ListPendingJobsUseCase listPendingJobsUseCase,
                           PrioritizePendingJobUseCase prioritizePendingJobUseCase,
                           ConnectClaudeUseCase connectClaudeUseCase) {
        this.createProfileUseCase = createProfileUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.deleteProfileUseCase = deleteProfileUseCase;
        this.linkAccountUseCase = linkAccountUseCase;
        this.unlinkAccountUseCase = unlinkAccountUseCase;
        this.listEmailFoldersUseCase = listEmailFoldersUseCase;
        this.updateEmailFolderUseCase = updateEmailFolderUseCase;
        this.listJobMatchesUseCase = listJobMatchesUseCase;
        this.getProcessingStatusUseCase = getProcessingStatusUseCase;
        this.getScraperActivityUseCase = getScraperActivityUseCase;
        this.openLinkedinLoginUseCase = openLinkedinLoginUseCase;
        this.generateCurriculumUseCase = generateCurriculumUseCase;
        this.listPendingJobsUseCase = listPendingJobsUseCase;
        this.prioritizePendingJobUseCase = prioritizePendingJobUseCase;
        this.connectClaudeUseCase = connectClaudeUseCase;
    }

    @GET
    public ProfileResponse get() {
        return ProfileRestMapper.toResponse(getProfileUseCase.execute());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response create(ProfileForm form) {
        var created = createProfileUseCase.execute(ProfileRestMapper.toDomain(form));
        return Response.status(Response.Status.CREATED)
                .entity(ProfileRestMapper.toResponse(created))
                .build();
    }

    @PATCH
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public ProfileResponse update(ProfileForm form) {
        return ProfileRestMapper.toResponse(updateProfileUseCase.execute(ProfileRestMapper.toDomain(form)));
    }

    @DELETE
    public Response delete() {
        deleteProfileUseCase.execute();
        return Response.noContent().build();
    }

    @GET
    @Path("/curriculum")
    @Produces("application/pdf")
    public Response curriculum() {
        Attachment curriculum = getProfileUseCase.execute().curriculum();
        if (curriculum == null) {
            throw new NotFoundException("Este perfil não tem currículo.");
        }
        return inline(curriculum);
    }

    private static Response inline(Attachment file) {
        String fileName = URLEncoder.encode(file.name(), StandardCharsets.UTF_8).replace("+", "%20");
        return Response.ok(file.content(), file.contentType())
                .header("Content-Disposition", "inline; filename*=UTF-8''" + fileName)
                .build();
    }

    @POST
    @Path("/accounts")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProfileResponse linkAccount(LinkedAccountRequest request) {
        return ProfileRestMapper.toResponse(linkAccountUseCase.execute(ProfileRestMapper.toDomain(request)));
    }

    @DELETE
    @Path("/accounts/{provider}")
    public ProfileResponse unlinkAccount(@PathParam("provider") ExternalProvider provider) {
        return ProfileRestMapper.toResponse(unlinkAccountUseCase.execute(provider));
    }

    @POST
    @Path("/accounts/LINKEDIN/session")
    public Response openLinkedinLogin() {
        openLinkedinLoginUseCase.execute();
        return Response.accepted().build();
    }

    @GET
    @Path("/accounts/CLAUDE/session")
    public ClaudeStatus claudeStatus() {
        return connectClaudeUseCase.status();
    }

    @POST
    @Path("/accounts/CLAUDE/session")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProfileResponse connectClaude(ClaudeLoginRequest request) {
        return ProfileRestMapper.toResponse(connectClaudeUseCase.execute(request == null ? null : request.method()));
    }

    @POST
    @Path("/accounts/CLAUDE/code")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submitClaudeCode(ClaudeCodeRequest request) {
        connectClaudeUseCase.submitCode(request == null ? null : request.code());
        return Response.noContent().build();
    }

    @GET
    @Path("/email/folders")
    public List<MailFolder> listEmailFolders() {
        return listEmailFoldersUseCase.execute();
    }

    @PUT
    @Path("/email/folder")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProfileResponse updateEmailFolder(EmailFolderRequest request) {
        return ProfileRestMapper.toResponse(updateEmailFolderUseCase.execute(
                request.source() == null ? AlertSource.LINKEDIN : request.source(), request.folder()));
    }

    @GET
    @Path("/job-matches")
    public List<JobMatchResponse> listJobMatches() {
        return listJobMatchesUseCase.execute().stream()
                .map(JobMatchRestMapper::toResponse)
                .toList();
    }

    @GET
    @Path("/job-matches/{id}/curriculum")
    @Produces("application/pdf")
    public Response curriculumPdf(@PathParam("id") UUID id) {
        return inline(generateCurriculumUseCase.pdf(id));
    }

    @POST
    @Path("/job-matches/{id}/curriculum")
    public JobMatchResponse generateCurriculum(@PathParam("id") UUID id) {
        return JobMatchRestMapper.toResponse(generateCurriculumUseCase.execute(id));
    }

    @GET
    @Path("/pending-jobs")
    public PendingJobsResponse listPendingJobs() {
        return new PendingJobsResponse(
                listPendingJobsUseCase.execute().stream().map(JobMatchRestMapper::toResponse).toList(),
                getProcessingStatusUseCase.analysis(),
                AnalyzePendingJobsUseCaseImpl.MAX_ATTEMPTS);
    }

    @POST
    @Path("/pending-jobs/{id}/priority")
    public PendingJobResponse prioritizePendingJob(@PathParam("id") UUID id) {
        return JobMatchRestMapper.toResponse(prioritizePendingJobUseCase.execute(id));
    }

    @GET
    @Path("/status")
    public Map<AlertSource, ProcessingStatus> status() {
        return getProcessingStatusUseCase.execute();
    }

    @GET
    @Path("/scraper")
    public Map<AlertSource, ScraperActivity> scraper() {
        return getScraperActivityUseCase.execute();
    }

    @GET
    @Path("/scraper/screenshot")
    @Produces("image/png")
    public byte[] scraperScreenshot() {
        return getScraperActivityUseCase.failureScreenshot();
    }
}
