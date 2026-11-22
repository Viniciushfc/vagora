package com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts;

import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheGupySearchEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class PanacheGupySearchRepository implements PanacheRepositoryBase<PanacheGupySearchEntity, UUID> {
}
