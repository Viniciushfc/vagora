package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record GupySearch(UUID id,
                         UUID profileId,
                         String jobName,
                         String state,
                         String city,
                         Set<WorkplaceType> workplaces,
                         Set<ContractType> contracts,
                         int everyHours,
                         boolean enabled,
                         LocalDateTime lastRunAt) {

    public static final int DEFAULT_EVERY_HOURS = 4;
    private static final List<Set<String>> LEVELS = List.of(
            Set.of("jr", "junior"), Set.of("pl", "pleno"), Set.of("sr", "senior"));

    public String query() {
        String query = Arrays.stream(jobName.trim().split("\\s+"))
                .filter(word -> !isLevelWord(word))
                .collect(Collectors.joining(" "));
        return query.isEmpty() ? jobName : query;
    }

    public boolean matchesLevel(String title) {
        Set<String> wanted = Arrays.stream(jobName.trim().split("\\s+"))
                .filter(GupySearch::isLevelWord)
                .flatMap(word -> LEVELS.stream().filter(level -> level.contains(words(word).getFirst())))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        return wanted.isEmpty() || words(title).stream().anyMatch(wanted::contains);
    }

    private static boolean isLevelWord(String word) {
        List<String> parts = words(word);
        return parts.size() == 1 && LEVELS.stream().anyMatch(level -> level.contains(parts.getFirst()));
    }

    private static List<String> words(String text) {
        String plain = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Arrays.stream(plain.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")).filter(word -> !word.isEmpty()).toList();
    }

    public boolean isDue(LocalDateTime now) {
        return enabled && (lastRunAt == null || !now.isBefore(lastRunAt.plusHours(everyHours)));
    }

    public GupySearch ranAt(LocalDateTime now) {
        return new GupySearch(id, profileId, jobName, state, city, workplaces, contracts, everyHours, enabled, now);
    }
}
