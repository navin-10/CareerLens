package com.careerlens.backend.resume;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final ObjectMapper objectMapper;

    public ResumeController(
            ResumeService resumeService,
            ResumeAnalysisService resumeAnalysisService,
            ObjectMapper objectMapper) {

        this.resumeService = resumeService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> uploadResume(
            @RequestParam("file") MultipartFile file) {

        try {

            // 1. Extract text from resume
            String extractedText =
                    resumeService.extractText(file);

            // 2. Analyze resume
            String analysisJson =
                    resumeAnalysisService.analyzeResume(extractedText);

            // 3. Convert analysis JSON string into a real JSON object
            Object analysis =
                    objectMapper.readValue(
                            analysisJson,
                            Object.class
                    );

            // 4. Build response
            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "filename",
                    file.getOriginalFilename()
            );

            response.put(
                    "characterCount",
                    extractedText.length()
            );

            response.put(
                    "text",
                    extractedText
            );

            response.put(
                    "analysis",
                    analysis
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "error",
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);

        } catch (IOException e) {

            e.printStackTrace();

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "error",
                    "Failed to process resume"
            );

            response.put(
                    "details",
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);

        } catch (Exception e) {

            e.printStackTrace();

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "error",
                    "Failed to analyze resume"
            );

            response.put(
                    "details",
                    e.getMessage()
            );

            response.put(
                    "exception",
                    e.getClass().getName()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}