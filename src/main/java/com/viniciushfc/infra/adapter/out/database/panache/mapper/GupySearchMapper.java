package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheGupySearchEntity;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GupySearchMapper {

    private GupySearchMapper() {
    }

    public static PanacheGupySearchEntity toEntity(GupySearch search) {
        return new PanacheGupySearchEntity(search.id(), search.profileId(), search.jobName(), search.state(), search.city(),
                toColumn(search.workplaces()), toColumn(search.contracts()), search.everyHours(), search.enabled(),
                search.lastRunAt());
    }

    public static GupySearch toDomain(PanacheGupySearchEntity entity) {
        return new GupySearch(entity.getId(), entity.getProfileId(), entity.getJobName(), entity.getState(), entity.getCity(),
                fromColumn(entity.getWorkplaces(), WorkplaceType.class, WorkplaceType::valueOf),
                fromColumn(entity.getContracts(), ContractType.class, ContractType::valueOf),
                entity.getEveryHours(), entity.isEnabled(), entity.getLastRunAt());
    }

    private static String toColumn(Collection<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private static <E extends Enum<E>> Set<E> fromColumn(String column, Class<E> type, Function<String, E> parse) {
        Set<E> values = EnumSet.noneOf(type);
        if (column != null && !column.isBlank()) {
            Arrays.stream(column.split(",")).map(parse).forEach(values::add);
        }
        return values;
    }
}
