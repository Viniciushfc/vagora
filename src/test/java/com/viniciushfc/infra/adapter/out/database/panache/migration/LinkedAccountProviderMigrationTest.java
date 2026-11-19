package com.viniciushfc.infra.adapter.out.database.panache.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LinkedAccountProviderMigrationTest {

    @Test
    void tiraSoARestricaoDoTipoDeConta() {
        String ddl = "CREATE TABLE user_linked_account (id_user_account blob not null, id blob, username varchar(255), password varchar(255), "
                + "externalProvider varchar(255) check ((externalProvider in ('LINKEDIN','EMAIL'))), folder varchar(255), indeedFolder varchar(255))";

        assertEquals("CREATE TABLE user_linked_account (id_user_account blob not null, id blob, username varchar(255), password varchar(255), "
                        + "externalProvider varchar(255), folder varchar(255), indeedFolder varchar(255))",
                LinkedAccountProviderMigration.withoutProviderCheck(ddl));
        assertNull(LinkedAccountProviderMigration.withoutProviderCheck(
                LinkedAccountProviderMigration.withoutProviderCheck(ddl)));
    }
}
