package com.careerlens.backend.skill;

import com.careerlens.backend.config.RoleConfigLoader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SkillNormalizer {

    private final Map<String, String> canonicalSkillsByAlias;

    public SkillNormalizer(RoleConfigLoader roleConfigLoader) {
        canonicalSkillsByAlias = new LinkedHashMap<>();

        roleConfigLoader.getSkills().forEach((canonicalSkill, aliases) -> {
            canonicalSkillsByAlias.put(
                    normalize(canonicalSkill),
                    canonicalSkill
            );
            for (String alias : aliases) {
                canonicalSkillsByAlias.put(normalize(alias), canonicalSkill);
            }
        });
    }

    public List<String> normalizeSkills(List<String> skills) {
        List<String> normalizedSkills = new ArrayList<>();
        for (String skill : skills) {
            if (skill == null) {
                continue;
            }

            String canonicalSkill =
                    canonicalSkillsByAlias.get(normalize(skill));
            if (canonicalSkill != null
                    && !normalizedSkills.contains(canonicalSkill)) {
                normalizedSkills.add(canonicalSkill);
            }
        }
        return normalizedSkills;
    }

    private String normalize(String skill) {
        return skill.trim().toLowerCase(Locale.ROOT);
    }
}
