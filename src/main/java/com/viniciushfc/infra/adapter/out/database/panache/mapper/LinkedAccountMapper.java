package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheLinkedAccountEmbeddable;

import java.util.ArrayList;
import java.util.List;

public class LinkedAccountMapper {

    public static List<LinkedAccount> toDomainLinkedAccounts(List<PanacheLinkedAccountEmbeddable> panacheLinkedAccountEmbeddables) {
        List<LinkedAccount> linkedAccounts = new ArrayList<>();

        for (PanacheLinkedAccountEmbeddable panacheLinkedAccountEmbeddable : panacheLinkedAccountEmbeddables) {
            LinkedAccount linkedAccount = new LinkedAccount(panacheLinkedAccountEmbeddable.id(),
                    panacheLinkedAccountEmbeddable.username(),
                    panacheLinkedAccountEmbeddable.password(),
                    panacheLinkedAccountEmbeddable.externalProvider()
            );

            linkedAccounts.add(linkedAccount);
        }

        return linkedAccounts;
    }

    public static List<PanacheLinkedAccountEmbeddable> toPanacheLinkedAccountEmbeddable(List<LinkedAccount> linkedAccounts) {
        List<PanacheLinkedAccountEmbeddable> panacheLinkedAccountEmbeddables = new ArrayList<>();

        for (LinkedAccount linkedAccount : linkedAccounts) {
            PanacheLinkedAccountEmbeddable panacheLinkedAccountEmbeddable = new PanacheLinkedAccountEmbeddable(
                    linkedAccount.id(),
                    linkedAccount.username(),
                    linkedAccount.password(),
                    linkedAccount.externalProvider()
            );

            panacheLinkedAccountEmbeddables.add(panacheLinkedAccountEmbeddable);
        }

        return panacheLinkedAccountEmbeddables;
    }

}
