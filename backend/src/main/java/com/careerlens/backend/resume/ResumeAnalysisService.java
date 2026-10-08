package com.careerlens.backend.resume;

import com.careerlens.backend.config.RoleConfigLoader;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeAnalysisService {

    private final RoleConfigLoader roleConfigLoader;

    public ResumeAnalysisService(
            RoleConfigLoader roleConfigLoader
    ) {
        this.roleConfigLoader = roleConfigLoader;
    }

    public String analyzeResume(
            String resumeText
    ) {

        if (
                resumeText == null
                        || resumeText.isBlank()
        ) {

            return """
                    {
                      "candidate": "Unknown",
                      "skills": [],
                      "projects": [],
                      "links": {}
                    }
                    """;
        }

        String candidate =
                extractCandidateName(
                        resumeText
                );

        List<String> skills =
                extractSkills(
                        resumeText
                );

        List<Map<String, String>> projects =
                extractProjects(
                        resumeText
                );

        Map<String, String> links =
                extractLinks(
                        resumeText
                );

        return convertToJson(
                candidate,
                skills,
                projects,
                links
        );
    }

    /*
     * ============================================================
     * CANDIDATE NAME
     * ============================================================
     */

    private String extractCandidateName(
            String resumeText
    ) {

        String[] lines =
                resumeText.split(
                        "\\R"
                );

        for (String line : lines) {

            String cleaned =
                    line.trim();

            if (cleaned.isBlank()) {
                continue;
            }

            if (cleaned.length() > 60) {
                continue;
            }

            if (cleaned.matches(".*\\d.*")) {
                continue;
            }

            String lower =
                    cleaned.toLowerCase(
                            Locale.ROOT
                    );

            if (
                    lower.contains("resume")
                            || lower.contains("curriculum")
                            || lower.contains("email")
                            || lower.contains("phone")
                            || lower.contains("@")
            ) {
                continue;
            }

            return cleaned;
        }

        return "Unknown";
    }

    /*
     * ============================================================
     * SKILLS
     * ============================================================
     */

    private List<String> extractSkills(
            String resumeText
    ) {

        List<String> detectedSkills =
                new ArrayList<>();

        Map<String, String[]> skillAliases =
                roleConfigLoader.getSkills();

        for (
                Map.Entry<String, String[]> entry :
                skillAliases.entrySet()
        ) {

            String canonicalSkill =
                    entry.getKey();

            String[] aliases =
                    entry.getValue();

            boolean found = false;

            for (String alias : aliases) {

                if (
                        alias == null
                                || alias.isBlank()
                ) {
                    continue;
                }

                String normalizedAlias =
                        alias
                                .toLowerCase(
                                        Locale.ROOT
                                )
                                .trim();

                String regex =
                        "(?<![a-z0-9])"
                                + Pattern.quote(
                                normalizedAlias
                        )
                                + "(?![a-z0-9])";

                Pattern pattern =
                        Pattern.compile(
                                regex,
                                Pattern.CASE_INSENSITIVE
                        );

                Matcher matcher =
                        pattern.matcher(
                                resumeText
                        );

                if (matcher.find()) {

                    found = true;
                    break;
                }
            }

            if (
                    found
                            && !detectedSkills.contains(
                            canonicalSkill
                    )
            ) {

                detectedSkills.add(
                        canonicalSkill
                );
            }
        }

        return detectedSkills;
    }

    /*
     * ============================================================
     * PROJECTS
     * ============================================================
     */

    private List<Map<String, String>> extractProjects(
            String resumeText
    ) {

        List<Map<String, String>> projects =
                new ArrayList<>();

        String lower =
                resumeText.toLowerCase(
                        Locale.ROOT
                );

        int projectIndex =
                lower.indexOf(
                        "projects"
                );

        if (projectIndex < 0) {
            return projects;
        }

        String projectSection =
                resumeText.substring(
                        projectIndex
                );

        String[] lines =
                projectSection.split(
                        "\\R"
                );

        String currentProject =
                null;

        StringBuilder description =
                new StringBuilder();

        for (String line : lines) {

            String cleaned =
                    line.trim();

            if (cleaned.isBlank()) {
                continue;
            }

            String lowerLine =
                    cleaned.toLowerCase(
                            Locale.ROOT
                    );

            if (
                    lowerLine.equals("education")
                            || lowerLine.equals("experience")
                            || lowerLine.equals("certifications")
                            || lowerLine.equals("achievements")
            ) {
                break;
            }

            boolean possibleProjectName =
                    !cleaned.startsWith("-")
                            && !cleaned.startsWith("•")
                            && cleaned.length() < 100;

            if (
                    possibleProjectName
                            && !cleaned.equalsIgnoreCase(
                            "projects"
                    )
            ) {

                if (currentProject != null) {

                    Map<String, String> project =
                            new LinkedHashMap<>();

                    project.put(
                            "name",
                            currentProject
                    );

                    project.put(
                            "description",
                            description
                                    .toString()
                                    .trim()
                    );

                    projects.add(
                            project
                    );
                }

                currentProject =
                        cleaned;

                description.setLength(0);

            } else if (
                    currentProject != null
            ) {

                String cleanedDescription =
                        cleaned
                                .replaceFirst(
                                        "^[•\\-]\\s*",
                                        ""
                                );

                if (description.length() > 0) {
                    description.append(" ");
                }

                description.append(
                        cleanedDescription
                );
            }
        }

        if (currentProject != null) {

            Map<String, String> project =
                    new LinkedHashMap<>();

            project.put(
                    "name",
                    currentProject
            );

            project.put(
                    "description",
                    description
                            .toString()
                            .trim()
            );

            projects.add(
                    project
            );
        }

        return projects;
    }

    /*
     * ============================================================
     * URL EXTRACTION
     * ============================================================
     */

    private Map<String, String> extractLinks(
            String resumeText
    ) {

        Map<String, String> links =
                new LinkedHashMap<>();

        /*
         * First detect complete URLs.
         *
         * Supports:
         *
         * https://github.com/user
         * http://github.com/user
         * www.github.com/user
         * github.com/user
         * linkedin.com/in/user
         * leetcode.com/u/user
         * kaggle.com/user
         */

        Pattern urlPattern =
                Pattern.compile(
                        "(?i)(?:(?:https?://|www\\.)?"
                                + "(?:github\\.com|linkedin\\.com|leetcode\\.com|kaggle\\.com)"
                                + "/[^\\s<>\"'\\]\\[\\)\\(]+)"
                );

        Matcher matcher =
                urlPattern.matcher(
                        resumeText
                );

        while (matcher.find()) {

            String url =
                    cleanUrl(
                            matcher.group()
                    );

            if (url.isBlank()) {
                continue;
            }

            String lowerUrl =
                    url.toLowerCase(
                            Locale.ROOT
                    );

            if (
                    lowerUrl.contains(
                            "github.com/"
                    )
                    && !links.containsKey(
                            "github"
                    )
            ) {

                links.put(
                        "github",
                        normalizeUrl(
                                url
                        )
                );

            } else if (
                    lowerUrl.contains(
                            "linkedin.com/"
                    )
                    && !links.containsKey(
                            "linkedin"
                    )
            ) {

                links.put(
                        "linkedin",
                        normalizeUrl(
                                url
                        )
                );

            } else if (
                    lowerUrl.contains(
                            "leetcode.com/"
                    )
                    && !links.containsKey(
                            "leetcode"
                    )
            ) {

                links.put(
                        "leetcode",
                        normalizeUrl(
                                url
                        )
                );

            } else if (
                    lowerUrl.contains(
                            "kaggle.com/"
                    )
                    && !links.containsKey(
                            "kaggle"
                    )
            ) {

                links.put(
                        "kaggle",
                        normalizeUrl(
                                url
                        )
                );
            }
        }

        /*
         * Second pass:
         *
         * Some PDF extractors can separate a URL from
         * surrounding formatting. Search explicitly for
         * platform domains.
         */

        if (!links.containsKey("github")) {

            String githubUrl =
                    extractPlatformUrl(
                            resumeText,
                            "github.com"
                    );

            if (githubUrl != null) {

                links.put(
                        "github",
                        githubUrl
                );
            }
        }

        if (!links.containsKey("linkedin")) {

            String linkedinUrl =
                    extractPlatformUrl(
                            resumeText,
                            "linkedin.com"
                    );

            if (linkedinUrl != null) {

                links.put(
                        "linkedin",
                        linkedinUrl
                );
            }
        }

        if (!links.containsKey("leetcode")) {

            String leetcodeUrl =
                    extractPlatformUrl(
                            resumeText,
                            "leetcode.com"
                    );

            if (leetcodeUrl != null) {

                links.put(
                        "leetcode",
                        leetcodeUrl
                );
            }
        }

        if (!links.containsKey("kaggle")) {

            String kaggleUrl =
                    extractPlatformUrl(
                            resumeText,
                            "kaggle.com"
                    );

            if (kaggleUrl != null) {

                links.put(
                        "kaggle",
                        kaggleUrl
                );
            }
        }

        return links;
    }

    private String extractPlatformUrl(
            String text,
            String domain
    ) {

        String lower =
                text.toLowerCase(
                        Locale.ROOT
                );

        int index =
                lower.indexOf(
                        domain
                );

        if (index < 0) {
            return null;
        }

        int end =
                index + domain.length();

        while (end < text.length()) {

            char c =
                    text.charAt(
                            end
                    );

            if (
                    Character.isWhitespace(c)
                            || c == ')'
                            || c == ']'
                            || c == '}'
                            || c == '>'
                            || c == '<'
                            || c == '"'
                            || c == '\''
                            || c == ','
                            || c == ';'
            ) {
                break;
            }

            end++;
        }

        String result =
                text.substring(
                        index,
                        end
                );

        result =
                cleanUrl(
                        result
                );

        return normalizeUrl(
                result
        );
    }

    private String cleanUrl(
            String url
    ) {

        if (url == null) {
            return "";
        }

        String cleaned =
                url.trim();

        cleaned =
                cleaned.replaceAll(
                        "^[\\[\\(\\{<]+",
                        ""
                );

        cleaned =
                cleaned.replaceAll(
                        "[.,;:!?\\]\\)\\}>]+$",
                        ""
                );

        return cleaned;
    }

    private String normalizeUrl(
            String url
    ) {

        if (url == null || url.isBlank()) {
            return "";
        }

        String normalized =
                url.trim();

        if (
                normalized.startsWith(
                        "www."
                )
        ) {

            normalized =
                    "https://"
                            + normalized;

        } else if (
                !normalized.startsWith(
                        "http://"
                )
                && !normalized.startsWith(
                        "https://"
                )
        ) {

            normalized =
                    "https://"
                            + normalized;
        }

        return normalized;
    }

    /*
     * ============================================================
     * JSON CONVERSION
     * ============================================================
     */

    private String convertToJson(
            String candidate,
            List<String> skills,
            List<Map<String, String>> projects,
            Map<String, String> links
    ) {

        StringBuilder json =
                new StringBuilder();

        json.append("{");

        json.append(
                "\"candidate\":\""
        );

        json.append(
                escapeJson(
                        candidate
                )
        );

        json.append("\",");

        json.append("\"skills\":[");

        for (int i = 0;
             i < skills.size();
             i++) {

            if (i > 0) {
                json.append(",");
            }

            json.append("\"");

            json.append(
                    escapeJson(
                            skills.get(i)
                    )
            );

            json.append("\"");
        }

        json.append("],");

        json.append("\"projects\":[");

        for (int i = 0;
             i < projects.size();
             i++) {

            if (i > 0) {
                json.append(",");
            }

            Map<String, String> project =
                    projects.get(i);

            json.append("{");

            json.append(
                    "\"name\":\""
            );

            json.append(
                    escapeJson(
                            project.get(
                                    "name"
                            )
                    )
            );

            json.append("\",");

            json.append(
                    "\"description\":\""
            );

            json.append(
                    escapeJson(
                            project.get(
                                    "description"
                            )
                    )
            );

            json.append("\"");

            json.append("}");
        }

        json.append("],");

        json.append("\"links\":{");

        int index = 0;

        for (
                Map.Entry<String, String> entry :
                links.entrySet()
        ) {

            if (index++ > 0) {
                json.append(",");
            }

            json.append("\"");

            json.append(
                    escapeJson(
                            entry.getKey()
                    )
            );

            json.append("\":\"");

            json.append(
                    escapeJson(
                            entry.getValue()
                    )
            );

            json.append("\"");
        }

        json.append("}");

        json.append("}");

        return json.toString();
    }

    private String escapeJson(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\n",
                        "\\n"
                );
    }
}