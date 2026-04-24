package com.trafficlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScenarioType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    protected Scenario() {
    }

    private Scenario(ScenarioType type, String name, String description, boolean enabled) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    public static Scenario create(ScenarioType type, String name, String description, boolean enabled) {
        return new Scenario(type, name, description, enabled);
    }

    public Long getId() {
        return id;
    }

    public ScenarioType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
