package com.viniciushfc.infra.adapter.in.rest;

import static org.mockito.ArgumentMatchers.argThat;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.MailFolder;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheProfileRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProfileResourceTest {
    @Inject
    JobMatchRepository jobMatchRepository;

    @Inject
    PendingJobRepository pendingJobRepository;

    @Inject
    PanacheProfileRepository panacheProfileRepository;

    @InjectMock
    MailboxGateway mailboxGateway;

    @InjectMock
    PdfGateway pdfGateway;

    @BeforeEach
    void setUp() {
        QuarkusTransaction.requiringNew().run(() -> panacheProfileRepository.listAll().forEach(panacheProfileRepository::delete));
        when(mailboxGateway.listFolders(any())).thenReturn(List.of(
                new MailFolder("INBOX", Map.of(AlertSource.LINKEDIN, 0)), new MailFolder("LinkedIn", Map.of(AlertSource.LINKEDIN, 7))));
        when(mailboxGateway.folderExists(any(), argThat(f -> List.of("INBOX", "LinkedIn").contains(f)))).thenReturn(true);
        when(pdfGateway.extractTextPdf(any())).thenReturn("Desenvolvedor Java");
    }

    private String createProfile() {
        return given()
                .multiPart("name", "Fulano")
                .multiPart("geminiApiKey", "chave-gemini")
                .multiPart("curriculum", "cv.pdf", new byte[200_000], "application/pdf")
                .when().post("/profile")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Fulano"))
                .body("hasGeminiApiKey", is(true))
                .body("hasGroqApiKey", is(false))
                .body("llmProvider", is("GEMINI"))
                .body("curriculumName", is("cv.pdf"))
                .body("linkedAccounts", empty())
                .body("$", not(hasKey("password")))
                .body("$", not(hasKey("username")))
                .body("$", not(hasKey("geminiApiKey")))
                .extract().path("id");
    }

    private String accountBody(String login, String provider, String folder) {
        String folderJson = folder == null ? "null" : "\"" + folder + "\"";
        return "{\"username\":\"" + login + "\",\"password\":\"x\",\"externalProvider\":\"" + provider + "\",\"folder\":" + folderJson + "}";
    }

    private void link(String body, int status) {
        given().contentType(ContentType.JSON).body(body).when().post("/profile/accounts").then().statusCode(status);
    }

    @Test
    void semPerfilRespondeNotFound() {
        given().when().get("/profile")
                .then()
                .statusCode(404)
                .body("success", is(false))
                .body("type_error", is("NOT_FOUND"))
                .body("message_error", is("Nenhum perfil configurado."))
                .body("path", is("/profile"))
                .body("timestamp", notNullValue())
                .body("$", not(hasKey("error_id")));
    }

    @Test
    void criaEBuscaOPerfil() {
        String id = createProfile();

        given().when().get("/profile").then().statusCode(200).body("id", is(id));
    }

    @Test
    void recusaUmSegundoPerfil() {
        createProfile();

        given().multiPart("name", "Outro").multiPart("curriculum", "cv.pdf", new byte[10], "application/pdf")
                .when().post("/profile")
                .then()
                .statusCode(409)
                .body("type_error", is("CONFLICT"))
                .body("message_error", is("Já existe um perfil nesta instalação."));
    }

    @Test
    void atualizaSomenteCamposEnviados() {
        createProfile();

        given().multiPart("name", "Novo Nome").when().patch("/profile")
                .then()
                .statusCode(200)
                .body("name", is("Novo Nome"))
                .body("hasGeminiApiKey", is(true))
                .body("curriculumName", is("cv.pdf"));

        given().multiPart("groqApiKey", "gsk-chave").multiPart("llmProvider", "GROQ").when().patch("/profile")
                .then()
                .statusCode(200)
                .body("hasGroqApiKey", is(true))
                .body("llmProvider", is("GROQ"))
                .body("hasGeminiApiKey", is(true))
                .body("$", not(hasKey("groqApiKey")));
    }

    @Test
    void atualizarSemPerfilRetorna404() {
        given().multiPart("name", "x").when().patch("/profile").then().statusCode(404);
    }

    @Test
    void ligaODesligaNavegadorEMonitoramento() {
        createProfile();
        given().when().get("/profile").then().body("showBrowser", is(false)).body("monitoringEnabled", is(true));

        given().multiPart("showBrowser", "true").when().patch("/profile").then().statusCode(200).body("showBrowser", is(true));
        given().multiPart("monitoringEnabled", "false").when().patch("/profile")
                .then().statusCode(200).body("monitoringEnabled", is(false)).body("name", is("Fulano"));
    }

    @Test
    void curriculoAutomaticoComecaDesligadoEPodeLigar() {
        createProfile();
        given().when().get("/profile").then().body("autoCurriculum", is(false));

        given().multiPart("autoCurriculum", "true").when().patch("/profile")
                .then().statusCode(200).body("autoCurriculum", is(true));
    }

    @Test
    void devolveOCurriculoEOTamanho() {
        createProfile();

        given().when().get("/profile").then().body("curriculumSizeBytes", is(200_000));
        given().when().get("/profile/curriculum")
                .then()
                .statusCode(200)
                .contentType("application/pdf")
                .header("Content-Disposition", is("inline; filename*=UTF-8''cv.pdf"));
    }

    @Test
    void rejeitaPdfSemTexto() {
        when(pdfGateway.extractTextPdf(any())).thenReturn("");

        given().multiPart("name", "F").multiPart("curriculum", "cv.pdf", new byte[10], "application/pdf")
                .when().post("/profile")
                .then().statusCode(400).body("message_error", containsString("texto selecionável"));
    }

    @Test
    void listaEPriorizaAsVagasPendentes() {
        UUID profileId = UUID.fromString(createProfile());
        var job = new JobDetail("https://www.linkedin.com/jobs/view/7/", "Dev Java", "desc", new CompanyDetails("ACME", null));
        var pendente = PendingJob.create(profileId, UUID.randomUUID(), AlertSource.LINKEDIN, job);
        pendingJobRepository.save(pendente);

        given().when().get("/profile/pending-jobs")
                .then()
                .statusCode(200)
                .body("jobs", hasSize(1))
                .body("jobs[0].jobTitle", is("Dev Java"))
                .body("jobs[0].jobLink", is("https://www.linkedin.com/jobs/view/7/"))
                .body("jobs[0].source", is("LINKEDIN"))
                .body("jobs[0].prioritized", is(false))
                .body("maxAttempts", is(3));

        given().when().post("/profile/pending-jobs/" + pendente.id() + "/priority")
                .then().statusCode(200).body("prioritized", is(true));
        given().when().post("/profile/pending-jobs/" + UUID.randomUUID() + "/priority")
                .then().statusCode(404);
    }

    @Test
    void recusaPedidoDeOutroSiteOuDeOutroHost() {
        createProfile();

        given().header("Origin", "https://site-malicioso.com").when().delete("/profile")
                .then().statusCode(403);
        given().header("Host", "site-malicioso.com").when().get("/profile")
                .then().statusCode(403);
        given().header("Origin", "http://localhost:7777").when().get("/profile")
                .then().statusCode(200);
    }

    @Test
    void rejeitaCurriculoGrandeOuQueNaoEhPdf() {
        given().multiPart("name", "F").multiPart("curriculum", "cv.pdf", new byte[6 * 1024 * 1024], "application/pdf")
                .when().post("/profile")
                .then().statusCode(400).body("type_error", is("VALIDATION_ERROR")).body("message_error", containsString("limite é 5.0 MB"));

        given().multiPart("name", "F").multiPart("curriculum", "cv.docx", new byte[10], "application/msword")
                .when().post("/profile")
                .then().statusCode(400).body("message_error", containsString("precisa ser um PDF"));

        given().multiPart("name", "F")
                .when().post("/profile")
                .then().statusCode(400).body("message_error", containsString("Envie o currículo"));
    }

    @Test
    void conectaEmailDetectandoAPastaDosAlertas() {
        createProfile();
        String email = "email-" + UUID.randomUUID() + "@gmail.com";

        given().contentType(ContentType.JSON).body(accountBody(email, "EMAIL", null))
                .when().post("/profile/accounts")
                .then()
                .statusCode(200)
                .body("linkedAccounts", hasSize(1))
                .body("linkedAccounts[0].username", is(email))
                .body("linkedAccounts[0].externalProvider", is("EMAIL"))
                .body("linkedAccounts[0].folder", is("LinkedIn"))
                .body("linkedAccounts[0]", not(hasKey("password")));
    }

    @Test
    void recusaSegundaContaDoMesmoProviderEPermiteTrocarDepoisDeDesconectar() {
        createProfile();
        link(accountBody("antigo", "LINKEDIN", null), 200);

        given().contentType(ContentType.JSON).body(accountBody("novo", "LINKEDIN", null))
                .when().post("/profile/accounts")
                .then().statusCode(409).body("type_error", is("CONFLICT"));

        given().when().delete("/profile/accounts/LINKEDIN").then().statusCode(200).body("linkedAccounts", empty());
        given().when().delete("/profile/accounts/LINKEDIN").then().statusCode(404);

        given().contentType(ContentType.JSON).body(accountBody("novo", "LINKEDIN", null))
                .when().post("/profile/accounts")
                .then().statusCode(200).body("linkedAccounts[0].username", is("novo"));
    }

    @Test
    void conectarContaSemPerfilRetorna404() {
        link(accountBody("x", "LINKEDIN", null), 404);
    }

    @Test
    void listaETrocaAPastaDoEmail() {
        createProfile();
        link(accountBody("email@gmail.com", "EMAIL", "INBOX"), 200);

        given().when().get("/profile/email/folders")
                .then()
                .statusCode(200)
                .body("name", contains("INBOX", "LinkedIn"))
                .body("jobAlerts.LINKEDIN", contains(0, 7));

        given().contentType(ContentType.JSON).body("{\"folder\":\"LinkedIn\"}")
                .when().put("/profile/email/folder")
                .then().statusCode(200).body("linkedAccounts[0].folder", is("LinkedIn"));

        given().contentType(ContentType.JSON).body("{\"folder\":\"INBOX\",\"source\":\"INDEED\"}")
                .when().put("/profile/email/folder")
                .then().statusCode(200)
                .body("linkedAccounts[0].folder", is("LinkedIn"))
                .body("linkedAccounts[0].indeedFolder", is("INBOX"));

        given().contentType(ContentType.JSON).body("{\"folder\":\"NaoExiste\"}")
                .when().put("/profile/email/folder")
                .then().statusCode(400);
    }

    @Test
    void listarPastasSemEmailRetorna422() {
        createProfile();

        given().when().get("/profile/email/folders").then().statusCode(422).body("type_error", is("BUSINESS_RULE"));
    }

    @Test
    void falhaNoEmailRetorna502() {
        createProfile();
        when(mailboxGateway.listFolders(any())).thenThrow(new ExternalServiceException("Falha ao acessar o email: timeout"));

        given().contentType(ContentType.JSON).body(accountBody("email@gmail.com", "EMAIL", null))
                .when().post("/profile/accounts")
                .then()
                .statusCode(502)
                .body("type_error", is("EXTERNAL_SERVICE_ERROR"))
                .body("message_error", containsString("Falha ao acessar o email"));
    }

    @Test
    void erroInesperadoNaoVazaDetalhesEDevolveErrorId() {
        createProfile();
        when(mailboxGateway.listFolders(any())).thenThrow(new RuntimeException("detalhe interno secreto"));

        given().contentType(ContentType.JSON).body(accountBody("email@gmail.com", "EMAIL", null))
                .when().post("/profile/accounts")
                .then()
                .statusCode(500)
                .body("type_error", is("INTERNAL_ERROR"))
                .body("error_id", notNullValue())
                .body("message_error", not(containsString("secreto")));
    }

    @Test
    void rotaInexistenteUsaOMesmoFormato() {
        given().when().get("/nao-existe")
                .then()
                .statusCode(404)
                .body("success", is(false))
                .body("type_error", is("HTTP_ERROR"))
                .body("message_error", is("Not Found"));
    }

    @Test
    void listaJobMatchesEStatus() {
        UUID id = UUID.fromString(createProfile());
        jobMatchRepository.saveAll(List.of(new JobMatch(UUID.randomUUID(), id, UUID.randomUUID(),
                "https://www.linkedin.com/jobs/view/1/", "Dev Java", "ACME", true, 88,
                List.of("bom match"), "cv ats", LocalDateTime.now(), null)));

        given().when().get("/profile/job-matches")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].jobTitle", is("Dev Java"))
                .body("[0].score", is(88))
                .body("[0].motivos", contains("bom match"));

        given().when().get("/profile/status")
                .then()
                .statusCode(200)
                .body("LINKEDIN.lastCheckAt", nullValue())
                .body("INDEED.lastError", nullValue());

        given().when().get("/profile/scraper").then().statusCode(200).body("LINKEDIN.running", is(false)).body("INDEED.running", is(false));
    }

    @Test
    void excluiOPerfil() {
        createProfile();

        given().when().delete("/profile").then().statusCode(204);
        given().when().get("/profile").then().statusCode(404);
        given().when().delete("/profile").then().statusCode(404);
    }
}
