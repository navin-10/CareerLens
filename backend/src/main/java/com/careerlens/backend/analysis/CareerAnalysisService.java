package com.careerlens.backend.analysis;

import com.careerlens.backend.config.RoleConfig;
import com.careerlens.backend.config.RoleConfigLoader;
import com.careerlens.backend.github.GitHubService;
import com.careerlens.backend.resume.ResumeAnalysisService;
import com.careerlens.backend.resume.ResumeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CareerAnalysisService {

    private final ResumeService resumeService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final GitHubService gitHubService;
    private final RoleConfigLoader roleConfigLoader;

    /*
     * We create the ObjectMapper locally instead of injecting it
     * as a Spring bean.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CareerAnalysisService(
            ResumeService resumeService,
            ResumeAnalysisService resumeAnalysisService,
            GitHubService gitHubService,
            RoleConfigLoader roleConfigLoader
    ) {
        this.resumeService = resumeService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.gitHubService = gitHubService;
        this.roleConfigLoader = roleConfigLoader;
    }

    public Map<String, Object> analyzeCareer(
            MultipartFile resume,
            String roleId
    ) throws Exception {

        /*
         * ---------------------------------------------------------
         * 1. Extract resume text
         * ---------------------------------------------------------
         */

        String resumeText =
                resumeService.extractText(resume);

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException(
                    "Could not extract text from the resume."
            );
        }

        /*
         * ---------------------------------------------------------
         * 2. Analyze resume
         * ---------------------------------------------------------
         */

        String resumeAnalysisJson =
                resumeAnalysisService.analyzeResume(resumeText);

        JsonNode resumeAnalysis =
                objectMapper.readTree(resumeAnalysisJson);

        /*
         * ---------------------------------------------------------
         * 3. Extract candidate name
         * ---------------------------------------------------------
         */

        String candidate =
                getTextValue(
                        resumeAnalysis,
                        "candidate"
                );

        if (candidate == null || candidate.isBlank()) {
            candidate = extractCandidateName(resumeText);
        }

        /*
         * ---------------------------------------------------------
         * 4. Extract resume skills
         * ---------------------------------------------------------
         */

        List<String> resumeSkills =
                extractSkills(resumeAnalysis);

        /*
         * ---------------------------------------------------------
         * 5. Extract URLs automatically
         * ---------------------------------------------------------
         */

        Map<String, String> links =
                extractLinks(
                        resumeAnalysis,
                        resumeText
                );

        /*
         * ---------------------------------------------------------
         * 6. Automatically detect GitHub username
         * ---------------------------------------------------------
         */

        String githubUrl =
                links.get("github");

        String githubUsername =
                extractGitHubUsername(githubUrl);

        /*
         * ---------------------------------------------------------
         * 7. Load target role
         * ---------------------------------------------------------
         */

        RoleConfig roleConfig =
                roleConfigLoader.getRoles().get(roleId);

        if (roleConfig == null) {
            throw new IllegalArgumentException(
                    "Invalid target role: " + roleId
            );
        }

        String targetRole =
                roleConfig.getName();

        List<String> roleSkills =
                roleConfig.getSkills();

        /*
         * ---------------------------------------------------------
         * 8. Analyze GitHub automatically
         * ---------------------------------------------------------
         */

        Map<String, Object> githubData;

        if (githubUsername != null
                && !githubUsername.isBlank()) {

            githubData =
                    analyzeGitHub(
                            githubUsername
                    );

        } else {

            githubData =
                    createUnavailableGitHubData(
                            githubUrl
                    );
        }

        /*
         * ---------------------------------------------------------
         * 9. Verify role skills
         * ---------------------------------------------------------
         */

        List<Map<String, Object>> skillVerification =
                new ArrayList<>();

        List<Map<String, Object>> skillGaps =
                new ArrayList<>();

        for (String skill : roleSkills) {

            boolean claimedOnResume =
                    containsSkill(
                            resumeSkills,
                            skill
                    );

            List<Map<String, Object>> evidence =
                    findEvidence(
                            skill,
                            githubData
                    );

            boolean githubEvidence =
                    !evidence.isEmpty();

            String status;

            int score;

            if (claimedOnResume && githubEvidence) {

                status = "VERIFIED";
                score = 90;

            } else if (claimedOnResume) {

                status = "UNVERIFIED";
                score = 20;

            } else if (githubEvidence) {

                status = "HIDDEN STRENGTH";
                score = 75;

            } else {

                status = "MISSING";
                score = 0;
            }

            Map<String, Object> verification =
                    new LinkedHashMap<>();

            verification.put(
                    "skill",
                    skill
            );

            verification.put(
                    "claimedOnResume",
                    claimedOnResume
            );

            verification.put(
                    "githubEvidence",
                    githubEvidence
            );

            verification.put(
                    "status",
                    status
            );

            verification.put(
                    "score",
                    score
            );

            verification.put(
                    "evidence",
                    evidence
            );

            skillVerification.add(
                    verification
            );

            if (
                    "MISSING".equals(status)
                    || "UNVERIFIED".equals(status)
            ) {

                Map<String, Object> gap =
                        new LinkedHashMap<>();

                gap.put(
                        "skill",
                        skill
                );

                if ("UNVERIFIED".equals(status)) {

                    gap.put(
                            "reason",
                            "Claimed on resume but no supporting GitHub evidence was found."
                    );

                } else {

                    gap.put(
                            "reason",
                            "Required for the selected role but not found in the resume or GitHub evidence."
                    );
                }

                skillGaps.add(gap);
            }
        }

        /*
         * ---------------------------------------------------------
         * 10. Calculate readiness score
         * ---------------------------------------------------------
         */

        double skillScore =
                calculateSkillScore(
                        skillVerification
                );

        int repositoryCount =
                getRepositoryCount(
                        githubData
                );

        double activityScore =
                calculateActivityScore(
                        repositoryCount
                );

        double readinessScore =
                Math.round(
                        (
                                skillScore * 0.80
                                        + activityScore * 0.20
                        ) * 100.0
                ) / 100.0;

        String readinessLevel =
                getReadinessLevel(
                        readinessScore
                );

        /*
         * ---------------------------------------------------------
         * 11. Generate recommendations
         * ---------------------------------------------------------
         */

        List<String> recommendations =
                generateRecommendations(
                        skillVerification
                );

        /*
         * ---------------------------------------------------------
         * 12. Extract projects
         * ---------------------------------------------------------
         */

        List<Map<String, String>> projects =
                extractProjects(
                        resumeAnalysis
                );

        /*
         * ---------------------------------------------------------
         * 13. Build final response
         * ---------------------------------------------------------
         */

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "candidate",
                candidate
        );

        result.put(
                "targetRole",
                targetRole
        );

        result.put(
                "roleId",
                roleId
        );

        result.put(
                "readinessScore",
                readinessScore
        );

        result.put(
                "readinessLevel",
                readinessLevel
        );

        result.put(
                "resumeSkills",
                resumeSkills
        );

        result.put(
                "links",
                links
        );

        result.put(
                "skillVerification",
                skillVerification
        );

        result.put(
                "skillGaps",
                skillGaps
        );

        result.put(
                "recommendations",
                recommendations
        );

        result.put(
                "projects",
                projects
        );

        result.put(
                "github",
                githubData
        );

        result.put(
                "analyzer",
                "CareerLens MVP Intelligence Engine"
        );

        return result;
    }

    /*
     * ============================================================
     * GITHUB ANALYSIS
     * ============================================================
     */

    private Map<String, Object> analyzeGitHub(
            String username
    ) {

        Map<String, Object> github =
                new LinkedHashMap<>();

        try {

            Object profile =
                    gitHubService.analyzeUser(
                            username
                    );

            if (profile instanceof Map<?, ?> profileMap) {

                github.putAll(
                        convertMap(
                                profileMap
                        )
                );
            }

            github.put(
                    "username",
                    username
            );

            github.put(
                    "profileUrl",
                    "https://github.com/"
                            + username
            );

            Object repositoriesValue =
                    github.get("repositories");

            List<?> repositories =
                    repositoriesValue instanceof List<?> list
                            ? list
                            : List.of();

            github.put(
                    "repositories",
                    repositories
            );

            github.put(
                    "publicRepositories",
                    repositories.size()
            );

            github.put(
                    "available",
                    true
            );

            return github;

        } catch (Exception e) {

            github.clear();

            github.put(
                    "username",
                    username
            );

            github.put(
                    "profileUrl",
                    "https://github.com/"
                            + username
            );

            github.put(
                    "publicRepositories",
                    0
            );

            github.put(
                    "followers",
                    0
            );

            github.put(
                    "following",
                    0
            );

            github.put(
                    "repositories",
                    List.of()
            );

            github.put(
                    "available",
                    false
            );

            github.put(
                    "error",
                    "GitHub profile could not be analyzed."
            );

            return github;
        }
    }

    private Map<String, Object> createUnavailableGitHubData(
            String githubUrl
    ) {

        Map<String, Object> github =
                new LinkedHashMap<>();

        github.put(
                "username",
                ""
        );

        github.put(
                "name",
                ""
        );

        github.put(
                "bio",
                ""
        );

        github.put(
                "publicRepositories",
                0
        );

        github.put(
                "followers",
                0
        );

        github.put(
                "following",
                0
        );

        github.put(
                "profileUrl",
                githubUrl == null
                        ? ""
                        : githubUrl
        );

        github.put(
                "repositories",
                List.of()
        );

        github.put(
                "available",
                false
        );

        github.put(
                "message",
                githubUrl == null
                        ? "No GitHub profile was found in the resume."
                        : "GitHub profile was found but could not be connected."
        );

        return github;
    }

    /*
     * ============================================================
     * URL EXTRACTION
     * ============================================================
     */

    private Map<String, String> extractLinks(
            JsonNode resumeAnalysis,
            String resumeText
    ) {

        Map<String, String> links =
                new LinkedHashMap<>();

        JsonNode linksNode =
                resumeAnalysis.get("links");

        if (linksNode != null
                && linksNode.isObject()) {

            Iterator<Map.Entry<String, JsonNode>> fields =
                    linksNode.properties().iterator();

            while (fields.hasNext()) {

                Map.Entry<String, JsonNode> field =
                        fields.next();

                String key =
                        field.getKey();

                String value =
                        field.getValue().asText("");

                if (!value.isBlank()) {

                    links.put(
                            key,
                            normalizeUrl(value)
                    );
                }
            }
        }

        /*
         * Fallback: directly scan resume text.
         */

        Pattern urlPattern =
                Pattern.compile(
                        "(https?://[^\\s<>\"']+|www\\.[^\\s<>\"']+)",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher matcher =
                urlPattern.matcher(
                        resumeText
                );

        while (matcher.find()) {

            String url =
                    matcher.group().trim();

            url =
                    url.replaceAll(
                            "[.,;:)\\]}]+$",
                            ""
                    );

            String lowerUrl =
                    url.toLowerCase(
                            Locale.ROOT
                    );

            if (
                    lowerUrl.contains(
                            "github.com"
                    )
                    && !links.containsKey(
                            "github"
                    )
            ) {

                links.put(
                        "github",
                        normalizeUrl(url)
                );

            } else if (
                    lowerUrl.contains(
                            "linkedin.com"
                    )
                    && !links.containsKey(
                            "linkedin"
                    )
            ) {

                links.put(
                        "linkedin",
                        normalizeUrl(url)
                );

            } else if (
                    lowerUrl.contains(
                            "leetcode.com"
                    )
                    && !links.containsKey(
                            "leetcode"
                    )
            ) {

                links.put(
                        "leetcode",
                        normalizeUrl(url)
                );

            } else if (
                    lowerUrl.contains(
                            "kaggle.com"
                    )
                    && !links.containsKey(
                            "kaggle"
                    )
            ) {

                links.put(
                        "kaggle",
                        normalizeUrl(url)
                );

            } else if (
                    !links.containsKey(
                            "portfolio"
                    )
                    && !isKnownPlatform(
                            lowerUrl
                    )
            ) {

                links.put(
                        "portfolio",
                        normalizeUrl(url)
                );
            }
        }

        return links;
    }

    private String normalizeUrl(
            String url
    ) {

        if (url == null || url.isBlank()) {
            return url;
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
        }

        return normalized;
    }

    private boolean isKnownPlatform(
            String url
    ) {

        return url.contains("github.com")
                || url.contains("linkedin.com")
                || url.contains("leetcode.com")
                || url.contains("kaggle.com");
    }

    /*
     * ============================================================
     * GITHUB USERNAME EXTRACTION
     * ============================================================
     */

    private String extractGitHubUsername(
            String githubUrl
    ) {

        if (
                githubUrl == null
                        || githubUrl.isBlank()
        ) {
            return null;
        }

        try {

            String normalized =
                    githubUrl.trim();

            normalized =
                    normalized.replace(
                            "https://",
                            ""
                    );

            normalized =
                    normalized.replace(
                            "http://",
                            ""
                    );

            normalized =
                    normalized.replace(
                            "www.",
                            ""
                    );

            int githubIndex =
                    normalized
                            .toLowerCase(
                                    Locale.ROOT
                            )
                            .indexOf(
                                    "github.com/"
                            );

            if (githubIndex < 0) {
                return null;
            }

            String path =
                    normalized.substring(
                            githubIndex
                                    + "github.com/"
                                            .length()
                    );

            path =
                    path.split(
                            "[?#]"
                    )[0];

            path =
                    path.replaceAll(
                            "/+$",
                            ""
                    );

            if (path.isBlank()) {
                return null;
            }

            String[] parts =
                    path.split("/");

            if (parts.length == 0) {
                return null;
            }

            String username =
                    parts[0].trim();

            if (
                    username.isBlank()
                            || username.equalsIgnoreCase(
                            "login"
                    )
                            || username.equalsIgnoreCase(
                            "signup"
                    )
            ) {
                return null;
            }

            return username;

        } catch (Exception e) {

            return null;
        }
    }

    /*
     * ============================================================
     * SKILL EXTRACTION
     * ============================================================
     */

    private List<String> extractSkills(
            JsonNode resumeAnalysis
    ) {

        List<String> skills =
                new ArrayList<>();

        JsonNode skillsNode =
                resumeAnalysis.get(
                        "skills"
                );

        if (
                skillsNode != null
                        && skillsNode.isArray()
        ) {

            for (JsonNode skill :
                    skillsNode) {

                String value =
                        skill.asText("")
                                .trim();

                if (
                        !value.isBlank()
                                && !skills.contains(
                                value
                        )
                ) {

                    skills.add(value);
                }
            }
        }

        return skills;
    }

    /*
     * ============================================================
     * SKILL MATCHING
     * ============================================================
     */

    private boolean containsSkill(
            List<String> resumeSkills,
            String targetSkill
    ) {

        if (resumeSkills == null) {
            return false;
        }

        String normalizedTarget =
                normalizeSkill(
                        targetSkill
                );

        for (String skill :
                resumeSkills) {

            if (
                    normalizeSkill(
                            skill
                    ).equals(
                            normalizedTarget
                    )
            ) {

                return true;
            }
        }

        return false;
    }

    private String normalizeSkill(
            String skill
    ) {

        if (skill == null) {
            return "";
        }

        return skill
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        ".js",
                        ""
                )
                .replace(
                        "-",
                        ""
                )
                .replace(
                        "_",
                        ""
                )
                .replace(
                        " ",
                        ""
                )
                .trim();
    }

    /*
     * ============================================================
     * GITHUB EVIDENCE
     * ============================================================
     */

    private List<Map<String, Object>> findEvidence(
            String skill,
            Map<String, Object> githubData
    ) {

        List<Map<String, Object>> evidence =
                new ArrayList<>();

        Object repositoriesObject =
                githubData.get(
                        "repositories"
                );

        if (
                !(repositoriesObject
                        instanceof List<?> repositories)
        ) {

            return evidence;
        }

        String normalizedSkill =
                normalizeSkill(
                        skill
                );

        for (Object repository :
                repositories) {

            if (
                    !(repository
                            instanceof Map<?, ?> repo)
            ) {
                continue;
            }

            String repositoryName =
                    getMapString(
                            repo,
                            "name"
                    );

            String description =
                    getMapString(
                            repo,
                            "description"
                    );

            String language =
                    getMapString(
                            repo,
                            "language"
                    );

            String repoText =
                    (
                            repositoryName
                                    + " "
                                    + description
                                    + " "
                                    + language
                    )
                            .toLowerCase(
                                    Locale.ROOT
                            );

            String normalizedRepoText =
                    repoText
                            .replace(
                                    "-",
                                    ""
                            )
                            .replace(
                                    "_",
                                    ""
                            )
                            .replace(
                                    " ",
                                    ""
                            );

            if (
                    normalizedRepoText.contains(
                            normalizedSkill
                    )
            ) {

                Map<String, Object> item =
                        new LinkedHashMap<>();

                item.put(
                        "repository",
                        repositoryName
                );

                item.put(
                        "signal",
                        buildEvidenceSignal(
                                skill,
                                repositoryName,
                                language,
                                description
                        )
                );

                String htmlUrl =
                        getMapString(
                                repo,
                                "html_url"
                        );

                item.put(
                        "repositoryUrl",
                        htmlUrl
                );

                evidence.add(
                        item
                );
            }
        }

        return evidence;
    }

    private String buildEvidenceSignal(
            String skill,
            String repository,
            String language,
            String description
    ) {

        if (
                language != null
                        && !language.isBlank()
                        && normalizeSkill(
                        language
                ).equals(
                        normalizeSkill(
                                skill
                        )
                )
        ) {

            return "Repository "
                    + repository
                    + " uses "
                    + skill
                    + " as its primary language.";
        }

        if (
                description != null
                        && !description.isBlank()
        ) {

            return "Repository "
                    + repository
                    + " description contains evidence related to "
                    + skill
                    + ".";
        }

        return "Repository "
                + repository
                + " contains a signal related to "
                + skill
                + ".";
    }

    /*
     * ============================================================
     * SCORE CALCULATION
     * ============================================================
     */

    private double calculateSkillScore(
            List<Map<String, Object>> verification
    ) {

        if (
                verification == null
                        || verification.isEmpty()
        ) {
            return 0;
        }

        double total = 0;

        for (
                Map<String, Object> item :
                verification
        ) {

            Object score =
                    item.get("score");

            if (score instanceof Number number) {
                total += number.doubleValue();
            }
        }

        return total
                / verification.size();
    }

    private double calculateActivityScore(
            int repositoryCount
    ) {

        if (repositoryCount <= 0) {
            return 0;
        }

        return Math.min(
                repositoryCount * 20.0,
                100.0
        );
    }

    private String getReadinessLevel(
            double score
    ) {

        if (score >= 80) {
            return "JOB READY";
        }

        if (score >= 60) {
            return "NEARLY READY";
        }

        if (score >= 40) {
            return "DEVELOPING";
        }

        return "BEGINNER";
    }

    /*
     * ============================================================
     * RECOMMENDATIONS
     * ============================================================
     */

    private List<String> generateRecommendations(
            List<Map<String, Object>> verification
    ) {

        List<String> recommendations =
                new ArrayList<>();

        for (
                Map<String, Object> item :
                verification
        ) {

            String skill =
                    String.valueOf(
                            item.get(
                                    "skill"
                            )
                    );

            String status =
                    String.valueOf(
                            item.get(
                                    "status"
                            )
                    );

            if ("MISSING".equals(status)) {

                recommendations.add(
                        "Build a practical project using "
                                + skill
                                + " and publish it on GitHub."
                );

            } else if (
                    "UNVERIFIED".equals(
                            status
                    )
            ) {

                recommendations.add(
                        "Add a GitHub project demonstrating "
                                + skill
                                + " to support your resume claim."
                );
            }
        }

        if (recommendations.isEmpty()) {

            recommendations.add(
                    "Continue building real projects and keep your GitHub evidence aligned with your resume."
            );
        }

        return recommendations;
    }

    /*
     * ============================================================
     * PROJECT EXTRACTION
     * ============================================================
     */

    private List<Map<String, String>> extractProjects(
            JsonNode resumeAnalysis
    ) {

        List<Map<String, String>> projects =
                new ArrayList<>();

        JsonNode projectsNode =
                resumeAnalysis.get(
                        "projects"
                );

        if (
                projectsNode != null
                        && projectsNode.isArray()
        ) {

            for (JsonNode project :
                    projectsNode) {

                Map<String, String> item =
                        new LinkedHashMap<>();

                if (project.isObject()) {

                    item.put(
                            "name",
                            getTextValue(
                                    project,
                                    "name"
                            )
                    );

                    item.put(
                            "description",
                            getTextValue(
                                    project,
                                    "description"
                            )
                    );

                } else {

                    item.put(
                            "name",
                            project.asText()
                    );

                    item.put(
                            "description",
                            ""
                    );
                }

                projects.add(item);
            }
        }

        return projects;
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private String getTextValue(
            JsonNode node,
            String field
    ) {

        if (
                node == null
                        || node.get(field) == null
        ) {
            return "";
        }

        return node
                .get(field)
                .asText("")
                .trim();
    }

    private String extractCandidateName(
            String resumeText
    ) {

        if (
                resumeText == null
                        || resumeText.isBlank()
        ) {
            return "Candidate";
        }

        String[] lines =
                resumeText.split(
                        "\\R"
                );

        for (String line :
                lines) {

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

            return cleaned;
        }

        return "Candidate";
    }

    private int getRepositoryCount(
            Map<String, Object> githubData
    ) {

        Object value =
                githubData.get(
                        "publicRepositories"
                );

        if (value instanceof Number number) {
            return number.intValue();
        }

        Object repositories =
                githubData.get(
                        "repositories"
                );

        if (
                repositories
                        instanceof List<?> list
        ) {

            return list.size();
        }

        return 0;
    }

    private String getMapString(
            Map<?, ?> map,
            String key
    ) {

        Object value =
                map.get(key);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private Map<String, Object> convertMap(
            Map<?, ?> source
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        for (
                Map.Entry<?, ?> entry :
                source.entrySet()
        ) {

            result.put(
                    String.valueOf(
                            entry.getKey()
                    ),
                    entry.getValue()
            );
        }

        return result;
    }
}