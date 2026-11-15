package com.viniciushfc.infra.adapter.in.rest;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserAccountResourceTest {

    @Inject
    JobMatchRepository jobMatchRepository;

    private String createUser() {
        return given()
                .multiPart("name", "Fulano")
                .multiPart("username", "fulano-" + UUID.randomUUID())
                .multiPart("password", "123")
                .multiPart("curriculum", "cv.pdf", new byte[]{1, 2}, "application/pdf")
                .when().post("/users")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", is("Fulano"))
                .body("curriculumName", is("cv.pdf"))
                .body("linkedAccounts", empty())
                .body("$", not(hasKey("password")))
                .extract().path("id");
    }

    private String linkBody(String username) {
        return "{\"username\":\"" + username + "\",\"password\":\"x\",\"externalProvider\":\"LINKEDIN\"}";
    }

    @Test
    void criaUsuario() {
        createUser();
    }

    @Test
    void atualizaSomenteCamposEnviados() {
        String id = createUser();

        given()
                .multiPart("name", "Novo Nome")
                .when().patch("/users/" + id)
                .then()
                .statusCode(200)
                .body("name", is("Novo Nome"))
                .body("username", startsWith("fulano-"))
                .body("curriculumName", is("cv.pdf"));
    }

    @Test
    void atualizarUsuarioInexistenteRetorna400() {
        given()
                .multiPart("name", "x")
                .when().patch("/users/" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("message", containsString("Usuário não encontrado"));
    }

    @Test
    void vinculaContaERejeitaDuplicada() {
        String id = createUser();
        String linked = "linked-" + UUID.randomUUID() + "@gmail.com";

        given()
                .contentType(ContentType.JSON)
                .body(linkBody(linked))
                .when().post("/users/" + id + "/linked-accounts")
                .then()
                .statusCode(200)
                .body("linkedAccounts", hasSize(1))
                .body("linkedAccounts[0].username", is(linked))
                .body("linkedAccounts[0].externalProvider", is("LINKEDIN"))
                .body("linkedAccounts[0]", not(hasKey("password")));

        given()
                .contentType(ContentType.JSON)
                .body(linkBody(linked))
                .when().post("/users/" + id + "/linked-accounts")
                .then()
                .statusCode(400)
                .body("message", containsString("Conta já vinculada"));
    }

    @Test
    void vincularContaEmUsuarioInexistenteRetorna400() {
        given()
                .contentType(ContentType.JSON)
                .body(linkBody("x-" + UUID.randomUUID() + "@gmail.com"))
                .when().post("/users/" + UUID.randomUUID() + "/linked-accounts")
                .then()
                .statusCode(400);
    }

    @Test
    void listaJobMatchesDoUsuario() {
        String id = createUser();
        UUID userId = UUID.fromString(id);
        jobMatchRepository.saveAll(List.of(new JobMatch(UUID.randomUUID(), userId, UUID.randomUUID(),
                "https://www.linkedin.com/jobs/view/1/", "Dev Java", "ACME", true, 88,
                List.of("bom match"), "cv ats", LocalDateTime.now())));

        given()
                .when().get("/users/" + id + "/job-matches")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].jobTitle", is("Dev Java"))
                .body("[0].score", is(88))
                .body("[0].match", is(true))
                .body("[0].motivos", contains("bom match"));
    }

    @Test
    void listaVaziaQuandoNaoHaMatches() {
        given()
                .when().get("/users/" + UUID.randomUUID() + "/job-matches")
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void removeUsuario() {
        String id = createUser();

        given().when().delete("/users/" + id).then().statusCode(204);

        given()
                .multiPart("name", "x")
                .when().patch("/users/" + id)
                .then()
                .statusCode(400);
    }
}
