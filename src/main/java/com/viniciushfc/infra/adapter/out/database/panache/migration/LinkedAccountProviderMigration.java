package com.viniciushfc.infra.adapter.out.database.panache.migration;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LinkedAccountProviderMigration {

    static final String TABLE = "user_linked_account";

    private final EntityManager entityManager;
    private final String dbKind;

    public LinkedAccountProviderMigration(EntityManager entityManager,
                                          @ConfigProperty(name = "quarkus.datasource.db-kind") String dbKind) {
        this.entityManager = entityManager;
        this.dbKind = dbKind;
    }

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (!"sqlite".equals(dbKind)) {
            return;
        }
        String ddl = (String) entityManager
                .createNativeQuery("select sql from sqlite_master where type = 'table' and name = '" + TABLE + "'")
                .getResultList().stream().findFirst().orElse(null);
        String withoutCheck = withoutProviderCheck(ddl);
        if (withoutCheck == null) {
            return;
        }
        Log.info("Liberando novos tipos de conta na tabela " + TABLE);
        entityManager.createNativeQuery("alter table " + TABLE + " rename to " + TABLE + "_old").executeUpdate();
        entityManager.createNativeQuery(withoutCheck).executeUpdate();
        entityManager.createNativeQuery("insert into " + TABLE + " select * from " + TABLE + "_old").executeUpdate();
        entityManager.createNativeQuery("drop table " + TABLE + "_old").executeUpdate();
    }

    static String withoutProviderCheck(String ddl) {
        if (ddl == null || !ddl.contains("externalProvider in")) {
            return null;
        }
        return ddl.replaceFirst("\\s*check\\s*\\(\\(externalProvider in \\([^)]*\\)\\)\\)", "");
    }
}
