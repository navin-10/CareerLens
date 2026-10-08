import { useState } from "react";
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  GitBranch,
  Globe2,
  Loader2,
  ShieldCheck,
  Sparkles,
  Target,
  Upload,
  XCircle,
} from "lucide-react";

import {
  analyzeCareer,
  type CareerAnalysis,
} from "./services/api";

function App() {
  const [resume, setResume] = useState<File | null>(null);
  const [role, setRole] = useState("java-fullstack");
  const [analysis, setAnalysis] = useState<CareerAnalysis | null>(null);
  const [loading, setLoading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState("");

  const roles = [
    {
      id: "java-backend",
      name: "Java Backend Developer",
    },
    {
      id: "java-fullstack",
      name: "Java Full Stack Developer",
    },
    {
      id: "frontend",
      name: "Frontend Developer",
    },
    {
      id: "python",
      name: "Python Developer",
    },
  ];

  const handleFile = (file: File | null) => {
    if (!file) return;

    const validTypes = [
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ];

    const validExtension =
      file.name.toLowerCase().endsWith(".pdf") ||
      file.name.toLowerCase().endsWith(".docx");

    if (!validTypes.includes(file.type) && !validExtension) {
      setError("Please upload a PDF or DOCX resume.");
      return;
    }

    setResume(file);
    setError("");
    setAnalysis(null);
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragActive(false);

    const file = event.dataTransfer.files?.[0];

    if (file) {
      handleFile(file);
    }
  };

  const handleAnalyze = async () => {
    if (!resume) {
      setError("Please upload your resume first.");
      return;
    }

    if (!role) {
      setError("Please select a target role.");
      return;
    }

    setLoading(true);
    setError("");
    setAnalysis(null);

    try {
      const result = await analyzeCareer(
        resume,
        "",
        role
      );

      setAnalysis(result);
    } catch (err: any) {
      console.error(err);

      const message =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "Unable to analyze the resume. Please make sure the backend is running.";

      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const verifiedCount =
    analysis?.skillVerification?.filter(
      (item) =>
        item.status?.toUpperCase() === "VERIFIED"
    ).length ?? 0;

  const evidenceCount =
    analysis?.skillVerification?.reduce(
      (total, item) =>
        total + (item.evidence?.length ?? 0),
      0
    ) ?? 0;

  return (
    <div className="min-h-screen bg-[#070b14] text-white">

      {/* Background */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute left-1/4 top-0 h-96 w-96 rounded-full bg-blue-600/10 blur-3xl" />
        <div className="absolute right-1/4 top-1/3 h-96 w-96 rounded-full bg-violet-600/10 blur-3xl" />
      </div>

      {/* Header */}
      <header className="relative z-10 border-b border-white/10 bg-[#070b14]/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5">

          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-violet-600 font-bold shadow-lg shadow-blue-500/20">
              CL
            </div>

            <div>
              <h1 className="text-lg font-bold tracking-tight">
                CareerLens
              </h1>

              <p className="text-xs text-slate-400">
                Evidence-Based Career Intelligence
              </p>
            </div>
          </div>

          <div className="hidden items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-xs text-slate-300 sm:flex">
            <ShieldCheck className="h-4 w-4 text-emerald-400" />
            Evidence-first analysis
          </div>
        </div>
      </header>

      <main className="relative z-10 mx-auto max-w-7xl px-6 py-12">

        {!analysis ? (
          <>
            {/* Hero */}
            <section className="mx-auto max-w-4xl text-center">

              <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-2 text-sm text-blue-300">
                <Sparkles className="h-4 w-4" />
                AI-powered employability analysis
              </div>

              <h2 className="text-4xl font-bold tracking-tight sm:text-6xl">
                Know how job-ready
                <span className="block bg-gradient-to-r from-blue-400 via-violet-400 to-cyan-400 bg-clip-text text-transparent">
                  you really are.
                </span>
              </h2>

              <p className="mx-auto mt-6 max-w-2xl text-base leading-7 text-slate-400 sm:text-lg">
                Upload your resume and CareerLens will analyze your
                skills, projects, and digital evidence to identify
                strengths, weaknesses, and the next steps for your career.
              </p>
            </section>

            {/* Analysis Card */}
            <section className="mx-auto mt-12 max-w-3xl">

              <div className="rounded-3xl border border-white/10 bg-white/[0.04] p-6 shadow-2xl shadow-black/20 backdrop-blur-xl sm:p-8">

                {/* Resume */}
                <div>
                  <div className="mb-3 flex items-center justify-between">
                    <label className="text-sm font-semibold text-slate-200">
                      Resume
                    </label>

                    <span className="text-xs text-slate-500">
                      PDF / DOCX
                    </span>
                  </div>

                  <div
                    onDragOver={(event) => {
                      event.preventDefault();
                      setDragActive(true);
                    }}
                    onDragLeave={() => setDragActive(false)}
                    onDrop={handleDrop}
                    onClick={() =>
                      document
                        .getElementById("resume-upload")
                        ?.click()
                    }
                    className={`cursor-pointer rounded-2xl border-2 border-dashed p-8 text-center transition ${
                      dragActive
                        ? "border-blue-400 bg-blue-500/10"
                        : "border-white/10 bg-black/20 hover:border-blue-500/40 hover:bg-white/[0.03]"
                    }`}
                  >
                    <input
                      id="resume-upload"
                      type="file"
                      accept=".pdf,.docx"
                      className="hidden"
                      onChange={(event) =>
                        handleFile(
                          event.target.files?.[0] ?? null
                        )
                      }
                    />

                    {resume ? (
                      <div className="flex flex-col items-center">

                        <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-500/10">
                          <CheckCircle2 className="h-6 w-6 text-emerald-400" />
                        </div>

                        <p className="font-medium text-white">
                          {resume.name}
                        </p>

                        <p className="mt-1 text-xs text-slate-500">
                          {(resume.size / 1024).toFixed(1)} KB
                        </p>

                        <p className="mt-4 text-xs text-blue-400">
                          Click to choose another file
                        </p>
                      </div>
                    ) : (
                      <div className="flex flex-col items-center">

                        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-500/10">
                          <Upload className="h-7 w-7 text-blue-400" />
                        </div>

                        <p className="font-medium text-slate-200">
                          Drop your resume here
                        </p>

                        <p className="mt-2 text-sm text-slate-500">
                          or click to browse your files
                        </p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Target Role */}
                <div className="mt-6">

                  <label className="mb-3 block text-sm font-semibold text-slate-200">
                    Target Role
                  </label>

                  <div className="relative">
                    <Target className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />

                    <select
                      value={role}
                      onChange={(event) =>
                        setRole(event.target.value)
                      }
                      className="w-full appearance-none rounded-xl border border-white/10 bg-black/30 py-4 pl-12 pr-4 text-sm text-white outline-none transition focus:border-blue-500/60 focus:ring-2 focus:ring-blue-500/10"
                    >
                      {roles.map((item) => (
                        <option
                          key={item.id}
                          value={item.id}
                          className="bg-[#101522]"
                        >
                          {item.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Automatic Evidence Notice */}
                <div className="mt-6 rounded-xl border border-blue-500/20 bg-blue-500/5 p-4">

                  <div className="flex gap-3">

                    <div className="mt-0.5">
                      <Globe2 className="h-5 w-5 text-blue-400" />
                    </div>

                    <div>
                      <p className="text-sm font-semibold text-blue-300">
                        Automatic profile discovery
                      </p>

                      <p className="mt-1 text-xs leading-5 text-slate-400">
                        CareerLens will automatically detect GitHub,
                        portfolio, LinkedIn, LeetCode, and other profile
                        links from your resume. No manual profile URL
                        entry is required.
                      </p>
                    </div>

                  </div>
                </div>

                {/* Error */}
                {error && (
                  <div className="mt-5 flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/5 p-4">

                    <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-400" />

                    <p className="text-sm text-red-300">
                      {error}
                    </p>

                  </div>
                )}

                {/* Analyze */}
                <button
                  onClick={handleAnalyze}
                  disabled={loading || !resume}
                  className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-500 to-violet-600 px-6 py-4 font-semibold text-white shadow-lg shadow-blue-500/20 transition hover:from-blue-400 hover:to-violet-500 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {loading ? (
                    <>
                      <Loader2 className="h-5 w-5 animate-spin" />
                      Analyzing your profile...
                    </>
                  ) : (
                    <>
                      Analyze My Career
                      <ArrowRight className="h-5 w-5" />
                    </>
                  )}
                </button>

              </div>
            </section>

            {/* Feature strip */}
            <section className="mx-auto mt-10 grid max-w-5xl gap-4 sm:grid-cols-3">

              <Feature
                icon={<ShieldCheck />}
                title="Evidence Based"
                description="Claims are checked against observable evidence."
              />

              <Feature
                icon={<Target />}
                title="Role Specific"
                description="Analysis is mapped to your target job role."
              />

              <Feature
                icon={<Sparkles />}
                title="Actionable"
                description="Get concrete recommendations to improve."
              />

            </section>
          </>
        ) : (
          <Dashboard
            analysis={analysis}
            verifiedCount={verifiedCount}
            evidenceCount={evidenceCount}
            onReset={() => {
              setAnalysis(null);
              setResume(null);
              setError("");
            }}
          />
        )}

      </main>
    </div>
  );
}

function Feature({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-5">

      <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500/10 text-blue-400">
        {icon}
      </div>

      <h3 className="font-semibold text-white">
        {title}
      </h3>

      <p className="mt-2 text-sm leading-6 text-slate-500">
        {description}
      </p>

    </div>
  );
}

function Dashboard({
  analysis,
  verifiedCount,
  evidenceCount,
  onReset,
}: {
  analysis: CareerAnalysis;
  verifiedCount: number;
  evidenceCount: number;
  onReset: () => void;
}) {
  const score = Math.round(
    analysis.readinessScore ?? 0
  );

  return (
    <div>

      {/* Dashboard header */}
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">

        <div>
          <p className="text-sm text-blue-400">
            Career Analysis Complete
          </p>

          <h2 className="mt-1 text-3xl font-bold">
            {analysis.targetRole}
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            Evidence-based analysis of your current profile.
          </p>
        </div>

        <button
          onClick={onReset}
          className="flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300 transition hover:bg-white/10"
        >
          Analyze Another Resume
        </button>

      </div>

      {/* Top cards */}
      <div className="grid gap-5 lg:grid-cols-3">

        {/* Score */}
        <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-6">

          <p className="text-sm text-slate-500">
            Job Readiness Score
          </p>

          <div className="mt-4 flex items-end gap-3">

            <span className="text-6xl font-bold tracking-tight">
              {score}
            </span>

            <span className="mb-2 text-slate-500">
              / 100
            </span>

          </div>

          <div className="mt-5 h-2 overflow-hidden rounded-full bg-white/10">

            <div
              className="h-full rounded-full bg-gradient-to-r from-blue-500 to-violet-500"
              style={{
                width: `${Math.min(score, 100)}%`,
              }}
            />

          </div>

          <p className="mt-4 text-sm font-medium text-blue-300">
            {analysis.readinessLevel}
          </p>

        </div>

        {/* Verified */}
        <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-6">

          <p className="text-sm text-slate-500">
            Verified Skills
          </p>

          <div className="mt-4 flex items-center gap-4">

            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/10">
              <CheckCircle2 className="h-7 w-7 text-emerald-400" />
            </div>

            <span className="text-4xl font-bold">
              {verifiedCount}
            </span>

          </div>

          <p className="mt-4 text-sm text-slate-500">
            Skills supported by available evidence.
          </p>

        </div>

        {/* Evidence */}
        <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-6">

          <p className="text-sm text-slate-500">
            Evidence Signals
          </p>

          <div className="mt-4 flex items-center gap-4">

            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-500/10">
              <GitBranch className="h-7 w-7 text-blue-400" />
            </div>

            <span className="text-4xl font-bold">
              {evidenceCount}
            </span>

          </div>

          <p className="mt-4 text-sm text-slate-500">
            Observable project and profile signals.
          </p>

        </div>

      </div>

      {/* Claim vs Reality */}
      <section className="mt-8 rounded-2xl border border-white/10 bg-white/[0.04]">

        <div className="border-b border-white/10 p-6">

          <h3 className="text-xl font-semibold">
            Claim vs Reality
          </h3>

          <p className="mt-1 text-sm text-slate-500">
            Resume claims compared with available evidence.
          </p>

        </div>

        <div className="overflow-x-auto">

          <table className="w-full text-left">

            <thead className="border-b border-white/10 text-xs uppercase tracking-wider text-slate-500">

              <tr>
                <th className="px-6 py-4">
                  Skill
                </th>

                <th className="px-6 py-4">
                  Resume
                </th>

                <th className="px-6 py-4">
                  Evidence
                </th>

                <th className="px-6 py-4">
                  Status
                </th>
              </tr>

            </thead>

            <tbody>

              {analysis.skillVerification?.map(
                (item, index) => {

                  const status =
                    item.status?.toUpperCase();

                  const verified =
                    status === "VERIFIED";

                  const hidden =
                    status === "HIDDEN STRENGTH";

                  const partial =
                    status === "PARTIAL";

                  return (
                    <tr
                      key={`${item.skill}-${index}`}
                      className="border-b border-white/5 last:border-0"
                    >

                      <td className="px-6 py-4 font-medium">
                        {item.skill}
                      </td>

                      <td className="px-6 py-4">

                        {item.claimedOnResume ? (
                          <CheckCircle2 className="h-5 w-5 text-emerald-400" />
                        ) : (
                          <XCircle className="h-5 w-5 text-slate-600" />
                        )}

                      </td>

                      <td className="px-6 py-4">

                        {item.githubEvidence ? (
                          <CheckCircle2 className="h-5 w-5 text-emerald-400" />
                        ) : (
                          <XCircle className="h-5 w-5 text-red-400" />
                        )}

                      </td>

                      <td className="px-6 py-4">

                        <span
                          className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${
                            verified
                              ? "bg-emerald-500/10 text-emerald-400"
                              : hidden
                                ? "bg-blue-500/10 text-blue-400"
                                : partial
                                  ? "bg-amber-500/10 text-amber-400"
                                  : "bg-red-500/10 text-red-400"
                          }`}
                        >
                          {item.status}
                        </span>

                      </td>

                    </tr>
                  );
                }
              )}

            </tbody>

          </table>

        </div>

      </section>

      {/* Gaps and recommendations */}
      <div className="mt-8 grid gap-6 lg:grid-cols-2">

        <section className="rounded-2xl border border-white/10 bg-white/[0.04] p-6">

          <div className="flex items-center gap-3">

            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-500/10">
              <AlertCircle className="h-5 w-5 text-red-400" />
            </div>

            <div>
              <h3 className="font-semibold">
                Skill Gaps
              </h3>

              <p className="text-xs text-slate-500">
                Areas that need improvement.
              </p>
            </div>

          </div>

          <div className="mt-6 space-y-3">

            {analysis.skillGaps?.length ? (
              analysis.skillGaps.map(
                (gap, index) => (
                  <div
                    key={`${gap.skill}-${index}`}
                    className="rounded-xl border border-white/5 bg-black/20 p-4"
                  >

                    <p className="font-medium text-white">
                      {gap.skill}
                    </p>

                    <p className="mt-1 text-sm leading-6 text-slate-500">
                      {gap.reason}
                    </p>

                  </div>
                )
              )
            ) : (
              <p className="text-sm text-emerald-400">
                No major skill gaps detected.
              </p>
            )}

          </div>

        </section>

        <section className="rounded-2xl border border-white/10 bg-white/[0.04] p-6">

          <div className="flex items-center gap-3">

            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-500/10">
              <Sparkles className="h-5 w-5 text-violet-400" />
            </div>

            <div>
              <h3 className="font-semibold">
                Recommended Next Steps
              </h3>

              <p className="text-xs text-slate-500">
                Actions based on your current gaps.
              </p>
            </div>

          </div>

          <div className="mt-6 space-y-3">

            {analysis.recommendations?.length ? (
              analysis.recommendations.map(
                (recommendation, index) => (
                  <div
                    key={index}
                    className="flex gap-3 rounded-xl border border-white/5 bg-black/20 p-4"
                  >

                    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-violet-500/10 text-xs font-bold text-violet-400">
                      {index + 1}
                    </span>

                    <p className="text-sm leading-6 text-slate-300">
                      {recommendation}
                    </p>

                  </div>
                )
              )
            ) : (
              <p className="text-sm text-slate-500">
                Keep building projects and collecting evidence.
              </p>
            )}

          </div>

        </section>

      </div>

      {/* Projects */}
      <section className="mt-8 rounded-2xl border border-white/10 bg-white/[0.04] p-6">

        <h3 className="text-xl font-semibold">
          Detected Projects
        </h3>

        <p className="mt-1 text-sm text-slate-500">
          Projects extracted from your resume.
        </p>

        <div className="mt-6 grid gap-4 md:grid-cols-2">

          {analysis.projects?.length ? (
            analysis.projects.map(
              (project, index) => (
                <div
                  key={`${project.name}-${index}`}
                  className="rounded-xl border border-white/5 bg-black/20 p-5"
                >

                  <h4 className="font-semibold text-white">
                    {project.name}
                  </h4>

                  <p className="mt-2 text-sm leading-6 text-slate-500">
                    {project.description}
                  </p>

                </div>
              )
            )
          ) : (
            <p className="text-sm text-slate-500">
              No projects detected.
            </p>
          )}

        </div>

      </section>

    </div>
  );
}

export default App;