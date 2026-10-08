package com.careerlens.backend.github;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;
    private final GitHubEvidenceService gitHubEvidenceService;

    public GitHubController(
            GitHubService gitHubService,
            GitHubEvidenceService gitHubEvidenceService) {

        this.gitHubService = gitHubService;
        this.gitHubEvidenceService =
                gitHubEvidenceService;
    }

    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeGitHub(
            @RequestParam String username) {

        try {

            Map<String, Object> result =
                    gitHubService.analyzeUser(username);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            Map<String, Object> error =
                    new LinkedHashMap<>();

            error.put(
                    "error",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(error);

        } catch (Exception e) {

            Map<String, Object> error =
                    new LinkedHashMap<>();

            error.put(
                    "error",
                    "Failed to analyze GitHub profile"
            );

            error.put(
                    "details",
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(error);
        }
    }

    @GetMapping("/repository")
    public ResponseEntity<Map<String, Object>> analyzeRepository(
            @RequestParam String username,
            @RequestParam String repository) {

        try {

            Map<String, Object> result =
                    gitHubEvidenceService.analyzeRepository(
                            username,
                            repository
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            Map<String, Object> error =
                    new LinkedHashMap<>();

            error.put(
                    "error",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(error);

        } catch (Exception e) {

            Map<String, Object> error =
                    new LinkedHashMap<>();

            error.put(
                    "error",
                    "Failed to analyze repository"
            );

            error.put(
                    "details",
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(error);
        }
    }
}