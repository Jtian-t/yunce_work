import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import {
  ArrowLeft,
  Calendar,
  ChevronDown,
  ChevronRight,
  Download,
  FileSearch,
  History,
  Loader2,
  Mail,
  MapPin,
  Pencil,
  Phone,
  RefreshCw,
  Sparkles,
  User,
} from "lucide-react";
import {
  useData,
  type AgentJob,
  type CandidateDetail as CandidateDetailType,
  type CandidateFeedback,
  type DecisionReport,
  type InterviewPlan,
  type LookupDepartment,
  type LookupUser,
  type ParseReport,
  type TimelineEvent,
} from "../context/DataContext";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../components/ui/dialog";

function toLocalInputValue(date: Date) {
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatConfidence(value?: number) {
  if (typeof value !== "number") {
    return null;
  }
  return `${Math.round(value * 100)}%`;
}

const INTERVIEW_ROUNDS = ["一面", "二面", "三面", "HR面", "终面"] as const;

export function CandidateDetail() {
  const { id } = useParams();
  const {
    loadCandidateDetail,
    loadCandidateTimeline,
    loadCandidateFeedbacks,
    loadCandidateInterviews,
    downloadResume,
    previewResume,
    loadDepartments,
    loadUsersByRole,
    advanceCandidate,
    updateCandidate,
    updateInterviewPlan,
    createParseJob,
    loadLatestParseJobOptional,
    createDecisionJob,
    loadLatestDecisionJob,
    loadDecisionHistory,
    applyParsedProfile,
    saveManualParsedFields,
  } = useData();

  const [candidate, setCandidate] = useState<CandidateDetailType | null>(null);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [feedbacks, setFeedbacks] = useState<CandidateFeedback[]>([]);
  const [interviews, setInterviews] = useState<InterviewPlan[]>([]);
  const [departments, setDepartments] = useState<LookupDepartment[]>([]);
  const [departmentLeads, setDepartmentLeads] = useState<LookupUser[]>([]);
  const [interviewers, setInterviewers] = useState<LookupUser[]>([]);
  const [latestParseJob, setLatestParseJob] = useState<AgentJob | null>(null);
  const [decisionJobs, setDecisionJobs] = useState<AgentJob[]>([]);
  const [selectedDecisionJobId, setSelectedDecisionJobId] = useState<number | null>(null);
  const [departmentId, setDepartmentId] = useState<number | "">("");
  const [reviewerId, setReviewerId] = useState<number | "">("");
  const [interviewerId, setInterviewerId] = useState<number | "">("");
  const [roundLabel, setRoundLabel] = useState("一面");
  const [meetingType, setMeetingType] = useState<"ONSITE" | "TENCENT_MEETING" | "PHONE">("TENCENT_MEETING");
  const [meetingUrl, setMeetingUrl] = useState("");
  const [meetingId, setMeetingId] = useState("");
  const [meetingPassword, setMeetingPassword] = useState("");
  const [interviewNotes, setInterviewNotes] = useState("");
  const [scheduledAt, setScheduledAt] = useState(toLocalInputValue(new Date(Date.now() + 24 * 60 * 60 * 1000)));
  const [endsAt, setEndsAt] = useState(toLocalInputValue(new Date(Date.now() + 25 * 60 * 60 * 1000)));
  const [actionNote, setActionNote] = useState("");
  const [parseHint, setParseHint] = useState("");
  const [decisionHint, setDecisionHint] = useState("");
  const [decisionDialogOpen, setDecisionDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editForm, setEditForm] = useState({
    name: "",
    phone: "",
    email: "",
    location: "",
    education: "",
    experience: "",
    skillsSummary: "",
    projectSummary: "",
    departmentId: "" as number | "",
  });
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [parseLoading, setParseLoading] = useState(false);
  const [decisionLoading, setDecisionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [expandedInterviewIds, setExpandedInterviewIds] = useState<number[]>([]);
  const [expandedCompletedInterviewIds, setExpandedCompletedInterviewIds] = useState<number[]>([]);

  useEffect(() => {
    if (!id) {
      setError("缺少候选人 ID");
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    Promise.all([
      loadCandidateDetail(Number(id)),
      loadCandidateTimeline(Number(id)),
      loadCandidateFeedbacks(Number(id)),
      loadCandidateInterviews(Number(id)),
      loadDepartments(),
      loadUsersByRole("DEPARTMENT_LEAD"),
      loadUsersByRole("INTERVIEWER"),
      loadLatestParseJobOptional(Number(id)),
      loadDecisionHistory(Number(id)),
    ])
      .then(
        ([
          detail,
          timelineData,
          feedbackData,
          interviewData,
          departmentOptions,
          leadOptions,
          interviewerOptions,
          parseJob,
          decisionHistory,
        ]) => {
          if (!active) {
            return;
          }
          setCandidate(detail);
          setTimeline(timelineData);
          setFeedbacks(feedbackData);
          setInterviews(interviewData);
          setDepartments(departmentOptions);
          setDepartmentLeads(leadOptions);
          setInterviewers(interviewerOptions);
          setLatestParseJob(parseJob);
          setDecisionJobs(decisionHistory);
          setSelectedDecisionJobId((current) => current ?? decisionHistory[0]?.id ?? null);
        }
      )
      .catch((requestError) => {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : "加载候选人详情失败");
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [
    id,
    loadCandidateDetail,
    loadCandidateFeedbacks,
    loadCandidateInterviews,
    loadCandidateTimeline,
    loadDecisionHistory,
    loadDepartments,
    loadLatestParseJobOptional,
    loadUsersByRole,
    refreshKey,
  ]);

  const filteredDepartmentLeads = useMemo(() => {
    if (!departmentId) {
      return departmentLeads;
    }
    return departmentLeads.filter((item) => item.departmentId === Number(departmentId));
  }, [departmentId, departmentLeads]);

  const latestDecisionJob = decisionJobs[0] ?? null;
  const selectedDecisionJob = decisionJobs.find((job) => job.id === selectedDecisionJobId) ?? latestDecisionJob;
  const parseReport = latestParseJob?.result?.parseReport ?? null;
  const latestDecisionReport = latestDecisionJob?.result?.decisionReport ?? null;
  const selectedDecisionReport = selectedDecisionJob?.result?.decisionReport ?? null;
  const candidateDepartmentId = useMemo(() => {
    if (!candidate?.department) {
      return null;
    }
    const normalizedDepartment = candidate.department.trim().toLowerCase();
    return (
      departments.find((department) => department.name.trim().toLowerCase() === normalizedDepartment)?.id ??
      null
    );
  }, [candidate?.department, departments]);
  const visibleInterviewers = useMemo(() => {
    const currentRoundInterviewerId =
      interviews.find((item) => (item.interviewStageLabel ?? item.roundLabel) === roundLabel)?.interviewerId ??
      interviews.find((item) => item.roundLabel === roundLabel)?.interviewerId ??
      null;
    const pinnedInterviewer = currentRoundInterviewerId
      ? interviewers.find((item) => item.id === currentRoundInterviewerId) ?? null
      : null;
    if (candidateDepartmentId) {
      const scopedById = interviewers.filter((item) => item.departmentId === candidateDepartmentId);
      if (scopedById.length > 0) {
        return pinnedInterviewer && !scopedById.some((item) => item.id === pinnedInterviewer.id)
          ? [...scopedById, pinnedInterviewer]
          : scopedById;
      }
    }
    if (candidate?.department) {
      const normalizedDepartment = candidate.department.trim().toLowerCase();
      const scopedByName = interviewers.filter(
        (item) => item.departmentName?.trim().toLowerCase() === normalizedDepartment
      );
      if (scopedByName.length > 0) {
        return pinnedInterviewer && !scopedByName.some((item) => item.id === pinnedInterviewer.id)
          ? [...scopedByName, pinnedInterviewer]
          : scopedByName;
      }
    }
    return pinnedInterviewer && !interviewers.some((item) => item.id === pinnedInterviewer.id)
      ? [...interviewers, pinnedInterviewer]
      : interviewers;
  }, [candidate?.department, candidateDepartmentId, interviewers, interviews, roundLabel]);
  const selectedInterviewer = useMemo(
    () => visibleInterviewers.find((item) => item.id === Number(interviewerId)) ?? null,
    [interviewerId, visibleInterviewers]
  );
  const latestSuggestedInterviewer = useMemo(
    () =>
      feedbacks.find(
        (item) =>
          item.decision === "PASS" &&
          (item.suggestedInterviewerId != null || (item.suggestedInterviewerName ?? item.suggestedInterviewer))
      ) ?? null,
    [feedbacks]
  );
  const sortedInterviews = useMemo(
    () =>
      [...interviews].sort(
        (left, right) => new Date(right.scheduledAt).getTime() - new Date(left.scheduledAt).getTime()
      ),
    [interviews]
  );
  const completedInterviews = useMemo(
    () => sortedInterviews.filter((item) => item.evaluationSubmitted),
    [sortedInterviews]
  );
  const latestCompletedInterview = completedInterviews[0] ?? null;
  const latestInterview = sortedInterviews[0] ?? null;
  const defaultRoundLabel = useMemo(() => {
    if (!latestCompletedInterview && !latestInterview) {
      return INTERVIEW_ROUNDS[0];
    }
    if (latestCompletedInterview) {
      const latestRound = latestCompletedInterview.interviewStageLabel ?? latestCompletedInterview.roundLabel;
      const currentIndex = INTERVIEW_ROUNDS.indexOf(latestRound as (typeof INTERVIEW_ROUNDS)[number]);
      if (currentIndex >= 0 && currentIndex < INTERVIEW_ROUNDS.length - 1) {
        return INTERVIEW_ROUNDS[currentIndex + 1];
      }
      return latestRound;
    }

    if (latestInterview && !latestInterview.evaluationSubmitted) {
      return latestInterview.interviewStageLabel ?? latestInterview.roundLabel;
    }

    return INTERVIEW_ROUNDS[0];
  }, [latestCompletedInterview, latestInterview]);
  const currentRoundInterview = useMemo(
    () =>
      interviews.find((item) => (item.interviewStageLabel ?? item.roundLabel) === roundLabel) ??
      interviews.find((item) => item.roundLabel === roundLabel) ??
      null,
    [interviews, roundLabel]
  );
  const canEditCurrentRoundInterview = currentRoundInterview != null && !currentRoundInterview.evaluationSubmitted;

  useEffect(() => {
    if (candidateDepartmentId == null) {
      return;
    }
    let active = true;
    loadUsersByRole("INTERVIEWER", candidateDepartmentId)
      .then((items) => {
        if (active && items.length > 0) {
          setInterviewers(items);
        }
      })
      .catch(() => {
        // Keep the default list if the department-scoped lookup fails.
      });
    return () => {
      active = false;
    };
  }, [candidateDepartmentId, loadUsersByRole]);

  useEffect(() => {
    if (!latestSuggestedInterviewer || interviewerId) {
      return;
    }
    if (latestSuggestedInterviewer.suggestedInterviewerId != null) {
      const matched = visibleInterviewers.find((item) => item.id === latestSuggestedInterviewer.suggestedInterviewerId);
      if (matched) {
        setInterviewerId(matched.id);
      }
    }
  }, [interviewerId, latestSuggestedInterviewer, visibleInterviewers]);

  useEffect(() => {
    setRoundLabel((current) => {
      if (!current) {
        return defaultRoundLabel;
      }
      if (current === defaultRoundLabel) {
        return current;
      }
      if (currentRoundInterview && !currentRoundInterview.evaluationSubmitted) {
        return current;
      }
      return defaultRoundLabel;
    });
  }, [currentRoundInterview, defaultRoundLabel]);

  useEffect(() => {
    if (!latestInterview) {
      setExpandedInterviewIds([]);
      return;
    }
    setExpandedInterviewIds((current) => (current.length > 0 ? current : [latestInterview.id]));
  }, [latestInterview]);

  useEffect(() => {
    if (!latestCompletedInterview) {
      setExpandedCompletedInterviewIds([]);
      return;
    }
    setExpandedCompletedInterviewIds((current) => (current.length > 0 ? current : [latestCompletedInterview.id]));
  }, [latestCompletedInterview]);

  useEffect(() => {
    if (currentRoundInterview) {
      setInterviewerId(currentRoundInterview.interviewerId ?? "");
      setScheduledAt(toLocalInputValue(new Date(currentRoundInterview.scheduledAt)));
      setEndsAt(toLocalInputValue(new Date(currentRoundInterview.endsAt)));
      setMeetingType(
        (currentRoundInterview.meetingType as "ONSITE" | "TENCENT_MEETING" | "PHONE" | null) ?? "TENCENT_MEETING"
      );
      setMeetingUrl(currentRoundInterview.meetingUrl ?? "");
      setMeetingId(currentRoundInterview.meetingId ?? "");
      setMeetingPassword(currentRoundInterview.meetingPassword ?? "");
      setInterviewNotes(currentRoundInterview.notes ?? "");
      return;
    }

    setInterviewerId((current) => {
      if (current && visibleInterviewers.some((item) => item.id === Number(current))) {
        return current;
      }
      return "";
    });
    setScheduledAt(toLocalInputValue(new Date(Date.now() + 24 * 60 * 60 * 1000)));
    setEndsAt(toLocalInputValue(new Date(Date.now() + 25 * 60 * 60 * 1000)));
    setMeetingType("TENCENT_MEETING");
    setMeetingUrl("");
    setMeetingId("");
    setMeetingPassword("");
    setInterviewNotes("");
  }, [currentRoundInterview, roundLabel, visibleInterviewers]);

  const skillList = parseReport?.extractedSkills?.length
    ? parseReport.extractedSkills
    : candidate?.skillsSummary
    ? candidate.skillsSummary.split(/[,，]/).map((item) => item.trim()).filter(Boolean)
    : [];
  const projectList = parseReport?.projectExperiences?.length
    ? parseReport.projectExperiences.map((project) => project.summary)
    : candidate?.projectSummary
    ? candidate.projectSummary.split(/\n|；|;/).map((item) => item.trim()).filter(Boolean)
    : [];

  async function runAction(payload: Parameters<typeof advanceCandidate>[1]) {
    if (!candidate) {
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      await advanceCandidate(candidate.id, payload);
      setActionNote("");
      setRefreshKey((value) => value + 1);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "推进候选人失败");
    } finally {
      setActionLoading(false);
    }
  }

  function resolveInterviewStageCode(label: string) {
    const normalized = label.replace(/\s+/g, "");
    if (normalized.includes("终")) {
      return "FINAL";
    }
    if (normalized.toUpperCase().includes("HR")) {
      return "HR";
    }
    if (normalized.includes("三")) {
      return "ROUND_3";
    }
    if (normalized.includes("二")) {
      return "ROUND_2";
    }
    return "ROUND_1";
  }

  async function handleSaveInterview() {
    if (!candidate || !interviewerId) {
      return;
    }

    setActionLoading(true);
    setError(null);
    try {
      const interviewDepartmentId = selectedInterviewer?.departmentId ?? candidateDepartmentId ?? undefined;
      const payload = {
        interviewerId: Number(interviewerId),
        roundLabel,
        scheduledAt: new Date(scheduledAt).toISOString(),
        endsAt: new Date(endsAt).toISOString(),
        meetingType,
        meetingUrl,
        meetingId,
        meetingPassword,
        interviewStageCode: resolveInterviewStageCode(roundLabel),
        interviewStageLabel: roundLabel,
        departmentId: interviewDepartmentId,
        notes: interviewNotes,
      };

      if (canEditCurrentRoundInterview && currentRoundInterview) {
        await updateInterviewPlan(currentRoundInterview.id, payload);
      } else {
        await advanceCandidate(candidate.id, {
          action: "SCHEDULE_INTERVIEW",
          interviewerId: Number(interviewerId),
          roundLabel,
          scheduledAt: payload.scheduledAt,
          endsAt: payload.endsAt,
          meetingType,
          meetingUrl,
          meetingId,
          meetingPassword,
          interviewStageCode: payload.interviewStageCode,
          interviewStageLabel: roundLabel,
          interviewDepartmentId,
          interviewNotes,
        });
      }

      setRefreshKey((value) => value + 1);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "保存面试安排失败");
    } finally {
      setActionLoading(false);
    }
  }

  async function waitForDecision(candidateId: number) {
    for (let attempt = 0; attempt < 8; attempt += 1) {
      const latest = await loadLatestDecisionJob(candidateId);
      if (latest?.status === "SUCCEEDED") {
        return latest;
      }
      if (latest?.status === "FAILED") {
        throw new Error(latest.lastError || "辅助决策生成失败");
      }
      await new Promise((resolve) => window.setTimeout(resolve, 400));
    }
    const latest = await loadLatestDecisionJob(candidateId);
    if (!latest) {
      throw new Error("未获取到辅助决策结果");
    }
    return latest;
  }

  async function waitForParse(candidateId: number) {
    for (let attempt = 0; attempt < 8; attempt += 1) {
      const latest = await loadLatestParseJobOptional(candidateId);
      if (latest?.status === "SUCCEEDED") {
        return latest;
      }
      if (latest?.status === "FAILED") {
        throw new Error(latest.lastError || "简历解析失败");
      }
      await new Promise((resolve) => window.setTimeout(resolve, 400));
    }
    const latest = await loadLatestParseJobOptional(candidateId);
    if (!latest) {
      throw new Error("未获取到解析结果");
    }
    return latest;
  }

  async function handleDecisionAnalysis() {
    if (!candidate) {
      return;
    }
    setDecisionDialogOpen(true);
    setDecisionLoading(true);
    setError(null);
    try {
      await createDecisionJob(candidate.id, decisionHint || undefined);
      const latest = await waitForDecision(candidate.id);
      const history = await loadDecisionHistory(candidate.id);
      setDecisionJobs(history);
      setSelectedDecisionJobId(latest.id);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "辅助决策生成失败");
    } finally {
      setDecisionLoading(false);
    }
  }

  async function handleReparseResume() {
    if (!candidate) {
      return;
    }
    if (!candidate.latestResume) {
      setError("当前候选人还没有上传简历，无法重新解析");
      return;
    }
    setParseLoading(true);
    setError(null);
    try {
      await createParseJob(candidate.id, parseHint || undefined);
      const latest = await waitForParse(candidate.id);
      setLatestParseJob(latest);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "简历重新解析失败");
    } finally {
      setParseLoading(false);
    }
  }

  async function handlePreviewResume() {
    if (!candidate) {
      return;
    }
    try {
      await previewResume(candidate.id);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "简历预览失败");
    }
  }

  async function handleApplyParsedFields() {
    if (!candidate) {
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      await applyParsedProfile(candidate.id);
      setRefreshKey((value) => value + 1);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "解析结果回显失败");
    } finally {
      setActionLoading(false);
    }
  }

  function openEditDialog() {
    if (!candidate) {
      return;
    }
    setEditForm({
      name: candidate.name ?? "",
      phone: candidate.phone ?? "",
      email: candidate.email ?? "",
      location: candidate.location ?? "",
      education: candidate.education ?? "",
      experience: candidate.experience ?? "",
      skillsSummary: candidate.skillsSummary ?? "",
      projectSummary: candidate.projectSummary ?? "",
      departmentId: candidateDepartmentId ?? "",
    });
    setEditDialogOpen(true);
  }

  function fillFromParse() {
    if (!parseReport) {
      return;
    }
    setEditForm((prev) => ({
      ...prev,
      name: parseReport.fields.name?.value ?? prev.name,
      phone: parseReport.fields.phone?.value ?? prev.phone,
      email: parseReport.fields.email?.value ?? prev.email,
      location: parseReport.fields.location?.value ?? prev.location,
      education: parseReport.fields.education?.value ?? prev.education,
      experience: parseReport.fields.experience?.value ?? prev.experience,
      skillsSummary: parseReport.fields.skillsSummary?.value ?? prev.skillsSummary,
      projectSummary: parseReport.fields.projectSummary?.value ?? prev.projectSummary,
    }));
  }

  async function saveManualFields() {
    if (!candidate) {
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      await saveManualParsedFields(candidate.id, {
        name: editForm.name,
        phone: editForm.phone,
        email: editForm.email,
        location: editForm.location,
        education: editForm.education,
        experience: editForm.experience,
        skillsSummary: editForm.skillsSummary,
        projectSummary: editForm.projectSummary,
        lockReason: "HR 鎵嬪伐纭",
      });
      await updateCandidate(candidate.id, {
        name: editForm.name.trim() || candidate.name,
        position: candidate.position,
        departmentId: editForm.departmentId === "" ? null : Number(editForm.departmentId),
        source: candidate.source,
        submittedDate: candidate.submittedDate,
        nextAction: candidate.nextAction,
        phone: editForm.phone.trim(),
        email: editForm.email.trim(),
        location: editForm.location.trim(),
        education: editForm.education.trim(),
        experience: editForm.experience.trim(),
        skillsSummary: editForm.skillsSummary.trim(),
        projectSummary: editForm.projectSummary.trim(),
      });
      setEditDialogOpen(false);
      setRefreshKey((value) => value + 1);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "保存简历信息失败");
    } finally {
      setActionLoading(false);
    }
  }

  function toggleInterviewExpanded(interviewId: number) {
    setExpandedInterviewIds((current) =>
      current.includes(interviewId)
        ? current.filter((item) => item !== interviewId)
        : [...current, interviewId]
    );
  }

  function toggleCompletedInterviewExpanded(interviewId: number) {
    setExpandedCompletedInterviewIds((current) =>
      current.includes(interviewId)
        ? current.filter((item) => item !== interviewId)
        : [...current, interviewId]
    );
  }

  if (loading) {
    return <div className="p-6 text-gray-600">正在加载候选人详情...</div>;
  }

  if (!candidate) {
    return (
      <div className="p-6">
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error ?? "未找到候选人"}
        </div>
      </div>
    );
  }

  const canScheduleInterview = ["PENDING_INTERVIEW", "INTERVIEWING", "INTERVIEW_PASSED"].includes(candidate.statusCode);
  const hasWorkflowAction =
    ["NEW", "TIMEOUT", "IN_DEPT_REVIEW"].includes(candidate.statusCode) ||
    canScheduleInterview ||
    candidate.statusCode === "INTERVIEW_PASSED" ||
    candidate.statusCode === "OFFER_PENDING" ||
    candidate.statusCode === "OFFER_SENT" ||
    !["HIRED", "REJECTED"].includes(candidate.statusCode);

  return (
    <div className="p-6 space-y-6">
      <Link to="/candidates" className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900">
        <ArrowLeft className="h-5 w-5" />
        返回候选人列表
      </Link>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[0.95fr,1.05fr]">
        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="mb-6 flex flex-col items-center text-center">
              <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-blue-100 text-2xl font-semibold text-blue-700">
                {candidate.name[0]}
              </div>
              <h2 className="text-xl font-bold text-gray-900">{candidate.name}</h2>
              <p className="mt-1 text-gray-600">{candidate.position}</p>
              <span className="mt-3 rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-700">{candidate.status}</span>
              <button
                type="button"
                onClick={openEditDialog}
                className="mt-3 inline-flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                <Pencil className="h-4 w-4" />
                编辑简历信息
              </button>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-3 text-gray-700">
                <Phone className="h-4 w-4 text-gray-400" />
                <span>{candidate.phone ?? "-"}</span>
              </div>
              <div className="flex items-center gap-3 text-gray-700">
                <Mail className="h-4 w-4 text-gray-400" />
                <span>{candidate.email ?? "-"}</span>
              </div>
              <div className="flex items-center gap-3 text-gray-700">
                <MapPin className="h-4 w-4 text-gray-400" />
                <span>{candidate.location ?? "-"}</span>
              </div>
              <div className="flex items-center gap-3 text-gray-700">
                <Calendar className="h-4 w-4 text-gray-400" />
                <span>推荐日期：{candidate.submittedDate}</span>
              </div>
            </div>

            <div className="mt-6 border-t border-gray-200 pt-4">
              <div className="mb-3 text-sm font-medium text-gray-700">简历附件</div>
              {candidate.latestResume ? (
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => void handlePreviewResume()}
                    className="inline-flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-100"
                  >
                    <FileSearch className="h-4 w-4" />
                    查看简历
                  </button>
                  <button
                    type="button"
                    onClick={() => void downloadResume(candidate.id, candidate.latestResume?.originalFileName ?? `${candidate.name}-resume.pdf`)}
                    className="inline-flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                  >
                    <Download className="h-4 w-4" />
                    下载简历
                  </button>
                  <button
                    type="button"
                    onClick={() => setDecisionDialogOpen(true)}
                    className="inline-flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700"
                  >
                    <Sparkles className="h-4 w-4" />
                    辅助决策
                  </button>
                </div>
              ) : (
                <div className="text-sm text-gray-500">暂无简历附件</div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">简历解析洞察</h3>
                <p className="mt-1 text-sm text-gray-500">支持重新解析并保留最新结构化结果，供后续流程与辅助决策使用。</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => void handleApplyParsedFields()}
                  disabled={actionLoading || !parseReport}
                  className="inline-flex items-center gap-2 rounded-lg border border-emerald-300 px-4 py-2 text-sm font-medium text-emerald-700 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  一键回填到候选人
                </button>
                <button
                  type="button"
                  onClick={() => void handleReparseResume()}
                  disabled={parseLoading || !candidate.latestResume}
                  className="inline-flex items-center gap-2 rounded-lg border border-blue-300 px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {parseLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                  重新解析
                </button>
              </div>
            </div>

            <label className="mt-4 block text-sm text-gray-700">
              解析关注点(可选)
              <input
                value={parseHint}
                onChange={(event) => setParseHint(event.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                placeholder="例如：优先识别 Java 后端年限、项目经验和城市"
              />
            </label>

            {parseReport ? (
              <div className="mt-4 space-y-4">
                <div className="rounded-lg border border-green-200 bg-green-50 p-4">
                  <div className="text-sm font-medium text-green-900">最新解析摘要</div>
                  <div className="mt-1 text-sm text-green-800">{parseReport.summary}</div>
                  <div className="mt-2 text-xs text-green-700">
                    模式：{parseReport.extractionMode ?? "UNKNOWN"}
                    {parseReport.ocrRequired ? " · 需要 OCR" : ""}
                  </div>
                </div>

                {parseReport.highlights.length > 0 && (
                  <div>
                      <div className="mb-2 text-sm font-medium text-gray-700">解析亮点</div>
                    <div className="flex flex-wrap gap-2">
                      {parseReport.highlights.map((highlight) => (
                        <span key={highlight} className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                          {highlight}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <div className="grid gap-3 md:grid-cols-2">
                  {Object.entries(parseReport.fields).map(([fieldKey, fieldValue]) => (
                    <div key={fieldKey} className="rounded-lg border border-gray-200 p-3">
                      <div className="text-xs font-medium uppercase tracking-wide text-gray-500">{fieldKey}</div>
                      <div className="mt-1 text-sm font-medium text-gray-900">{fieldValue.value}</div>
                      <div className="mt-1 text-xs text-gray-500">
                        置信度：{formatConfidence(fieldValue.confidence) ?? "-"} · 来源 {fieldValue.source}
                      </div>
                    </div>
                  ))}
                </div>

                {parseReport.issues.length > 0 && (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
                    <div className="text-sm font-medium text-amber-900">待确认项</div>
                    <ul className="mt-2 space-y-1 text-sm text-amber-800">
                      {parseReport.issues.map((issue) => (
                        <li key={`${issue.severity}-${issue.message}`}>- {issue.message}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ) : (
              <div className="mt-4 rounded-lg border border-gray-200 bg-gray-50 p-4 text-sm text-gray-600">
                无结构化解析结果。上传简历后可在这里查看摘要、学历信息与待确认项。              </div>
            )}
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">最近一次辅助决策</h3>
            {latestDecisionReport ? (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap items-center gap-3">
                  <span className="rounded-full bg-violet-50 px-3 py-1 text-sm font-medium text-violet-700">
                    {latestDecisionReport.recommendationLevel}
                  </span>
                  <span className="rounded-full bg-gray-100 px-3 py-1 text-sm font-medium text-gray-700">
                    综合评分 {latestDecisionReport.recommendationScore}
                  </span>
                </div>
                <div className="text-sm text-gray-700">{latestDecisionReport.conclusion}</div>
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-4">
                  <div className="text-sm font-medium text-gray-900">建议动作</div>
                  <div className="mt-1 text-sm text-gray-600">{latestDecisionReport.recommendedAction}</div>
                </div>
                {latestDecisionReport.strengths.length > 0 && (
                  <div>
                    <div className="mb-2 text-sm font-medium text-gray-700">关键优势</div>
                    <ul className="space-y-1 text-sm text-gray-700">
                      {latestDecisionReport.strengths.map((item) => (
                        <li key={item}>- {item}</li>
                      ))}
                    </ul>
                  </div>
                )}
                <button
                  type="button"
                  onClick={() => setDecisionDialogOpen(true)}
                  className="inline-flex items-center gap-2 rounded-lg border border-violet-300 px-4 py-2 text-sm font-medium text-violet-700 hover:bg-violet-50"
                >
                  <History className="h-4 w-4" />
                  查看完整结果与历史
                </button>
              </div>
            ) : (
              <div className="mt-4 rounded-lg border border-dashed border-violet-200 bg-violet-50 p-4 text-sm text-violet-700">
                暂无辅助决策结果。点击“辅助决策”即可综合简历、面试反馈和流程状态生成建议。
              </div>
            )}
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">技能与项目</h3>
            <div className="mt-4">
              <div className="mb-2 text-sm font-medium text-gray-700">技能标签</div>
              <div className="flex flex-wrap gap-2">
                {skillList.length > 0 ? (
                  skillList.map((skill) => (
                    <span key={skill} className="rounded-lg bg-blue-50 px-3 py-1.5 text-sm font-medium text-blue-700">
                      {skill}
                    </span>
                  ))
                ) : (
                  <span className="text-sm text-gray-500">暂无技能摘要</span>
                )}
              </div>
            </div>
            <div className="mt-5">
              <div className="mb-2 text-sm font-medium text-gray-700">项目经历</div>
              <div className="space-y-2 text-sm text-gray-700">
                {projectList.length > 0 ? projectList.map((project) => <div key={project}>• {project}</div>) : <div className="text-gray-500">暂无项目摘要</div>}
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">HR 推进一步</h3>
            <p className="mt-1 text-sm text-gray-500">根据当前状态展示合法动作，所有流程都会写入时间线。</p>

            <div className="mt-5 space-y-5">
              {(candidate.statusCode === "NEW" || candidate.statusCode === "TIMEOUT" || candidate.statusCode === "IN_DEPT_REVIEW") && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">分发或退回简历池</div>
                  {latestSuggestedInterviewer && (
                    <div className="mb-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
                      部门建议面试官：
                      {latestSuggestedInterviewer.suggestedInterviewerName ??
                        latestSuggestedInterviewer.suggestedInterviewer ??
                        "未指定"}
                      。最终通知会发送给当前选中的面试官。
                    </div>
                  )}
                  <div className="grid gap-3 md:grid-cols-2">
                    <select
                      value={departmentId}
                      onChange={(event) => setDepartmentId(event.target.value ? Number(event.target.value) : "")}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    >
                      <option value="">选择部门</option>
                      {departments.map((department) => (
                        <option key={department.id} value={department.id}>
                          {department.name}
                        </option>
                      ))}
                    </select>
                    {visibleInterviewers.length === 0 && (
                      <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                        当前候选人所属部门下暂无可选面试官，请先检查该部门人员配置。
                      </div>
                    )}
                    <select
                      value={reviewerId}
                      onChange={(event) => setReviewerId(event.target.value ? Number(event.target.value) : "")}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    >
                      <option value="">选择负责人</option>
                      {filteredDepartmentLeads.map((reviewer) => (
                        <option key={reviewer.id} value={reviewer.id}>
                          {reviewer.displayName}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-3">
                    <button
                      type="button"
                      disabled={actionLoading || !departmentId || !reviewerId}
                      onClick={() =>
                        void runAction({
                          action: "ASSIGN_TO_DEPARTMENT",
                          departmentId: Number(departmentId),
                          reviewerId: Number(reviewerId),
                        })
                      }
                      className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      分发到部门
                    </button>
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MOVE_TO_POOL", note: actionNote })}
                      className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      退回简历池
                    </button>
                    {(candidate.statusCode === "IN_DEPT_REVIEW" || candidate.statusCode === "TIMEOUT") && (
                      <button
                        type="button"
                        disabled={actionLoading}
                        onClick={() => void runAction({ action: "REMIND_REVIEWER" })}
                        className="rounded-lg border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-medium text-amber-700 hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        催办负责人
                      </button>
                    )}
                  </div>
                </div>
              )}

              {canScheduleInterview && (
                <div className="rounded-xl border border-gray-200 p-4">
                  {latestCompletedInterview && (
                    <div className="mb-3 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">
                      {latestCompletedInterview.interviewer} ·{" "}
                      {new Date(latestCompletedInterview.scheduledAt).toLocaleString("zh-CN")}
                    </div>
                  )}
                  {completedInterviews.length > 0 && (
                    <div className="mb-4 space-y-2">
                      {completedInterviews.map((interview) => {
                        const expanded = expandedCompletedInterviewIds.includes(interview.id);
                        const label = interview.interviewStageLabel ?? interview.roundLabel;
                        const evaluation = interview.evaluations[0];
                        const evaluationTime = evaluation?.createdAt ?? null;
                        return (
                          <div key={`completed-${interview.id}`} className="rounded-lg border border-gray-300 bg-gray-50">
                            <button
                              type="button"
                              onClick={() => toggleCompletedInterviewExpanded(interview.id)}
                              className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-100"
                            >
                              <div className="font-medium text-gray-900">
                                {label} - 已完成
                                {evaluationTime ? `（评估时间：${new Date(evaluationTime).toLocaleString("zh-CN")}）` : ""}
                              </div>
                              {expanded ? (
                                <ChevronDown className="h-4 w-4 text-gray-500" />
                              ) : (
                                <ChevronRight className="h-4 w-4 text-gray-500" />
                              )}
                            </button>
                            {expanded && (
                              <div className="border-t border-gray-200 bg-white px-4 py-3 text-sm text-gray-700">
                                <div>面试官：{interview.interviewer}</div>
                                <div className="mt-1">
                                  时间：{new Date(interview.scheduledAt).toLocaleString("zh-CN")} - {new Date(interview.endsAt).toLocaleString("zh-CN")}
                                </div>
                                {evaluation ? (
                                  <div className="mt-3 rounded-lg border border-gray-200 bg-gray-50 p-3">
                                    <div className="font-medium text-gray-900">
                                      评分 {evaluation.score} · {evaluation.result}
                                    </div>
                                    <div className="mt-1">{evaluation.evaluation}</div>
                                    {evaluation.strengths && <div className="mt-1">优点：{evaluation.strengths}</div>}
                                    {evaluation.weaknesses && <div className="mt-1">风险点：{evaluation.weaknesses}</div>}
                                    {evaluation.suggestion && <div className="mt-1 text-blue-700">建议：{evaluation.suggestion}</div>}
                                  </div>
                                ) : (
                                  <div className="mt-3 text-gray-500">当前还没有面试评价</div>
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                  <div className="mb-3 font-medium text-gray-900">
                    {canEditCurrentRoundInterview ? "编辑面试安排" : candidate.statusCode === "INTERVIEW_PASSED" ? "安排" + roundLabel : "安排面试"}
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <select
                      value={interviewerId}
                      onChange={(event) => setInterviewerId(event.target.value ? Number(event.target.value) : "")}
                      disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                    >
                      <option value="">选择面试官</option>
                      {visibleInterviewers.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.displayName}
                          {item.departmentName ? " | " + item.departmentName : ""}
                        </option>
                      ))}
                    </select>
                    <select
                      value={roundLabel}
                      onChange={(event) => setRoundLabel(event.target.value)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5"
                    >
                      <option value="一面">一面</option>
                      <option value="二面">二面</option>
                      <option value="三面">三面</option>
                      <option value="HR面">HR 面</option>
                      <option value="终面">终面</option>
                    </select>
                    <input
                      type="datetime-local"
                      value={scheduledAt}
                      onChange={(event) => setScheduledAt(event.target.value)}
                      disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                    />
                    <input
                      type="datetime-local"
                      value={endsAt}
                      onChange={(event) => setEndsAt(event.target.value)}
                      disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                    />
                    <select
                      value={meetingType}
                      onChange={(event) => setMeetingType(event.target.value as "ONSITE" | "TENCENT_MEETING" | "PHONE")}
                      disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                    >
                      <option value="TENCENT_MEETING">腾讯会议</option>
                      <option value="ONSITE">现场面试</option>
                      <option value="PHONE">电话面试</option>
                    </select>
                    <input
                      value={meetingUrl}
                      onChange={(event) => setMeetingUrl(event.target.value)}
                      disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                      placeholder="会议链接"
                    />
                    <input
                      value={meetingId}
                      onChange={(event) => setMeetingId(event.target.value)}
                      disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                      placeholder="会议号"
                    />
                    <input
                      value={meetingPassword}
                      onChange={(event) => setMeetingPassword(event.target.value)}
                      disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                      className="rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                      placeholder="会议密码"
                    />
                  </div>
                  <textarea
                    value={interviewNotes}
                    onChange={(event) => setInterviewNotes(event.target.value)}
                    disabled={Boolean(currentRoundInterview?.evaluationSubmitted)}
                    className="mt-3 min-h-20 w-full rounded-lg border border-gray-300 px-3 py-2.5 disabled:bg-gray-100"
                    placeholder="面试备注"
                  />
                  <button
                    type="button"
                    disabled={actionLoading || !interviewerId || Boolean(currentRoundInterview?.evaluationSubmitted)}
                    onClick={() => void handleSaveInterview()}
                    className="mt-3 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {canEditCurrentRoundInterview
                      ? "编辑面试安排"
                      : candidate.statusCode === "INTERVIEW_PASSED"
                      ? `安排${roundLabel}`
                      : "安排面试"}
                  </button>
                </div>
              )}

              {candidate.statusCode === "INTERVIEW_PASSED" && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">Offer 阶段</div>
                  <button
                    type="button"
                    disabled={actionLoading}
                    onClick={() => void runAction({ action: "ADVANCE_TO_OFFER_PENDING" })}
                    className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    推进到待发 Offer
                  </button>
                </div>
              )}

              {candidate.statusCode === "OFFER_PENDING" && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">发放 Offer</div>
                  <button
                    type="button"
                    disabled={actionLoading}
                    onClick={() => void runAction({ action: "MARK_OFFER_SENT" })}
                    className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    标记已发 Offer
                  </button>
                </div>
              )}

              {candidate.statusCode === "OFFER_SENT" && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-3 font-medium text-gray-900">Offer 结果</div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MARK_HIRED" })}
                      className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      标记录用
                    </button>
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MARK_REJECTED", note: actionNote })}
                      className="rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      标记淘汰
                    </button>
                  </div>
                </div>
              )}

              {!["HIRED", "REJECTED"].includes(candidate.statusCode) && (
                <div className="rounded-xl border border-gray-200 p-4">
                  <div className="mb-2 font-medium text-gray-900">人工淘汰备注</div>
                  <textarea
                    value={actionNote}
                    onChange={(event) => setActionNote(event.target.value)}
                    className="min-h-24 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                    placeholder="填写淘汰原因、退回原因或其他说明"
                  />
                  {candidate.statusCode !== "OFFER_SENT" && (
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => void runAction({ action: "MARK_REJECTED", note: actionNote })}
                      className="mt-3 rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      人工淘汰
                    </button>
                  )}
                </div>
              )}

              {!hasWorkflowAction && (
                <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-sm text-gray-600">
                  当前候选人流程已结束，暂无可继续推进的 HR 操作。
                </div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">流程时间线</h3>
            <div className="mt-5 space-y-5">
              {timeline.length > 0 ? (
                timeline.map((item) => (
                  <div key={item.id} className="flex gap-4">
                    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-700">
                      {item.statusLabel[0]}
                    </div>
                    <div className="flex-1">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <div className="font-semibold text-gray-900">{item.sourceAction}</div>
                          <div className="text-sm text-gray-500">操作人：{item.actorName}</div>
                        </div>
                        <div className="text-sm text-gray-500">{new Date(item.occurredAt).toLocaleString("zh-CN")}</div>
                      </div>
                      <div className="mt-2 text-sm text-gray-700">{item.note || item.statusLabel}</div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-sm text-gray-500">暂无流程事件</div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">部门反馈记录</h3>
            <div className="mt-4 space-y-4">
              {feedbacks.length > 0 ? (
                feedbacks.map((feedback) => (
                  <div key={feedback.id} className="rounded-lg border border-gray-200 p-4">
                    <div className="mb-3 flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100">
                          <User className="h-5 w-5 text-blue-600" />
                        </div>
                        <div>
                          <div className="font-medium text-gray-900">{feedback.reviewer}</div>
                          <div className="text-sm text-gray-500">{new Date(feedback.createdAt).toLocaleString("zh-CN")}</div>
                        </div>
                      </div>
                      <span
                        className={`rounded-full px-3 py-1 text-xs font-medium ${
                          feedback.decision === "PASS" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                        }`}
                      >
                        {feedback.decision === "PASS" ? "通过" : "不通过"}
                      </span>
                    </div>
                    <p className="text-sm text-gray-700">{feedback.feedback}</p>
                    {(feedback.suggestedInterviewerName ?? feedback.suggestedInterviewer) && (
                      <div className="mt-2 text-sm text-amber-700">
                        建议面试官：{feedback.suggestedInterviewerName ?? feedback.suggestedInterviewer}
                      </div>
                    )}
                    {feedback.nextStep && <div className="mt-2 text-sm font-medium text-blue-600">下一步：{feedback.nextStep}</div>}
                  </div>
                ))
              ) : (
                <div className="text-sm text-gray-500">暂无部门反馈</div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">面试进展</h3>
            <div className="mt-4 space-y-3">
              {sortedInterviews.length > 0 ? (
                sortedInterviews.map((interview) => {
                  const expanded = expandedInterviewIds.includes(interview.id);
                  const label = interview.interviewStageLabel ?? interview.roundLabel;
                  const interviewStatusText = interview.evaluationSubmitted ? "已评价" : "待评价";
                  return (
                    <div key={`progress-${interview.id}`} className="rounded-lg border border-gray-200">
                      <button
                        type="button"
                        onClick={() => toggleInterviewExpanded(interview.id)}
                        className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left hover:bg-gray-50"
                      >
                        <div>
                          <div className="font-medium text-gray-900">{label}</div>
                          <div className="mt-1 text-sm text-gray-600">
                            {expanded
                              ? `${interview.status} 路 ${new Date(interview.scheduledAt).toLocaleString("zh-CN")}`
                              : `${label} · ${interviewStatusText}`}
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-xs text-gray-500">{interviewStatusText}</span>
                          {expanded ? (
                            <ChevronDown className="h-4 w-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="h-4 w-4 text-gray-400" />
                          )}
                        </div>
                      </button>
                      {expanded && (
                        <div className="border-t border-gray-100 px-4 py-3 text-sm text-gray-700">
                          <div>面试官：{interview.interviewer}</div>
                          <div className="mt-1">
                            时间：{new Date(interview.scheduledAt).toLocaleString("zh-CN")} -{" "}
                            {new Date(interview.endsAt).toLocaleString("zh-CN")}
                          </div>
                          <div className="mt-1">状态：{interview.status}</div>
                          {interview.notes && <div className="mt-1">备注：{interview.notes}</div>}
                        </div>
                      )}
                    </div>
                  );
                })
              ) : (
                <div className="text-sm text-gray-500">暂无面试进展</div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900">面试记录</h3>
            <div className="mt-4 space-y-4">
              {sortedInterviews.length > 0 ? (
                sortedInterviews.map((interview) => {
                  const expanded = expandedInterviewIds.includes(interview.id);
                  const label = interview.interviewStageLabel ?? interview.roundLabel;
                  const interviewStatusText = interview.evaluationSubmitted ? "点击展开查看详情" : "待面试评价";
                  return (
                    <div key={interview.id} className="rounded-lg border border-gray-200">
                      <button
                        type="button"
                        onClick={() => toggleInterviewExpanded(interview.id)}
                        className="flex w-full items-start justify-between gap-3 px-4 py-4 text-left hover:bg-gray-50"
                      >
                        <div>
                          <div className="font-semibold text-gray-900">{label}</div>
                          <div className="mt-1 text-sm text-gray-600">
                            {expanded
                              ? `面试官：${interview.interviewer} · ${new Date(interview.scheduledAt).toLocaleString("zh-CN")}`
                              : `${label} · ${interviewStatusText}`}
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                            {interview.status}
                          </span>
                          {expanded ? (
                            <ChevronDown className="h-4 w-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="h-4 w-4 text-gray-400" />
                          )}
                        </div>
                      </button>
                      {expanded && (
                        <div className="border-t border-gray-100 px-4 py-4">
                          <div className="text-sm text-gray-600">
                            面试官：{interview.interviewer} · {new Date(interview.scheduledAt).toLocaleString("zh-CN")}
                          </div>
                          {interview.meetingUrl && (
                            <a
                              href={interview.meetingUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="mt-2 inline-block text-sm text-blue-600 hover:text-blue-700"
                            >
                              打开会议链接
                            </a>
                          )}
                          {(interview.meetingId || interview.meetingPassword) && (
                            <div className="mt-2 text-xs text-gray-500">
                              会议号：{interview.meetingId ?? "-"} · 密码：{interview.meetingPassword ?? "-"}
                            </div>
                          )}
                          {interview.evaluations.length > 0 ? (
                            <div className="mt-4 space-y-3 border-t border-gray-100 pt-4">
                              {interview.evaluations.map((evaluation) => (
                                <div key={evaluation.id} className="space-y-2 text-sm text-gray-700">
                                  <div className="font-medium text-gray-900">
                                    {evaluation.interviewer} · {evaluation.result} · Score {evaluation.score}
                                  </div>
                                  <div className="text-xs text-gray-500">Submitted At: {new Date(evaluation.createdAt).toLocaleString("zh-CN")}</div>
                                  <div>{evaluation.evaluation}</div>
                                  {evaluation.strengths && <div>Strengths: {evaluation.strengths}</div>}
                                  {evaluation.weaknesses && <div>Weaknesses: {evaluation.weaknesses}</div>}
                                  {evaluation.suggestion && <div className="text-blue-600">Suggestion: {evaluation.suggestion}</div>}
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div className="mt-3 text-sm text-gray-500">当前还没有面试评价</div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })
              ) : (
                <div className="text-sm text-gray-500">暂无面试记录</div>
              )}
            </div>
          </div>
        </div>
      </div>

      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>编辑基础信息</DialogTitle>
            <DialogDescription>支持解析结果回填，并将当前手工确认值锁定为最终信息。</DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 md:grid-cols-2">
            <input value={editForm.name} onChange={(event) => setEditForm((prev) => ({ ...prev, name: event.target.value }))} className="rounded-lg border border-gray-300 px-3 py-2.5" placeholder="姓名" />
            <input value={editForm.phone} onChange={(event) => setEditForm((prev) => ({ ...prev, phone: event.target.value }))} className="rounded-lg border border-gray-300 px-3 py-2.5" placeholder="手机号" />
            <input value={editForm.email} onChange={(event) => setEditForm((prev) => ({ ...prev, email: event.target.value }))} className="rounded-lg border border-gray-300 px-3 py-2.5" placeholder="邮箱" />
            <input value={editForm.location} onChange={(event) => setEditForm((prev) => ({ ...prev, location: event.target.value }))} className="rounded-lg border border-gray-300 px-3 py-2.5" placeholder="城市" />
            <select
              value={editForm.departmentId}
              onChange={(event) =>
                setEditForm((prev) => ({
                  ...prev,
                  departmentId: event.target.value ? Number(event.target.value) : "",
                }))
              }
              className="rounded-lg border border-gray-300 px-3 py-2.5"
            >
              <option value="">未分配部门</option>
              {departments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
            <input value={editForm.education} onChange={(event) => setEditForm((prev) => ({ ...prev, education: event.target.value }))} className="rounded-lg border border-gray-300 px-3 py-2.5" placeholder="学历" />
            <input value={editForm.experience} onChange={(event) => setEditForm((prev) => ({ ...prev, experience: event.target.value }))} className="rounded-lg border border-gray-300 px-3 py-2.5" placeholder="工作年限" />
          </div>
          <textarea value={editForm.skillsSummary} onChange={(event) => setEditForm((prev) => ({ ...prev, skillsSummary: event.target.value }))} className="min-h-24 w-full rounded-lg border border-gray-300 px-3 py-2.5" placeholder="技能摘要" />
          <textarea value={editForm.projectSummary} onChange={(event) => setEditForm((prev) => ({ ...prev, projectSummary: event.target.value }))} className="min-h-24 w-full rounded-lg border border-gray-300 px-3 py-2.5" placeholder="项目摘要" />
          <DialogFooter>
            <button
              type="button"
              onClick={fillFromParse}
              disabled={!parseReport}
              className="inline-flex items-center justify-center rounded-lg border border-emerald-300 px-4 py-2 text-sm font-medium text-emerald-700 hover:bg-emerald-50 disabled:opacity-50"
            >
              使用解析结果回填
            </button>
            <button
              type="button"
              onClick={() => void saveManualFields()}
              disabled={actionLoading}
              className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              保存并锁定人工值
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={decisionDialogOpen} onOpenChange={setDecisionDialogOpen}>
        <DialogContent className="sm:max-w-4xl">
          <DialogHeader>
            <DialogTitle>辅助决策</DialogTitle>
            <DialogDescription>综合简历解析、部门反馈、面试进展与评价，为当前候选人生成可保留的决策建议。</DialogDescription>
          </DialogHeader>

          <div className="grid gap-6 md:grid-cols-[1.2fr,0.8fr]">
            <div className="space-y-4">
              <label className="block text-sm text-gray-700">
                决策关注点（可选）
                <input
                  value={decisionHint}
                  onChange={(event) => setDecisionHint(event.target.value)}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="例如：重点判断是否能直接推进 Offer，或是否还需要终面"
                />
              </label>

              {decisionLoading ? (
                <div className="flex min-h-52 items-center justify-center rounded-xl border border-dashed border-violet-200 bg-violet-50 text-sm text-violet-700">
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  正在综合分析候选人情况...
                </div>
              ) : selectedDecisionReport ? (
                <div className="space-y-4 rounded-xl border border-gray-200 bg-gray-50 p-4">
                  <div className="flex flex-wrap items-center gap-3">
                    <span className="rounded-full bg-violet-50 px-3 py-1 text-sm font-medium text-violet-700">
                      {selectedDecisionReport.recommendationLevel}
                    </span>
                    <span className="rounded-full bg-gray-100 px-3 py-1 text-sm font-medium text-gray-700">
                      综合评分 {selectedDecisionReport.recommendationScore}
                    </span>
                  </div>

                  <div>
                    <div className="text-sm font-medium text-gray-900">最新结论</div>
                    <div className="mt-1 text-sm text-gray-700">{selectedDecisionReport.conclusion}</div>
                  </div>

                  <div>
                      <div className="text-sm font-medium text-gray-900">推荐动作</div>
                    <div className="mt-1 rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm text-gray-700">
                      {selectedDecisionReport.recommendedAction}
                    </div>
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <div className="text-sm font-medium text-gray-900">关键优势</div>
                      <ul className="mt-2 space-y-1 text-sm text-gray-700">
                        {selectedDecisionReport.strengths.length > 0 ? (
                          selectedDecisionReport.strengths.map((item) => <li key={item}>- {item}</li>)
                        ) : (
                          <li>暂无</li>
                        )}
                      </ul>
                    </div>
                    <div>
                      <div className="text-sm font-medium text-gray-900">关键风险</div>
                      <ul className="mt-2 space-y-1 text-sm text-gray-700">
                        {selectedDecisionReport.risks.length > 0 ? (
                          selectedDecisionReport.risks.map((item) => <li key={item}>- {item}</li>)
                        ) : (
                          <li>暂无</li>
                        )}
                      </ul>
                    </div>
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <div className="text-sm font-medium text-gray-900">缺失信息 / 待补问</div>
                      <ul className="mt-2 space-y-1 text-sm text-gray-700">
                        {selectedDecisionReport.missingInformation.length > 0 ? (
                          selectedDecisionReport.missingInformation.map((item) => <li key={item}>- {item}</li>)
                        ) : (
                          <li>暂无</li>
                        )}
                      </ul>
                    </div>
                    <div>
                      <div className="text-sm font-medium text-gray-900">分析依据</div>
                      <ul className="mt-2 space-y-1 text-sm text-gray-700">
                        {selectedDecisionReport.supportingEvidence.length > 0 ? (
                          selectedDecisionReport.supportingEvidence.map((item) => <li key={item}>- {item}</li>)
                        ) : (
                          <li>暂无</li>
                        )}
                      </ul>
                    </div>
                  </div>

                  <div>
                    <div className="text-sm font-medium text-gray-900">推理摘要</div>
                    <div className="mt-1 text-sm text-gray-700">{selectedDecisionReport.reasoningSummary}</div>
                  </div>
                </div>
              ) : (
                <div className="rounded-xl border border-dashed border-violet-200 bg-violet-50 p-6 text-sm text-violet-700">
                  还没有辅助决策记录。点击下方按钮即可基于简历、部门反馈和面试记录生成首条建议。
                </div>
              )}
            </div>

            <div className="space-y-3">
              <div className="text-sm font-medium text-gray-900">历史记录</div>
              <div className="max-h-[420px] space-y-2 overflow-auto rounded-xl border border-gray-200 p-3">
                {decisionJobs.length > 0 ? (
                  decisionJobs.map((job) => {
                    const report: DecisionReport | null | undefined = job.result?.decisionReport;
                    const active = job.id === selectedDecisionJobId;
                    return (
                      <button
                        key={job.id}
                        type="button"
                        onClick={() => setSelectedDecisionJobId(job.id)}
                        className={`w-full rounded-lg border px-3 py-3 text-left transition-colors ${
                          active ? "border-violet-300 bg-violet-50" : "border-gray-200 hover:bg-gray-50"
                        }`}
                      >
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-sm font-medium text-gray-900">{report?.recommendationLevel ?? job.status}</span>
                          <span className="text-xs text-gray-500">{new Date(job.requestedAt).toLocaleString("zh-CN")}</span>
                        </div>
                        <div className="mt-1 text-sm text-gray-600 line-clamp-2">
                          {report?.conclusion ?? job.result?.summary ?? "暂无结论"}
                        </div>
                      </button>
                    );
                  })
                ) : (
                  <div className="text-sm text-gray-500">暂无历史记录</div>
                )}
              </div>
            </div>
          </div>

          <DialogFooter>
            <button
              type="button"
              onClick={() => void handleDecisionAnalysis()}
              disabled={decisionLoading}
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {decisionLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
              生成新的辅助决策
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}


