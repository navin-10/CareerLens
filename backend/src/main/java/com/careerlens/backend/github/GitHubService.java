package com.careerlens.backend.github;

import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GitHubService {

    private final RestClient restClient;

    public GitHubService() {
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

    public Map<String, Object> analyzeUser(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "GitHub username is required"
            );
        }

        String cleanUsername =
                username.trim();

        // Get GitHub profile
        Map<String, Object> profile =
                restClient.get()
                        .uri("/users/{username}", cleanUsername)
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        Map<String, Object>>() {
                                }
                        );

        if (profile == null) {
            throw new RuntimeException(
                    "GitHub user not found"
            );
        }

        // Get public repositories
        List<Map<String, Object>> repositories =
                restClient.get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path(
                                                "/users/{username}/repos"
                                        )
                                        .queryParam(
                                                "per_page",
                                                100
                                        )
                                        .queryParam(
                                                "sort",
                                                "updated"
                                        )
                                        .build(cleanUsername)
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        List<Map<String, Object>>>() {
                                }
                        );

        if (repositories == null) {
            repositories = List.of();
        }

        // Create simplified response
        Map<String, Object> result =
                new java.util.LinkedHashMap<>();

        result.put(
                "username",
                profile.get("login")
        );

        result.put(
                "name",
                profile.get("name")
        );

        result.put(
                "bio",
                profile.get("bio")
        );

        result.put(
                "publicRepositories",
                profile.get("public_repos")
        );

        result.put(
                "followers",
                profile.get("followers")
        );

        result.put(
                "following",
                profile.get("following")
        );

        result.put(
                "profileUrl",
                profile.get("html_url")
        );

        result.put(
                "repositories",
                simplifyRepositories(repositories)
        );

        return result;
    }

    private List<Map<String, Object>> simplifyRepositories(
            List<Map<String, Object>> repositories) {

        return repositories.stream()
                .map(repository -> {

                    Map<String, Object> result =
                            new java.util.LinkedHashMap<>();

                    result.put(
                            "name",
                            repository.get("name")
                    );

                    result.put(
                            "description",
                            repository.get("description")
                    );

                    result.put(
                            "language",
                            repository.get("language")
                    );

                    result.put(
                            "stars",
                            repository.get("stargazers_count")
                    );

                    result.put(
                            "forks",
                            repository.get("forks_count")
                    );

                    result.put(
                            "url",
                            repository.get("html_url")
                    );

                    result.put(
                            "updatedAt",
                            repository.get("updated_at")
                    );

                    result.put(
                            "topics",
                            repository.get("topics")
                    );

                    return result;
                })
                .toList();
    }
}