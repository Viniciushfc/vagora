package com.viniciushfc.infra.adapter.out.database.panache.models;

import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheAttachmentEmbeddable;
import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheLinkedAccountEmbeddable;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_account")
public class PanacheUserAccountEntity {

    @Id
    private UUID id;
    private String name;
    private String username;
    private String password;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "nameAttachment")),
            @AttributeOverride(name = "contentType", column = @Column(name = "contentType")),
            @AttributeOverride(name = "content", column = @Column(name = "content"))
    })
    private PanacheAttachmentEmbeddable curriculum;

    @ElementCollection
    @CollectionTable(
            name = "user_linked_account",
            joinColumns = @JoinColumn(name = "id_user_account", referencedColumnName = "id")
    )
    private List<PanacheLinkedAccountEmbeddable> linkedAccounts;

    public PanacheUserAccountEntity(UUID id, String name, String username, String password, PanacheAttachmentEmbeddable curriculum, List<PanacheLinkedAccountEmbeddable> linkedAccounts) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.curriculum = curriculum;
        this.linkedAccounts = linkedAccounts;
    }

    public PanacheUserAccountEntity() {

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public PanacheAttachmentEmbeddable getCurriculum() {
        return curriculum;
    }

    public void setCurriculum(PanacheAttachmentEmbeddable curriculum) {
        this.curriculum = curriculum;
    }

    public List<PanacheLinkedAccountEmbeddable> getLinkedAccounts() {
        return linkedAccounts;
    }

    public void setLinkedAccounts(List<PanacheLinkedAccountEmbeddable> linkedAccounts) {
        this.linkedAccounts = linkedAccounts;
    }
};
