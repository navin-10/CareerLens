package com.careerlens.backend.config;

import java.util.List;

public class RoleConfig {

    private String name;
    private List<String> skills;

    public RoleConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }
}
