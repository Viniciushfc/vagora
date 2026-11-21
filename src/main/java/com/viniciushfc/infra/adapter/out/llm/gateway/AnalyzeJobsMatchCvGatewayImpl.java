package com.viniciushfc.infra.adapter.out.llm.gateway;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.llm.LlmService;
import com.viniciushfc.infra.adapter.out.llm.claude.impl.ClaudeServiceImpl;
import com.viniciushfc.infra.adapter.out.llm.gemini.impl.GeminiServiceImpl;
import com.viniciushfc.infra.adapter.out.llm.groq.impl.GroqServiceImpl;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

@ApplicationScoped
public class AnalyzeJobsMatchCvGatewayImpl implements AnalyzeJobsMatchCvGateway {

    private final Map<LlmProvider, LlmService> services;

    public AnalyzeJobsMatchCvGatewayImpl(GeminiServiceImpl gemini, GroqServiceImpl groq, ClaudeServiceImpl claude) {
        this.services = Map.of(LlmProvider.GEMINI, gemini, LlmProvider.GROQ, groq, LlmProvider.CLAUDE, claude);
    }

    @Override
    public List<CvMatchResult> analyze(List<JobDetail> jobs, String cvUser, Profile profile) {
        // o currículo vem primeiro: o começo igual entre chamadas aproveita o cache automático do Gemini
        StringBuilder prompt = new StringBuilder(curriculum(cvUser));
        for (int i = 0; i < jobs.size(); i++) {
            prompt.append(job(jobs.get(i), i + 1)).append('\n');
        }
        return withFallback(profile, profile.analysisProviders(), (service, key) -> service.analyze(prompt.toString(), jobs.size(), key));
    }

    @Override
    public String adaptCurriculum(JobDetail jobDetail, String cvUser, Profile profile) {
        String prompt = curriculum(cvUser) + job(jobDetail, 1);
        return withFallback(profile, profile.curriculumProviders(), (service, key) -> service.adaptCurriculum(prompt, key));
    }

    private <T> T withFallback(Profile profile, List<LlmProvider> providers, BiFunction<LlmService, String, T> call) {
        if (providers.isEmpty()) {
            throw new BusinessException(
                    "Nenhuma IA disponível para esta etapa. Configure uma chave na aba IA ou marque outra IA em Fluxo da IA.",
                    ErrorCode.LLM_KEY_MISSING);
        }
        RuntimeException failure = null;
        for (LlmProvider provider : providers) {
            try {
                return call.apply(services.get(provider), profile.apiKey(provider));
            } catch (RuntimeException e) {
                Log.warnf("%s falhou: %s", provider.label, e.getMessage());
                failure = e;
            }
        }
        throw failure;
    }

    static String job(JobDetail job, int id) {
        var company = job.companyDetails();
        return """
                <vaga id="%d">
                Título: %s
                Empresa: %s
                Descrição: %s
                Sobre a empresa: %s
                </vaga>""".formatted(id, job.title(), company == null ? "" : company.name(), job.description(),
                company == null ? "" : Objects.toString(company.description(), ""));
    }

    static String curriculum(String cvUser) {
        return "<curriculo>\n" + cvUser + "\n</curriculo>\n";
    }
}
