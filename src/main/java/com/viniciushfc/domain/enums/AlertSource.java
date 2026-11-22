package com.viniciushfc.domain.enums;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public enum AlertSource {
    LINKEDIN("jobalerts-noreply@linkedin.com", "LinkedIn", 1) {
        @Override
        public Map<String, String> jobLinks(String body) {
            Map<String, String> links = new LinkedHashMap<>();
            Matcher matcher = LINKEDIN_LINK.matcher(body);
            while (matcher.find()) {
                links.putIfAbsent("https://www.linkedin.com/jobs/view/" + matcher.group(1) + "/",
                        matcher.group().replace("&amp;", "&"));
            }
            return links;
        }
    },
    INDEED("indeedapply@indeed.com", "Indeed", 10) {
        @Override
        public Map<String, String> jobLinks(String body) {
            Map<String, String> links = new LinkedHashMap<>();
            Matcher matcher = INDEED_LINK.matcher(body);
            while (matcher.find()) {
                Matcher target = INDEED_TARGET.matcher(unzip(matcher.group(1)));
                if (target.find()) {
                    links.putIfAbsent("https://" + target.group(1) + "/viewjob?jk=" + target.group(2), matcher.group());
                }
            }
            return links;
        }
    },
    GLASSDOOR("noreply@glassdoor.com", "Glassdoor", 10) {
        @Override
        public Map<String, String> jobLinks(String body) {
            Map<String, String> links = new LinkedHashMap<>();
            Matcher matcher = GLASSDOOR_LINK.matcher(body);
            while (matcher.find()) {
                Matcher id = GLASSDOOR_JOB_ID.matcher(matcher.group());
                if (id.find()) {
                    links.putIfAbsent("https://www.glassdoor.com.br/job-listing?jl=" + id.group(1), matcher.group().replace("&amp;", "&"));
                }
            }
            return links;
        }
    },
    GUPY(null, "Gupy", 0) {
        @Override
        public Map<String, String> jobLinks(String body) {
            return Map.of();
        }
    };
    private static final Pattern LINKEDIN_LINK =
            Pattern.compile("https://www\\.linkedin\\.com/comm/jobs/view/(\\d+)/?(?:\\?[^\\s\"'<>]*)?");
    private static final Pattern INDEED_LINK = Pattern.compile("https://cts\\.indeed\\.com/v3/([A-Za-z0-9_-]+)/[A-Za-z0-9_-]+");
    private static final Pattern GLASSDOOR_LINK =
            Pattern.compile("https://www\\.glassdoor\\.com(?:\\.br)?/partner/jobListing\\.htm\\?[^\\s\"'<>]*");
    private static final Pattern GLASSDOOR_JOB_ID = Pattern.compile("[?&;]jobListingId=(\\d+)");
    private static final Pattern INDEED_TARGET =
            Pattern.compile("\"u\"\\s*:\\s*\"https://([a-z.]*indeed\\.com)/(?:rc/clk|viewjob)\\?[^\"]*?\\bjk=([0-9a-f]+)");

    public final String sender;
    public final String label;
    public final int alertsPerRun;

    AlertSource(String sender, String label, int alertsPerRun) {
        this.sender = sender;
        this.label = label;
        this.alertsPerRun = alertsPerRun;
    }

    public abstract Map<String, String> jobLinks(String body);

    public boolean viaEmail() {
        return sender != null;
    }

    private static String unzip(String payload) {
        try (var in = new GZIPInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(payload)))) {
            return new String(in.readNBytes(64 * 1024), StandardCharsets.UTF_8);
        } catch (IOException | IllegalArgumentException e) {
            return "";
        }
    }
}
