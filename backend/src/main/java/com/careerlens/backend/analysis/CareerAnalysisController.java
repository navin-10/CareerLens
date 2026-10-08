package com.careerlens.backend.analysis;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analyze")
@CrossOrigin(origins = "http://localhost:5173")
public class CareerAnalysisController {

    private final CareerAnalysisService careerAnalysisService;

    public CareerAnalysisController(
            CareerAnalysisService careerAnalysisService
    ) {
        this.careerAnalysisService = careerAnalysisService;
    }

    @PostMapping(
            value = "/career",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> analyzeCareer(
            @RequestPart("resume") MultipartFile resume,
            @RequestPart("role") String role
    ) {

        try {

            if (resume == null || resume.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Resume file is required.");
            }

            if (role == null || role.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Target role is required.");
            }

            Object result =
                    careerAnalysisService.analyzeCareer(
                            resume,
                            role
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(
                            "Career analysis failed: "
                                    + e.getMessage()
                    );
        }
    }
}