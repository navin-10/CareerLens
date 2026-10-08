export interface SkillEvidence {
  repository: string;
  signal: string;
  repositoryUrl: string;
}

export interface SkillVerification {
  skill: string;
  claimedOnResume: boolean;
  githubEvidence: boolean;
  status: "VERIFIED" | "UNVERIFIED" | "HIDDEN STRENGTH" | "MISSING" | "PARTIAL" | "WEAK";
  score: number;
  evidence: SkillEvidence[];
}

export interface SkillGap {
  skill: string;
  reason: string;
}

export interface ResumeProject {
  name: string;
  description: string;
}

export interface GitHubProfile {
  username: string;
  name: string | null;
  bio: string | null;
  publicRepositories: number;
  followers: number;
  following: number;
  profileUrl: string;
  repositories: {
    name: string;
    description: string | null;
    language: string | null;
    stars: number;
    forks: number;
    url: string;
    updatedAt: string;
    topics: string[];
  }[];
}

export interface CareerAnalysis {
  candidate: string;
  targetRole: string;
  roleId: string;
  readinessScore: number;
  readinessLevel: string;
  resumeSkills: string[];
  skillVerification: SkillVerification[];
  skillGaps: SkillGap[];
  recommendations: string[];
  projects: ResumeProject[];
  github: GitHubProfile;
  analyzer: string;
}

const API_BASE_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function analyzeCareer(
  resume: File,
  githubUsername: string,
  role: string,
): Promise<CareerAnalysis> {
  const formData = new FormData();
  formData.append("resume", resume);
  formData.append("githubUsername", githubUsername);
  formData.append("role", role);

  const response = await fetch(`${API_BASE_URL}/api/analyze/career`, {
    method: "POST",
    body: formData,
  });

  const responseBody: unknown = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMessage =
      typeof responseBody === "object"
      && responseBody !== null
      && "error" in responseBody
      && typeof responseBody.error === "string"
        ? responseBody.error
        : `Career analysis failed (${response.status})`;
    throw new Error(errorMessage);
  }

  if (typeof responseBody !== "object" || responseBody === null) {
    throw new Error("Career analysis returned an invalid response");
  }

  return responseBody as CareerAnalysis;
}
