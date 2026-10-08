package com.careerlens.backend.github;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitHubEvidenceService {

    private final RestClient restClient;

    public GitHubEvidenceService() {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(
                        "Accept",
                        "application/vnd.github+json"
                )
                .defaultHeader(
                        "X-GitHub-Api-Version",
                        "2022-11-28"
                )
                .build();
    }

    public Map<String, Object> analyzeRepository(
            String username,
            String repositoryName) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "GitHub username is required"
            );
        }

        if (repositoryName == null
                || repositoryName.isBlank()) {

            throw new IllegalArgumentException(
                    "Repository name is required"
            );
        }

        String cleanUsername =
                username.trim();

        String cleanRepository =
                repositoryName.trim();

        Map<String, Object> repository =
                getRepository(
                        cleanUsername,
                        cleanRepository
                );

        List<Map<String, Object>> contents =
                getRepositoryContents(
                        cleanUsername,
                        cleanRepository
                );

        Map<String, Object> evidence =
                new LinkedHashMap<>();

        evidence.put(
                "repository",
                repository.get("name")
        );

        evidence.put(
                "repositoryUrl",
                repository.get("html_url")
        );

        evidence.put(
                "description",
                repository.get("description")
        );

        evidence.put(
                "primaryLanguage",
                repository.get("language")
        );

        evidence.put(
                "stars",
                repository.get("stargazers_count")
        );

        evidence.put(
                "forks",
                repository.get("forks_count")
        );

        evidence.put(
                "evidence",
                analyzeFiles(contents)
        );

        return evidence;
    }

    private Map<String, Object> getRepository(
            String username,
            String repositoryName) {

        Map<String, Object> repository =
                restClient.get()
                        .uri(
                                "/repos/{username}/{repository}",
                                username,
                                repositoryName
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        Map<String, Object>>() {
                                }
                        );

        if (repository == null) {
            throw new RuntimeException(
                    "Repository not found"
            );
        }

        return repository;
    }

    private List<Map<String, Object>> getRepositoryContents(
            String username,
            String repositoryName) {

        List<Map<String, Object>> contents =
                restClient.get()
                        .uri(
                                "/repos/{username}/{repository}/contents",
                                username,
                                repositoryName
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        List<Map<String, Object>>>() {
                                }
                        );

        if (contents == null) {
            return List.of();
        }

        return contents;
    }

    private Map<String, Object> analyzeFiles(
            List<Map<String, Object>> contents) {

        List<String> files =
                new ArrayList<>();

        List<String> directories =
                new ArrayList<>();

        boolean hasPomXml = false;
        boolean hasPackageJson = false;
        boolean hasDockerfile = false;
        boolean hasRequirementsTxt = false;
        boolean hasPythonFiles = false;
        boolean hasJavaFiles = false;

        for (Map<String, Object> item : contents) {

            String name =
                    String.valueOf(
                            item.get("name")
                    );

            String type =
                    String.valueOf(
                            item.get("type")
                    );

            if ("dir".equals(type)) {

                directories.add(name);

            } else {

                files.add(name);

                String lowerName =
                        name.toLowerCase();

                if ("pom.xml".equals(lowerName)) {
                    hasPomXml = true;
                }

                if ("package.json".equals(lowerName)) {
                    hasPackageJson = true;
                }

                if ("dockerfile".equals(lowerName)) {
                    hasDockerfile = true;
                }

                if ("requirements.txt".equals(lowerName)) {
                    hasRequirementsTxt = true;
                }

                if (lowerName.endsWith(".py")) {
                    hasPythonFiles = true;
                }

                if (lowerName.endsWith(".java")) {
                    hasJavaFiles = true;
                }
            }
        }

        List<Map<String, Object>> signals =
                new ArrayList<>();

        addSignal(
                signals,
                "Java",
                hasJavaFiles,
                "Java source files found"
        );

        addSignal(
                signals,
                "Maven",
                hasPomXml,
                "pom.xml found"
        );

        addSignal(
                signals,
                "React / JavaScript",
                hasPackageJson,
                "package.json found"
        );

        addSignal(
                signals,
                "Docker",
                hasDockerfile,
                "Dockerfile found"
        );

        addSignal(
                signals,
                "Python",
                hasPythonFiles,
                "Python source files found"
        );

        addSignal(
                signals,
                "Python Dependencies",
                hasRequirementsTxt,
                "requirements.txt found"
        );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "files",
                files
        );

        result.put(
                "directories",
                directories
        );

        result.put(
                "signals",
                signals
        );

        return result;
    }

    private void addSignal(
            List<Map<String, Object>> signals,
            String skill,
            boolean found,
            String reason) {

        if (!found) {
            return;
        }

        Map<String, Object> signal =
                new LinkedHashMap<>();

        signal.put(
                "skill",
                skill
        );

        signal.put(
                "status",
                "FOUND"
        );

        signal.put(
                "reason",
                reason
        );

        signals.add(signal);
    }
}