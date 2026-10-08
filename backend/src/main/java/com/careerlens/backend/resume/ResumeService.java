package com.careerlens.backend.resume;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeService {

    public String extractText(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String lowerCaseFilename = filename.toLowerCase();

        if (lowerCaseFilename.endsWith(".pdf")) {
            return extractPdfText(file);
        }

        if (lowerCaseFilename.endsWith(".docx")) {
            return extractDocxText(file);
        }

        throw new IllegalArgumentException(
                "Unsupported file type. Please upload a PDF or DOCX file."
        );
    }

    private String extractPdfText(MultipartFile file) throws IOException {

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();

            return stripper.getText(document).trim();
        }
    }

    private String extractDocxText(MultipartFile file) throws IOException {

        try (XWPFDocument document =
                     new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor =
                     new XWPFWordExtractor(document)) {

            return extractor.getText().trim();
        }
    }
}