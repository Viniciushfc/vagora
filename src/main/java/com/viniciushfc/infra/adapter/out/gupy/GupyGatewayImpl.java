package com.viniciushfc.infra.adapter.out.gupy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.gateways.GupyGateway;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.GupyJob;
import com.viniciushfc.domain.vo.JobDetail;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class GupyGatewayImpl implements GupyGateway {

    static final String ENDPOINT = "https://portal.gupy.io/api/job-search/jobs";
    static final int PAGE_SIZE = 100;
    static final int MAX_PAGES = 5;

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private static final Map<WorkplaceType, String> WORKPLACE_PARAM = Map.of(
            WorkplaceType.REMOTE, "remote", WorkplaceType.HYBRID, "hybrid", WorkplaceType.ON_SITE, "on-site");
    private static final Map<String, String> WORKPLACE_LABEL = Map.of(
            "remote", "Remoto", "hybrid", "Híbrido", "on-site", "Presencial");
    private static final Map<String, ContractType> CONTRACT = Map.of(
            "vacancy_type_effective", ContractType.CLT,
            "vacancy_legal_entity", ContractType.PJ,
            "vacancy_type_internship", ContractType.INTERNSHIP,
            "vacancy_type_temporary", ContractType.TEMPORARY,
            "vacancy_type_talent_pool", ContractType.TALENT_POOL);
    private static final Map<ContractType, String> CONTRACT_LABEL = Map.of(
            ContractType.CLT, "CLT", ContractType.PJ, "PJ", ContractType.INTERNSHIP, "Estágio",
            ContractType.TEMPORARY, "Temporária", ContractType.TALENT_POOL, "Banco de talentos", ContractType.OTHER, "Outro");

    private final ObjectMapper mapper;

    public GupyGatewayImpl(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<GupyJob> search(GupySearch search, LocalDate since) {
        List<GupyJob> jobs = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            List<GupyJob> found = toJobs(fetch(url(search, page * PAGE_SIZE)).path("data"));
            jobs.addAll(found);
            if (found.size() < PAGE_SIZE || found.getLast().publishedAt().toLocalDate().isBefore(since)) {
                break;
            }
        }
        return jobs;
    }

    static String url(GupySearch search, int offset) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("jobName", search.query());
        if (search.state() != null) {
            params.put("state", search.state());
        }
        if (search.city() != null) {
            params.put("city", search.city());
        }
        params.put("workplaceType", search.workplaces().stream().map(WORKPLACE_PARAM::get).sorted().collect(Collectors.joining(",")));
        params.put("limit", String.valueOf(PAGE_SIZE));
        params.put("offset", String.valueOf(offset));
        return ENDPOINT + "?" + params.entrySet().stream()
                .map(param -> param.getKey() + "=" + URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8).replace("%2C", ","))
                .collect(Collectors.joining("&"));
    }

    static List<GupyJob> toJobs(JsonNode data) {
        List<GupyJob> jobs = new ArrayList<>();
        for (JsonNode node : data) {
            if (node.path("jobUrl").asText("").isBlank()) {
                continue;
            }
            ContractType contract = CONTRACT.getOrDefault(node.path("type").asText(), ContractType.OTHER);
            String header = "Modalidade: " + WORKPLACE_LABEL.getOrDefault(node.path("workplaceType").asText(), "não informada")
                    + ". Local: " + location(node) + ". Contratação: " + CONTRACT_LABEL.get(contract) + ".";
            JobDetail job = new JobDetail(node.path("jobUrl").asText(), node.path("name").asText(),
                    header + "\n\n" + node.path("description").asText(),
                    new CompanyDetails(node.path("careerPageName").asText(null), null));
            jobs.add(new GupyJob(job, contract, dateTime(node.path("publishedDate").asText(null)),
                    date(node.path("applicationDeadline").asText(null))));
        }
        return jobs;
    }

    private static String location(JsonNode node) {
        String city = node.path("city").asText("");
        String state = node.path("state").asText("");
        if (city.isBlank() && state.isBlank()) {
            return "não informado";
        }
        return city.isBlank() ? state : state.isBlank() ? city : city + " - " + state;
    }

    private static LocalDateTime dateTime(String instant) {
        return instant == null ? LocalDateTime.now() : LocalDateTime.ofInstant(Instant.parse(instant), ZoneId.systemDefault());
    }

    private static LocalDate date(String instant) {
        return instant == null ? null : dateTime(instant).toLocalDate();
    }

    private JsonNode fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw unavailable(new IOException("HTTP " + response.statusCode()));
            }
            return mapper.readTree(response.body());
        } catch (IOException e) {
            throw unavailable(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        }
    }

    private static ExternalServiceException unavailable(Exception cause) {
        return new ExternalServiceException("Não foi possível buscar vagas na Gupy agora. O Vagora tenta de novo em alguns minutos.", cause);
    }
}
