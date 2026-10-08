package com.careerlens.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @GetMapping
    public List<Map<String, String>> getRoles() {

        return List.of(
                Map.of(
                        "id", "java-backend",
                        "name", "Java Backend Developer"
                ),
                Map.of(
                        "id", "java-fullstack",
                        "name", "Java Full Stack Developer"
                ),
                Map.of(
                        "id", "frontend",
                        "name", "Frontend Developer"
                ),
                Map.of(
                        "id", "python",
                        "name", "Python Developer"
                )
        );
    }
}