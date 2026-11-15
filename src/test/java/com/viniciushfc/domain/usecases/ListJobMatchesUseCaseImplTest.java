package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.usecases.impl.ListJobMatchesUseCaseImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListJobMatchesUseCaseImplTest {

    @Test
    void listaOsMatchesDoUsuario() {
        var repository = mock(JobMatchRepository.class);
        var userId = UUID.randomUUID();
        var matches = List.of(new JobMatch(UUID.randomUUID(), userId, UUID.randomUUID(), "link", "Dev", "Empresa",
                true, 90, List.of("m"), "cv", LocalDateTime.now()));
        when(repository.findByUserAccountId(userId)).thenReturn(matches);

        assertEquals(matches, new ListJobMatchesUseCaseImpl(repository).execute(userId));
    }
}
