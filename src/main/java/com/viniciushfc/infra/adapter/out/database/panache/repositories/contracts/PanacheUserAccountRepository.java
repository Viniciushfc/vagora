package com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts;

import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheUserAccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import java.util.UUID;

public interface PanacheUserAccountRepository extends PanacheRepositoryBase<PanacheUserAccountEntity, UUID> {

}
