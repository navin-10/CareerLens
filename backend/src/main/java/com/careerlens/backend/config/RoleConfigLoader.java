package com.careerlens.backend.config;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class RoleConfigLoader {

    private final ObjectMapper objectMapper;

    private Map<String, RoleConfig> roles;
    private Map<String, String[]> skills;

    public RoleConfigLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadConfigurations();
    }

    private void loadConfigurations() {
        try {
            ClassPathResource rolesResource =
                    new ClassPathResource("config/roles.json");

            try (InputStream inputStream = rolesResource.getInputStream()) {
                roles = objectMapper.readValue(
                        inputStream,
                        new TypeReference<Map<String, RoleConfig>>() {}
                );
            }

            ClassPathResource skillsResource =
                    new ClassPathResource("config/skills.json");

            try (InputStream inputStream = skillsResource.getInputStream()) {
                skills = objectMapper.readValue(
                        inputStream,
                        new TypeReference<Map<String, String[]>>() {}
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load CareerLens configuration files",
                    e
            );
        }
    }

    public Map<String, RoleConfig> getRoles() {
        return roles;
    }

    public Map<String, String[]> getSkills() {
        return skills;
    }
}