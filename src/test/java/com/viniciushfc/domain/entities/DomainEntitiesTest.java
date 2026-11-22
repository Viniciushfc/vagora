package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainEntitiesTest {

    private Email email(String from) {
        return new Email(from, "to@x.com", "assunto", "corpo", List.of());
    }

    @Test
    void isFromSomenteParaORemetenteDeAlertaDaFonte() {
        assertTrue(EmailReceived.create("m", email("LinkedIn <jobalerts-noreply@linkedin.com>")).isFrom(AlertSource.LINKEDIN));
        assertTrue(EmailReceived.create("m", email("Indeed <indeedapply@indeed.com>")).isFrom(AlertSource.INDEED));
        assertFalse(EmailReceived.create("m", email("Indeed <indeedapply@indeed.com>")).isFrom(AlertSource.LINKEDIN));
        assertFalse(EmailReceived.create("m", email("outro@linkedin.com")).isFrom(AlertSource.LINKEDIN));
        assertFalse(EmailReceived.create("m", email(null)).isFrom(AlertSource.INDEED));
    }

    @Test
    void glassdoorUsaONumeroDaVagaComoLinkEMantemOLinkDoEmailParaAbrir() {
        String body = """
                <a href="https://www.glassdoor.com.br/partner/jobListing.htm?pos=101&amp;ao=1136043&amp;guid=abc&amp;jobListingId=1010327777737&amp;utm_medium=email">Vaga 1</a>
                <a href="https://www.glassdoor.com.br/partner/jobListing.htm?pos=102&amp;guid=def&amp;jobListingId=1010327777737&amp;ea=1">Vaga 1 de novo</a>
                <a href="https://www.glassdoor.com.br/partner/jobListing.htm?pos=103&amp;jobListingId=1010324587469">Vaga 2</a>
                <a href="https://www.glassdoor.com.br/Vaga/remoto-vagas.htm?ja=439605581">Ver mais vagas</a>""";

        var links = AlertSource.GLASSDOOR.jobLinks(body);

        assertEquals(List.of("https://www.glassdoor.com.br/job-listing?jl=1010327777737", "https://www.glassdoor.com.br/job-listing?jl=1010324587469"),
                List.copyOf(links.keySet()));
        assertEquals("https://www.glassdoor.com.br/partner/jobListing.htm?pos=101&ao=1136043&guid=abc&jobListingId=1010327777737&utm_medium=email",
                links.get("https://www.glassdoor.com.br/job-listing?jl=1010327777737"));
        assertTrue(EmailReceived.create("m", email("Vagas do Glassdoor <noreply@glassdoor.com>")).isFrom(AlertSource.GLASSDOOR));
    }

    @Test
    void findLinkedAccountPorProvider() {
        var email = LinkedAccount.create("e@x.com", "s", ExternalProvider.EMAIL, "INBOX");
        var user = Profile.create("F", "k", null, null, null);
        user.linkedAccounts().add(email);

        assertEquals(email, user.findLinkedAccount(ExternalProvider.EMAIL));
        assertNull(user.findLinkedAccount(ExternalProvider.LINKEDIN));
    }

    @Test
    void cadaEtapaSegueOFluxoConfiguradoPulandoIaSemChave() {
        var user = new Profile(null, "F", "gemini", null, new ArrayList<>(), null, null, null, LlmProvider.GEMINI, null, null, null,
                List.of(LlmProvider.GROQ, LlmProvider.CLAUDE, LlmProvider.GEMINI), null, null);

        assertEquals(List.of(LlmProvider.GEMINI), user.analysisProviders());
        assertEquals(List.of(LlmProvider.GEMINI), user.curriculumProviders());
        assertEquals(List.of(LlmProvider.GROQ, LlmProvider.CLAUDE, LlmProvider.GEMINI), user.analysisOrder());
        assertEquals(List.of(LlmProvider.GEMINI, LlmProvider.GROQ, LlmProvider.CLAUDE, LlmProvider.LOCAL), user.curriculumOrder());

        var semGemini = new Profile(null, "F", "gemini", null, new ArrayList<>(), null, null, "groq", LlmProvider.GEMINI, null, null, null,
                List.of(LlmProvider.GROQ), List.of(LlmProvider.GEMINI, LlmProvider.GROQ), null);
        assertEquals(List.of(LlmProvider.GROQ), semGemini.analysisProviders());
        assertEquals(List.of(LlmProvider.GEMINI, LlmProvider.GROQ), semGemini.curriculumProviders());
    }

    @Test
    void claudeSoEntraComoIaDepoisDeConectadoEmContas() {
        var user = Profile.create("F", "k", null, LlmProvider.CLAUDE, null);
        assertEquals(List.of(LlmProvider.GEMINI), user.llmProviders());

        user.linkedAccounts().add(LinkedAccount.create("e@x.com", null, ExternalProvider.CLAUDE, null));
        assertEquals(List.of(LlmProvider.CLAUDE, LlmProvider.GEMINI), user.llmProviders());
        assertNull(user.apiKey(LlmProvider.CLAUDE));
    }
}
