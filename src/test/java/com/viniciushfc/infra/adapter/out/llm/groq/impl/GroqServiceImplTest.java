package com.viniciushfc.infra.adapter.out.llm.groq.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqServiceImplTest {

    private record Reply(int status, String body, String retryAfter) {
    }

    private record Received(String authorization, JsonNode body) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<Reply> replies = new ArrayDeque<>();
    private final List<Received> received = new ArrayList<>();
    private HttpServer server;
    private GroqServiceImpl groq;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", this::answer);
        server.start();

        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat");
        groq = new GroqServiceImpl(new CvMatchMapper(objectMapper), objectMapper, "openai/gpt-oss-120b", endpoint);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void answer(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        received.add(new Received(authorization, objectMapper.readTree(exchange.getRequestBody())));

        Reply reply = replies.removeFirst();
        if (reply.retryAfter() != null) {
            exchange.getResponseHeaders().add("retry-after", reply.retryAfter());
        }
        byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(reply.status(), body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void reply(int status, String body) {
        replies.add(new Reply(status, body, null));
    }

    private void replyLimited(String retryAfter) {
        replies.add(new Reply(429, "{\"error\":{\"message\":\"Rate limit reached\"}}", retryAfter));
    }

    private String completion(String content) {
        var message = objectMapper.createObjectNode().put("content", content);
        var choice = objectMapper.createObjectNode().set("message", message);
        return objectMapper.createObjectNode()
                .set("choices", objectMapper.createArrayNode().add(choice))
                .toString();
    }

    @Test
    void analisaComEsquemaEstritoELeOConteudo() {
        reply(200, completion("""
                {"resultados": [{"id": 1, "match": true, "score": 88, "motivos": ["a", "b"]}]}"""));

        assertEquals(List.of(new CvMatchResult(true, 88, List.of("a", "b"), null)), groq.analyze("prompt", 1, "gsk"));

        Received request = received.getFirst();
        assertEquals("Bearer gsk", request.authorization());
        assertEquals("openai/gpt-oss-120b", request.body().path("model").asText());
        assertEquals("low", request.body().path("reasoning_effort").asText());
        assertEquals("prompt", request.body().path("messages").path(1).path("content").asText());
        assertTrue(request.body().path("response_format").path("json_schema").path("strict").asBoolean());
    }

    @Test
    void curriculoVaiSemEsquema() {
        reply(200, completion("  cv adaptado "));

        assertEquals("cv adaptado", groq.adaptCurriculum("prompt", "gsk"));
        assertTrue(received.getFirst().body().path("response_format").isMissingNode());
    }

    @Test
    void limitePorMinutoCurtoEsperaETentaDeNovoUmaVez() {
        replyLimited("0");
        reply(200, completion("cv"));

        assertEquals("cv", groq.adaptCurriculum("p", "x"));
        assertEquals(2, received.size());
    }

    @Test
    void limiteQueContinuaDepoisDaNovaTentativaSobeComoCota() {
        replyLimited("0");
        replyLimited("0");

        assertThrows(RateLimitedException.class, () -> groq.adaptCurriculum("p", "x"));
        assertEquals(2, received.size());
    }

    @Test
    void limiteLongoNaoSeguraAThread() {
        replyLimited("3600");

        RateLimitedException limited = assertThrows(RateLimitedException.class, () -> groq.adaptCurriculum("p", "x"));

        assertEquals(Duration.ofHours(1), limited.retryAfter());
        assertEquals(ErrorCode.LLM_QUOTA_EXCEEDED, limited.code());
        assertEquals(1, received.size());
    }

    @Test
    void chaveRecusadaERespostaForaDoFormato() {
        reply(401, "{\"error\":{\"message\":\"Invalid API Key\"}}");
        ExternalServiceException rejected = assertThrows(ExternalServiceException.class,
                () -> groq.adaptCurriculum("p", "x"));
        assertEquals(ErrorCode.LLM_KEY_REJECTED, rejected.code());

        reply(200, completion("não é json"));
        ExternalServiceException invalid = assertThrows(ExternalServiceException.class,
                () -> groq.analyze("p", 1, "x"));
        assertEquals(ErrorCode.LLM_INVALID_RESPONSE, invalid.code());
    }
}
