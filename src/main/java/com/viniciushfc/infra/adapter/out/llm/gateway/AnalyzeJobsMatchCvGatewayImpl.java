package com.viniciushfc.infra.adapter.out.llm.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.text.PlainText;
import com.viniciushfc.domain.vo.ChatMessage;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.domain.vo.LlmModels;
import com.viniciushfc.domain.vo.LocalLlm;
import com.viniciushfc.infra.adapter.out.llm.LlmService;
import com.viniciushfc.infra.adapter.out.llm.claude.impl.ClaudeServiceImpl;
import com.viniciushfc.infra.adapter.out.llm.gemini.impl.GeminiServiceImpl;
import com.viniciushfc.infra.adapter.out.llm.groq.impl.GroqServiceImpl;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class AnalyzeJobsMatchCvGatewayImpl implements AnalyzeJobsMatchCvGateway {
    private static final Pattern CURRICULUM_TAG = Pattern.compile("<curriculo_ats>([\\s\\S]*?)</curriculo_ats>");
    private final Map<LlmProvider, LlmService> services;
    private final ClaudeServiceImpl claude;
    private final CvMatchMapper cvMatchMapper;
    private final ObjectMapper objectMapper;

    public AnalyzeJobsMatchCvGatewayImpl(GeminiServiceImpl gemini, GroqServiceImpl groq, ClaudeServiceImpl claude,
                                         CvMatchMapper cvMatchMapper, ObjectMapper objectMapper) {
        this.services = Map.of(LlmProvider.GEMINI, gemini, LlmProvider.GROQ, groq, LlmProvider.CLAUDE, claude);
        this.claude = claude;
        this.cvMatchMapper = cvMatchMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CvMatchResult> analyze(List<JobDetail> jobs, String cvUser, Profile profile) {
        StringBuilder prompt = new StringBuilder(curriculum(cvUser));
        for (int i = 0; i < jobs.size(); i++) {
            prompt.append(job(jobs.get(i), i + 1)).append('\n');
        }
        return withFallback(profile, profile.analysisProviders(), (service, key, model) -> service.analyze(prompt.toString(), jobs.size(), key, model))
                .stream()
                .map(result -> new CvMatchResult(result.match(), result.score(), PlainText.cleanAll(result.motivos()), PlainText.clean(result.curriculoAts())))
                .toList();
    }

    @Override
    public String adaptCurriculum(JobDetail jobDetail, String cvUser, Profile profile) {
        String prompt = curriculum(cvUser) + job(jobDetail, 1);
        return keepingNumbers(cvUser, prompt, UnaryOperator.identity(),
                text -> withFallback(profile, profile.curriculumProviders(), (service, key, model) -> service.adaptCurriculum(text, key, model)));
    }

    @Override
    public String chat(JobMatch jobMatch, String cvUser, List<ChatMessage> messages, Profile profile) {
        String prompt = curriculum(cvUser) + job(jobMatch.jobDetail(), 1) + "\n" + analysis(jobMatch) + conversation(messages);
        return keepingNumbers(cvUser, prompt, AnalyzeJobsMatchCvGatewayImpl::curriculumIn,
                text -> withFallback(profile, profile.chatProviders(), (service, key, model) -> service.chat(text, key, model)));
    }

    @Override
    public List<String> models(LlmProvider provider, Profile profile) {
        if (provider == LlmProvider.LOCAL && (profile.localLlm() == null || !profile.localLlm().configured())) {
            return List.of();
        }
        String key = profile.apiKey(provider);
        boolean needsKey = provider == LlmProvider.GEMINI || provider == LlmProvider.GROQ;
        return needsKey && (key == null || key.isBlank()) ? List.of() : service(profile, provider).models(key);
    }

    @Override
    public LlmModels defaultModels() {
        return new LlmModels(services.get(LlmProvider.GEMINI).defaultModel(null), services.get(LlmProvider.GROQ).defaultModel(null),
                claude.apiDefaultModel(), claude.defaultModel(null));
    }

    private static String keepingNumbers(String cvUser, String prompt, UnaryOperator<String> curriculumOf, UnaryOperator<String> call) {
        String first = PlainText.clean(call.apply(prompt));
        String curriculum = curriculumOf.apply(first);
        List<String> missing = curriculum == null ? List.of() : PlainText.missingNumbers(cvUser, curriculum);
        if (missing.isEmpty()) {
            return first;
        }
        Log.infof("Currículo gerado sem %d número(s) do original (%s); pedindo de novo", missing.size(), String.join(", ", missing));
        try {
            String second = PlainText.clean(call.apply(prompt + revision(curriculum, missing)));
            String retried = curriculumOf.apply(second);
            return retried != null && PlainText.missingNumbers(cvUser, retried).size() < missing.size() ? second : first;
        } catch (RuntimeException e) {
            Log.warnf("A segunda tentativa do currículo falhou, fica a primeira: %s", e.getMessage());
            return first;
        }
    }

    static String revision(String curriculum, List<String> missing) {
        return "\n<curriculo_gerado>\n" + curriculum + "\n</curriculo_gerado>\n"
                + "Revisão: o currículo acima, que você gerou, deixou de fora estes números do <curriculo>: "
                + String.join(", ", missing) + ". Reescreva o currículo completo incluindo os fatos a que esses números "
                + "pertencem, quando fizerem parte de uma experiência, formação, projeto, curso ou certificação. "
                + "Ignore os que pertencem a dados que as regras mandam omitir ou a fatos que o candidato pediu para tirar na conversa.\n";
    }

    static String curriculumIn(String reply) {
        Matcher found = CURRICULUM_TAG.matcher(reply);
        return found.find() ? found.group(1) : null;
    }

    private interface Call<T> {
        T apply(LlmService service, String apiKey, String model);
    }

    private <T> T withFallback(Profile profile, List<LlmProvider> providers, Call<T> call) {
        if (providers.isEmpty()) {
            throw new BusinessException(
                    "Nenhuma IA disponível para esta etapa. Configure uma chave na aba IA ou marque outra IA em Fluxo da IA.",
                    ErrorCode.LLM_KEY_MISSING);
        }
        RuntimeException failure = null;
        for (LlmProvider provider : providers) {
            try {
                return call.apply(service(profile, provider), profile.apiKey(provider), profile.model(provider));
            } catch (RuntimeException e) {
                Log.warnf("%s falhou: %s", provider.label, e.getMessage());
                failure = e;
            }
        }
        throw failure;
    }

    private LlmService service(Profile profile, LlmProvider provider) {
        if (provider != LlmProvider.LOCAL) {
            return services.get(provider);
        }
        LocalLlm local = profile.localLlm();
        return GroqServiceImpl.compatible(cvMatchMapper, objectMapper, provider.label, local.model(), local.chatCompletions());
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

    static String analysis(JobMatch jobMatch) {
        StringBuilder text = new StringBuilder("<analise>\nNota: ").append(jobMatch.score())
                .append("\nCompatível: ").append(jobMatch.match() ? "sim" : "não").append("\nMotivos:\n");
        Objects.requireNonNullElse(jobMatch.motivos(), List.<String>of()).forEach(reason -> text.append("- ").append(reason).append('\n'));
        text.append("</analise>\n");
        if (jobMatch.curriculoAts() != null && !jobMatch.curriculoAts().isBlank()) {
            text.append("<curriculo_adaptado>\n").append(jobMatch.curriculoAts()).append("\n</curriculo_adaptado>\n");
        }
        return text.toString();
    }

    static String conversation(List<ChatMessage> messages) {
        StringBuilder text = new StringBuilder("<conversa>\n");
        messages.forEach(message -> text.append(message.fromUser() ? "Candidato: " : "Você: ").append(message.text()).append("\n\n"));
        return text.append("</conversa>\n").toString();
    }

    static String curriculum(String cvUser) {
        return "<curriculo>\n" + cvUser + "\n</curriculo>\n";
    }
}
