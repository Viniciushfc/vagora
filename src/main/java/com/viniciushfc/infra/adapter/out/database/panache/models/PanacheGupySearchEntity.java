package com.viniciushfc.infra.adapter.out.database.panache.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gupy_search")
public class PanacheGupySearchEntity {

    @Id
    private UUID id;
    private UUID profileId;
    private String jobName;
    private String state;
    private String city;
    private String workplaces;
    private String contracts;
    private int everyHours;
    private boolean enabled;
    private LocalDateTime lastRunAt;

    public PanacheGupySearchEntity(UUID id, UUID profileId, String jobName, String state, String city, String workplaces,
                                   String contracts, int everyHours, boolean enabled, LocalDateTime lastRunAt) {
        this.id = id;
        this.profileId = profileId;
        this.jobName = jobName;
        this.state = state;
        this.city = city;
        this.workplaces = workplaces;
        this.contracts = contracts;
        this.everyHours = everyHours;
        this.enabled = enabled;
        this.lastRunAt = lastRunAt;
    }

    public PanacheGupySearchEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getState() {
        return state;
    }

    public String getCity() {
        return city;
    }

    public String getWorkplaces() {
        return workplaces;
    }

    public String getContracts() {
        return contracts;
    }

    public int getEveryHours() {
        return everyHours;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }
}
