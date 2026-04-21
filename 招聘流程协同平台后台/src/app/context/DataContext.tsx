import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export interface Candidate {
  id: number;
  name: string;
  position: string;
  department: string | null;
  statusCode: string;
  status: string;
  owner: string | null;
  source: string;
  submittedDate: string;
  updatedDate: string;
  nextAction: string;
}

export interface CandidateDetail extends Candidate {
  phone?: string | null;
  email?: string | null;
  location?: string | null;
  experience?: string | null;
  education?: string | null;
  skillsSummary?: string | null;
  projectSummary?: string | null;
  latestResume?: {
    id: number;
    originalFileName: string;
    contentType: string;
    fileSize: number;
    uploadedAt: string;
    uploadedBy: string;
  } | null;
}

export interface CandidateUpsertPayload {
  name: string;
  position: string;
  departmentId?: number | null;
  ownerId?: number | null;
  source: string;
  submittedDate: string;
  nextAction?: string;
  phone?: string;
  email?: string;
  location?: string;
  experience?: string;
  education?: string;
  skillsSummary?: string;
  projectSummary?: string;
}

export interface DepartmentTask {
  id: number;
  candidateId: number;
  candidateName: string;
  department: string;
  reviewer: string;
  status: string;
  dueAt: string;
  completedAt?: string | null;
  remindedAt?: string | null;
}

export interface DashboardOverview {
  newCandidatesToday: number;
  pendingFeedbackCount: number;
  timeoutCount: number;
  hiredCount: number;
}

export interface DashboardMetric {
  label: string;
  value: number;
}

export interface DepartmentEfficiency {
  department: string;
  averageDays: number;
  completedCount: number;
}

export interface DashboardAlert {
  assignmentId: number;
  candidateId: number;
  candidateName: string;
  position: string;
  department: string;
  overdueHours: number;
}

export interface TimelineEvent {
  id: number;
  eventType: string;
  actorName: string;
  sourceAction: string;
  statusCode: string;
  statusLabel: string;
  note: string;
  occurredAt: string;
}

export interface CandidateFeedback {
  id: number;
  candidateId: number;
  assignmentId: number;
  reviewer: string;
  decision: string;
  feedback: string;
  rejectReason?: string | null;
  nextStep?: string | null;
  suggestedInterviewer?: string | null;
  createdAt: string;
}

export interface InterviewEvaluation {
  id: number;
  interviewer: string;
  result: string;
  score: number;
  evaluation: string;
  strengths?: string | null;
  weaknesses?: string | null;
  suggestion?: string | null;
  createdAt: string;
}

export interface InterviewPlan {
  id: number;
  candidateId: number;
  roundLabel: string;
  interviewer: string;
  status: string;
  scheduledAt: string;
  endsAt: string;
  evaluations: InterviewEvaluation[];
}

export interface LookupDepartment {
  id: number;
  code: string;
  name: string;
}

export interface LookupUser {
  id: number;
  username: string;
  displayName: string;
  email: string;
  departmentId: number | null;
  departmentName: string | null;
}

export interface ParsedCandidateDraft {
  name?: string | null;
  phone?: string | null;
  email?: string | null;
  location?: string | null;
  education?: string | null;
  experience?: string | null;
  skillsSummary?: string | null;
  projectSummary?: string | null;
}

export interface ParseFieldValue {
  value: string;
  confidence: number;
  source: string;
}

export interface ParseIssue {
  severity: string;
  message: string;
}

export interface ParseProject {
  title: string;
  summary: string;
}

export interface ParseReport {
  summary: string;
  highlights: string[];
  extractedSkills: string[];
  projectExperiences: ParseProject[];
  fields: Record<string, ParseFieldValue>;
  issues: ParseIssue[];
}

export interface DecisionReport {
  conclusion: string;
  recommendationScore: number;
  recommendationLevel: string;
  recommendedAction: string;
  strengths: string[];
  risks: string[];
  missingInformation: string[];
  supportingEvidence: string[];
  reasoningSummary: string;
}

export interface AgentJobResult {
  summary?: string | null;
  overallScore?: number | null;
  dimensionScores?: Record<string, number> | null;
  strengths?: string | null;
  risks?: string | null;
  recommendedAction?: string | null;
  rawReasoningDigest?: string | null;
  parsedCandidateDraft?: ParsedCandidateDraft | null;
  parseReport?: ParseReport | null;
  decisionReport?: DecisionReport | null;
}

export interface AgentJob {
  id: number;
  candidateId: number;
  jobType: "ANALYSIS" | "PARSE" | "DECISION";
  status: "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED";
  requestedAt: string;
  completedAt?: string | null;
  result?: AgentJobResult | null;
  lastError?: string | null;
}

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  email: string;
  department: string | null;
  roles: string[];
}

export interface AdvanceCandidatePayload {
  action:
    | "ASSIGN_TO_DEPARTMENT"
    | "MOVE_TO_POOL"
    | "REMIND_REVIEWER"
    | "SCHEDULE_INTERVIEW"
    | "ADVANCE_TO_OFFER_PENDING"
    | "MARK_OFFER_SENT"
    | "MARK_HIRED"
    | "MARK_REJECTED";
  departmentId?: number;
  reviewerId?: number;
  dueAt?: string;
  interviewerId?: number;
  roundLabel?: string;
  scheduledAt?: string;
  endsAt?: string;
  note?: string;
}

interface DataContextType {
  candidates: Candidate[];
  departmentTasks: DepartmentTask[];
  dashboardOverview: DashboardOverview | null;
  funnelMetrics: DashboardMetric[];
  statusDistribution: DashboardMetric[];
  departmentEfficiency: DepartmentEfficiency[];
  alerts: DashboardAlert[];
  currentUser: CurrentUser | null;
  pendingFeedbackCount: number;
  loading: boolean;
  error: string | null;
  authReady: boolean;
  refreshData: () => Promise<void>;
  loadCandidateDetail: (id: number) => Promise<CandidateDetail>;
  loadCandidateTimeline: (id: number) => Promise<TimelineEvent[]>;
  loadCandidateFeedbacks: (id: number) => Promise<CandidateFeedback[]>;
  loadCandidateInterviews: (id: number) => Promise<InterviewPlan[]>;
  createCandidate: (payload: CandidateUpsertPayload) => Promise<CandidateDetail>;
  updateCandidate: (id: number, payload: CandidateUpsertPayload) => Promise<CandidateDetail>;
  uploadResume: (candidateId: number, file: File) => Promise<void>;
  createParseJob: (candidateId: number, hint?: string) => Promise<AgentJob>;
  loadLatestParseJob: (candidateId: number) => Promise<AgentJob>;
  loadLatestParseJobOptional: (candidateId: number) => Promise<AgentJob | null>;
  createDecisionJob: (candidateId: number, focusHint?: string) => Promise<AgentJob>;
  loadLatestDecisionJob: (candidateId: number) => Promise<AgentJob | null>;
  loadDecisionHistory: (candidateId: number) => Promise<AgentJob[]>;
  advanceCandidate: (candidateId: number, payload: AdvanceCandidatePayload) => Promise<CandidateDetail>;
  loadDepartments: () => Promise<LookupDepartment[]>;
  loadUsersByRole: (role: "DEPARTMENT_LEAD" | "INTERVIEWER") => Promise<LookupUser[]>;
  submitDepartmentFeedback: (params: {
    candidateId: number;
    passed: boolean;
    feedback: string;
    rejectReason?: string;
    nextStep?: string;
    suggestedInterviewer?: string;
  }) => Promise<void>;
  getCandidateById: (id: number) => Candidate | undefined;
  getDepartmentTaskByCandidateId: (candidateId: number) => DepartmentTask | undefined;
  downloadResume: (candidateId: number, fileName?: string) => Promise<void>;
  previewResume: (candidateId: number) => Promise<void>;
}

const DataContext = createContext<DataContextType | undefined>(undefined);

function mapCandidate(item: any): Candidate {
  return {
    id: item.id,
    name: item.name,
    position: item.position,
    department: item.department ?? null,
    statusCode: item.statusCode,
    status: item.statusLabel,
    owner: item.owner ?? null,
    source: item.source,
    submittedDate: item.submittedDate,
    updatedDate: item.updatedAt,
    nextAction: item.nextAction,
  };
}

function mapCandidateDetail(detail: any): CandidateDetail {
  return {
    ...mapCandidate(detail),
    phone: detail.phone,
    email: detail.email,
    location: detail.location,
    experience: detail.experience,
    education: detail.education,
    skillsSummary: detail.skillsSummary,
    projectSummary: detail.projectSummary,
    latestResume: detail.latestResume ?? null,
  };
}

function mapAgentJob(job: any): AgentJob {
  return {
    id: job.id,
    candidateId: job.candidateId,
    jobType: job.jobType,
    status: job.status,
    requestedAt: job.requestedAt,
    completedAt: job.completedAt ?? null,
    lastError: job.lastError ?? null,
    result: job.result
      ? {
          summary: job.result.summary ?? null,
          overallScore: job.result.overallScore ?? null,
          dimensionScores: job.result.dimensionScores ?? null,
          strengths: job.result.strengths ?? null,
          risks: job.result.risks ?? null,
          recommendedAction: job.result.recommendedAction ?? null,
          rawReasoningDigest: job.result.rawReasoningDigest ?? null,
          parsedCandidateDraft: job.result.parsedCandidateDraft ?? null,
          parseReport: job.result.parseReport ?? null,
          decisionReport: job.result.decisionReport ?? null,
        }
      : null,
  };
}

async function bootstrapToken() {
  const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      username: "hr",
      password: "Password123!",
    }),
  });

  if (!response.ok) {
    throw new Error("后端登录失败，请先确认 backend 已启动");
  }

  const data = await response.json();
  return data.accessToken as string;
}

export function DataProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [departmentTasks, setDepartmentTasks] = useState<DepartmentTask[]>([]);
  const [dashboardOverview, setDashboardOverview] = useState<DashboardOverview | null>(null);
  const [funnelMetrics, setFunnelMetrics] = useState<DashboardMetric[]>([]);
  const [statusDistribution, setStatusDistribution] = useState<DashboardMetric[]>([]);
  const [departmentEfficiency, setDepartmentEfficiency] = useState<DepartmentEfficiency[]>([]);
  const [alerts, setAlerts] = useState<DashboardAlert[]>([]);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [authReady, setAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const tokenPromiseRef = useRef<Promise<string> | null>(null);

  async function ensureToken() {
    if (token) {
      return token;
    }

    if (!tokenPromiseRef.current) {
      tokenPromiseRef.current = bootstrapToken()
        .then((accessToken) => {
          setToken(accessToken);
          setAuthReady(true);
          return accessToken;
        })
        .finally(() => {
          tokenPromiseRef.current = null;
        });
    }

    return tokenPromiseRef.current;
  }

  async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
    const authToken = await ensureToken();
    const headers = new Headers(init?.headers ?? {});

    if (!(init?.body instanceof FormData) && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
    headers.set("Authorization", `Bearer ${authToken}`);

    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers,
    });

    if (!response.ok) {
      const contentType = response.headers.get("content-type") ?? "";
      if (contentType.includes("application/json")) {
        const payload = await response.json();
        const error = new Error(payload.message ?? `请求失败: ${response.status}`) as Error & { status?: number };
        error.status = response.status;
        throw error;
      }
      const error = new Error((await response.text()) || `请求失败: ${response.status}`) as Error & { status?: number };
      error.status = response.status;
      throw error;
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json() as Promise<T>;
  }

  async function apiRequestOptional<T>(path: string, init?: RequestInit): Promise<T | null> {
    try {
      return await apiRequest<T>(path, init);
    } catch (requestError) {
      if ((requestError as { status?: number })?.status === 404) {
        return null;
      }
      throw requestError;
    }
  }

  async function fetchBlob(path: string) {
    const authToken = await ensureToken();
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        Authorization: `Bearer ${authToken}`,
      },
    });

    if (!response.ok) {
      const message = await response.text();
      throw new Error(message || "文件请求失败");
    }

    return response.blob();
  }

  async function loadCurrentUser() {
    const me = await apiRequest<any>("/api/auth/me");
    setCurrentUser({
      id: me.id,
      username: me.username,
      displayName: me.displayName,
      email: me.email,
      department: me.department ?? null,
      roles: Array.isArray(me.roles) ? me.roles : [],
    });
  }

  async function refreshData() {
    setLoading(true);
    setError(null);
    try {
      const [
        candidateData,
        overviewData,
        funnelData,
        statusData,
        efficiencyData,
        alertData,
        taskData,
      ] = await Promise.all([
        apiRequest<any[]>("/api/candidates"),
        apiRequest<DashboardOverview>("/api/dashboard/overview"),
        apiRequest<DashboardMetric[]>("/api/dashboard/funnel"),
        apiRequest<DashboardMetric[]>("/api/dashboard/status-distribution"),
        apiRequest<DepartmentEfficiency[]>("/api/dashboard/department-efficiency"),
        apiRequest<DashboardAlert[]>("/api/dashboard/alerts"),
        apiRequest<DepartmentTask[]>("/api/department/tasks"),
      ]);

      setCandidates(candidateData.map(mapCandidate));
      setDashboardOverview(overviewData);
      setFunnelMetrics(funnelData);
      setStatusDistribution(statusData);
      setDepartmentEfficiency(efficiencyData);
      setAlerts(alertData);
      setDepartmentTasks(taskData);
      await loadCurrentUser();
      setAuthReady(true);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "前后端联调请求失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadCandidateDetail(id: number): Promise<CandidateDetail> {
    const detail = await apiRequest<any>(`/api/candidates/${id}`);
    return mapCandidateDetail(detail);
  }

  async function loadCandidateTimeline(id: number) {
    return apiRequest<TimelineEvent[]>(`/api/candidates/${id}/timeline`);
  }

  async function loadCandidateFeedbacks(id: number) {
    return apiRequest<CandidateFeedback[]>(`/api/candidates/${id}/feedbacks`);
  }

  async function loadCandidateInterviews(id: number) {
    return apiRequest<InterviewPlan[]>(`/api/candidates/${id}/interviews`);
  }

  async function createCandidate(payload: CandidateUpsertPayload) {
    const detail = await apiRequest<any>("/api/candidates", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    await refreshData();
    return mapCandidateDetail(detail);
  }

  async function updateCandidate(id: number, payload: CandidateUpsertPayload) {
    const detail = await apiRequest<any>(`/api/candidates/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    await refreshData();
    return mapCandidateDetail(detail);
  }

  async function uploadResume(candidateId: number, file: File) {
    const formData = new FormData();
    formData.append("file", file);
    await apiRequest(`/api/candidates/${candidateId}/resume`, {
      method: "POST",
      body: formData,
    });
    await refreshData();
  }

  async function createParseJob(candidateId: number, hint?: string) {
    const job = await apiRequest<any>(`/api/candidates/${candidateId}/parse-jobs`, {
      method: "POST",
      body: JSON.stringify({
        hint,
      }),
    });
    return mapAgentJob(job);
  }

  async function loadLatestParseJob(candidateId: number) {
    const job = await apiRequest<any>(`/api/candidates/${candidateId}/parse-jobs/latest`);
    return mapAgentJob(job);
  }

  async function loadLatestParseJobOptional(candidateId: number) {
    const job = await apiRequestOptional<any>(`/api/candidates/${candidateId}/parse-jobs/latest`);
    return job ? mapAgentJob(job) : null;
  }

  async function createDecisionJob(candidateId: number, focusHint?: string) {
    const job = await apiRequest<any>(`/api/candidates/${candidateId}/decision-jobs`, {
      method: "POST",
      body: JSON.stringify({
        focusHint,
      }),
    });
    return mapAgentJob(job);
  }

  async function loadLatestDecisionJob(candidateId: number) {
    const job = await apiRequestOptional<any>(`/api/candidates/${candidateId}/decision-jobs/latest`);
    return job ? mapAgentJob(job) : null;
  }

  async function loadDecisionHistory(candidateId: number) {
    const jobs = await apiRequest<any[]>(`/api/candidates/${candidateId}/decision-jobs`);
    return jobs.map(mapAgentJob);
  }

  async function advanceCandidate(candidateId: number, payload: AdvanceCandidatePayload) {
    const detail = await apiRequest<any>(`/api/candidates/${candidateId}/advance`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
    await refreshData();
    return mapCandidateDetail(detail);
  }

  async function loadDepartments() {
    return apiRequest<LookupDepartment[]>("/api/lookups/departments");
  }

  async function loadUsersByRole(role: "DEPARTMENT_LEAD" | "INTERVIEWER") {
    return apiRequest<LookupUser[]>(`/api/lookups/users?role=${role}`);
  }

  async function submitDepartmentFeedback(params: {
    candidateId: number;
    passed: boolean;
    feedback: string;
    rejectReason?: string;
    nextStep?: string;
    suggestedInterviewer?: string;
  }) {
    const assignment = departmentTasks.find((task) => task.candidateId === params.candidateId);
    if (!assignment) {
      throw new Error("未找到对应的部门任务，无法提交反馈");
    }

    await apiRequest("/api/feedbacks", {
      method: "POST",
      body: JSON.stringify({
        assignmentId: assignment.id,
        decision: params.passed ? "PASS" : "REJECT",
        feedback: params.feedback,
        rejectReason: params.rejectReason,
        nextStep: params.nextStep,
        suggestedInterviewer: params.suggestedInterviewer,
      }),
    });

    await refreshData();
  }

  function getCandidateById(id: number) {
    return candidates.find((candidate) => candidate.id === id);
  }

  function getDepartmentTaskByCandidateId(candidateId: number) {
    return departmentTasks.find((task) => task.candidateId === candidateId);
  }

  async function downloadResume(candidateId: number, fileName = `candidate-${candidateId}-resume.pdf`) {
    const blob = await fetchBlob(`/api/candidates/${candidateId}/resume/download`);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  async function previewResume(candidateId: number) {
    const previewWindow = window.open("", "_blank");
    if (previewWindow) {
      previewWindow.document.write("<title>简历预览加载中</title><p style='font-family:sans-serif;padding:24px;'>正在加载简历预览...</p>");
      previewWindow.document.close();
    }

    try {
      const blob = await fetchBlob(`/api/candidates/${candidateId}/resume/preview`);
      const url = URL.createObjectURL(blob);
      if (previewWindow) {
        previewWindow.location.replace(url);
      } else {
        window.open(url, "_blank");
      }
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (requestError) {
      if (previewWindow && !previewWindow.closed) {
        previewWindow.close();
      }
      throw requestError;
    }
  }

  useEffect(() => {
    void refreshData();
  }, []);

  const value = useMemo<DataContextType>(
    () => ({
      candidates,
      departmentTasks,
      dashboardOverview,
      funnelMetrics,
      statusDistribution,
      departmentEfficiency,
      alerts,
      currentUser,
      pendingFeedbackCount: dashboardOverview?.pendingFeedbackCount ?? departmentTasks.length,
      loading,
      error,
      authReady,
      refreshData,
      loadCandidateDetail,
      loadCandidateTimeline,
      loadCandidateFeedbacks,
      loadCandidateInterviews,
      createCandidate,
      updateCandidate,
      uploadResume,
      createParseJob,
      loadLatestParseJob,
      loadLatestParseJobOptional,
      createDecisionJob,
      loadLatestDecisionJob,
      loadDecisionHistory,
      advanceCandidate,
      loadDepartments,
      loadUsersByRole,
      submitDepartmentFeedback,
      getCandidateById,
      getDepartmentTaskByCandidateId,
      downloadResume,
      previewResume,
    }),
    [
      candidates,
      departmentTasks,
      dashboardOverview,
      funnelMetrics,
      statusDistribution,
      departmentEfficiency,
      alerts,
      currentUser,
      loading,
      error,
      authReady,
    ]
  );

  return <DataContext.Provider value={value}>{children}</DataContext.Provider>;
}

export function useData() {
  const context = useContext(DataContext);
  if (context === undefined) {
    throw new Error("useData must be used within a DataProvider");
  }
  return context;
}
