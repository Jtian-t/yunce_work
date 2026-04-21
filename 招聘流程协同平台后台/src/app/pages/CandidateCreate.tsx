import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router";
import { ArrowLeft, FileText, Loader2, Sparkles, Upload } from "lucide-react";
import { useData, type CandidateUpsertPayload, type ParsedCandidateDraft } from "../context/DataContext";

type AssignmentMode = "POOL" | "ASSIGN";

const today = new Date().toISOString().slice(0, 10);

export function CandidateCreate() {
  const navigate = useNavigate();
  const {
    createCandidate,
    updateCandidate,
    uploadResume,
    createParseJob,
    loadLatestParseJob,
    advanceCandidate,
    loadDepartments,
    loadUsersByRole,
  } = useData();

  const [form, setForm] = useState<CandidateUpsertPayload>({
    name: "",
    position: "",
    source: "简历上传",
    submittedDate: today,
    nextAction: "待 HR 初筛 / 待分发",
    phone: "",
    email: "",
    location: "",
    experience: "",
    education: "",
    skillsSummary: "",
    projectSummary: "",
  });
  const [file, setFile] = useState<File | null>(null);
  const [draftCandidateId, setDraftCandidateId] = useState<number | null>(null);
  const [resumeUploaded, setResumeUploaded] = useState(false);
  const [assignmentMode, setAssignmentMode] = useState<AssignmentMode>("POOL");
  const [departmentId, setDepartmentId] = useState<number | "">("");
  const [reviewerId, setReviewerId] = useState<number | "">("");
  const [departments, setDepartments] = useState<Array<{ id: number; name: string }>>([]);
  const [reviewers, setReviewers] = useState<Array<{ id: number; displayName: string; departmentId: number | null }>>([]);
  const [loadingLookups, setLoadingLookups] = useState(false);
  const [parsing, setParsing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [parseHint, setParseHint] = useState("");
  const [parseMessage, setParseMessage] = useState<string | null>(null);

  const filteredReviewers = useMemo(() => {
    if (!departmentId) {
      return reviewers;
    }
    return reviewers.filter((reviewer) => reviewer.departmentId === Number(departmentId));
  }, [departmentId, reviewers]);

  async function ensureLookups() {
    if (departments.length > 0 && reviewers.length > 0) {
      return;
    }
    setLoadingLookups(true);
    try {
      const [departmentOptions, reviewerOptions] = await Promise.all([
        loadDepartments(),
        loadUsersByRole("DEPARTMENT_LEAD"),
      ]);
      setDepartments(departmentOptions.map((item) => ({ id: item.id, name: item.name })));
      setReviewers(
        reviewerOptions.map((item) => ({
          id: item.id,
          displayName: item.displayName,
          departmentId: item.departmentId,
        }))
      );
    } finally {
      setLoadingLookups(false);
    }
  }

  function deriveDraftPayload(): CandidateUpsertPayload {
    const fileName = file?.name.replace(/\.[^.]+$/, "") ?? "";
    return {
      ...form,
      name: form.name.trim() || fileName || "待解析候选人",
      position: form.position.trim() || "待定岗位",
      source: form.source.trim() || "简历上传",
      submittedDate: form.submittedDate || today,
      nextAction: "待 HR 初筛 / 待分发",
      departmentId: null,
      ownerId: null,
    };
  }

  function mergeParsedDraft(parsed?: ParsedCandidateDraft | null) {
    if (!parsed) {
      return;
    }
    setForm((current) => ({
      ...current,
      name: current.name || parsed.name || "",
      phone: current.phone || parsed.phone || "",
      email: current.email || parsed.email || "",
      education: current.education || parsed.education || "",
      experience: current.experience || parsed.experience || "",
      skillsSummary: current.skillsSummary || parsed.skillsSummary || "",
      projectSummary: current.projectSummary || parsed.projectSummary || "",
    }));
  }

  async function waitForParse(candidateId: number) {
    for (let attempt = 0; attempt < 8; attempt += 1) {
      const latest = await loadLatestParseJob(candidateId);
      if (latest.status === "SUCCEEDED") {
        return latest;
      }
      if (latest.status === "FAILED") {
        throw new Error(latest.lastError || "简历解析失败");
      }
      await new Promise((resolve) => window.setTimeout(resolve, 400));
    }
    return loadLatestParseJob(candidateId);
  }

  async function handleParseResume() {
    if (!file) {
      setError("请先选择 PDF 简历文件");
      return;
    }

    setParsing(true);
    setError(null);
    setParseMessage(null);
    try {
      let candidateId = draftCandidateId;
      if (!candidateId) {
        const draft = await createCandidate(deriveDraftPayload());
        candidateId = draft.id;
        setDraftCandidateId(draft.id);
      } else {
        await updateCandidate(candidateId, deriveDraftPayload());
      }

      await uploadResume(candidateId, file);
      setResumeUploaded(true);
      await createParseJob(candidateId, parseHint || undefined);
      const latest = await waitForParse(candidateId);
      mergeParsedDraft(latest.result?.parsedCandidateDraft);
      setParseMessage(latest.status === "SUCCEEDED" ? "已根据简历自动预填，可继续手动调整" : "解析已创建，请稍后刷新");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "简历解析失败");
    } finally {
      setParsing(false);
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      let candidateId = draftCandidateId;

      if (candidateId) {
        const updated = await updateCandidate(candidateId, {
          ...form,
          nextAction: assignmentMode === "POOL" ? "待 HR 初筛 / 待分发" : "待分发部门",
          departmentId: null,
          ownerId: null,
        });
        candidateId = updated.id;
      } else {
        const created = await createCandidate({
          ...form,
          nextAction: assignmentMode === "POOL" ? "待 HR 初筛 / 待分发" : "待分发部门",
          departmentId: null,
          ownerId: null,
        });
        candidateId = created.id;
      }

      if (file && !resumeUploaded) {
        await uploadResume(candidateId, file);
        setResumeUploaded(true);
      }

      if (assignmentMode === "ASSIGN") {
        if (!departmentId || !reviewerId) {
          throw new Error("请选择要分发的部门和负责人");
        }
        await advanceCandidate(candidateId, {
          action: "ASSIGN_TO_DEPARTMENT",
          departmentId: Number(departmentId),
          reviewerId: Number(reviewerId),
        });
      } else {
        await advanceCandidate(candidateId, {
          action: "MOVE_TO_POOL",
        });
      }

      navigate(`/candidates/${candidateId}`);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "保存候选人失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="p-6 space-y-6">
      <Link to="/candidates" className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900">
        <ArrowLeft className="w-4 h-4" />
        返回候选人列表
      </Link>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="rounded-2xl border border-blue-200 bg-gradient-to-r from-blue-600 to-cyan-600 px-6 py-8 text-white">
        <h1 className="text-2xl font-bold">新增候选人</h1>
        <p className="mt-2 text-sm text-blue-100">支持上传 PDF 简历自动解析，也支持完全手动录入。未准备推进的候选人会先进入简历池留存。</p>
      </div>

      <form onSubmit={handleSubmit} className="grid gap-6 lg:grid-cols-[1.2fr,0.8fr]">
        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">基础信息</h2>
              {draftCandidateId && (
                <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                  草稿 ID #{draftCandidateId}
                </span>
              )}
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <label className="text-sm text-gray-700">
                姓名
                <input
                  value={form.name}
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="例如：张三"
                  required
                />
              </label>
              <label className="text-sm text-gray-700">
                应聘岗位
                <input
                  value={form.position}
                  onChange={(event) => setForm((current) => ({ ...current, position: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="例如：Java 工程师"
                  required
                />
              </label>
              <label className="text-sm text-gray-700">
                手机号
                <input
                  value={form.phone}
                  onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="11 位手机号"
                />
              </label>
              <label className="text-sm text-gray-700">
                邮箱
                <input
                  value={form.email}
                  onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="candidate@example.com"
                />
              </label>
              <label className="text-sm text-gray-700">
                学历
                <input
                  value={form.education}
                  onChange={(event) => setForm((current) => ({ ...current, education: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="本科 / 硕士 / 博士"
                />
              </label>
              <label className="text-sm text-gray-700">
                工作年限
                <input
                  value={form.experience}
                  onChange={(event) => setForm((current) => ({ ...current, experience: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="例如：3年"
                />
              </label>
              <label className="text-sm text-gray-700">
                所在地
                <input
                  value={form.location}
                  onChange={(event) => setForm((current) => ({ ...current, location: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="例如：上海"
                />
              </label>
              <label className="text-sm text-gray-700">
                来源
                <input
                  value={form.source}
                  onChange={(event) => setForm((current) => ({ ...current, source: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="Boss 直聘 / 内推 / 猎头"
                  required
                />
              </label>
            </div>

            <div className="mt-4 grid gap-4">
              <label className="text-sm text-gray-700">
                技能摘要
                <textarea
                  value={form.skillsSummary}
                  onChange={(event) => setForm((current) => ({ ...current, skillsSummary: event.target.value }))}
                  className="mt-1 min-h-24 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="例如：Java, Spring Boot, MySQL, React"
                />
              </label>
              <label className="text-sm text-gray-700">
                项目摘要
                <textarea
                  value={form.projectSummary}
                  onChange={(event) => setForm((current) => ({ ...current, projectSummary: event.target.value }))}
                  className="mt-1 min-h-28 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                  placeholder="填写项目经历或让简历解析自动预填"
                />
              </label>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900">简历解析</h2>
            <label className="mt-4 flex cursor-pointer items-center justify-center rounded-xl border border-dashed border-blue-300 bg-blue-50 px-4 py-6 text-center">
              <input
                type="file"
                accept="application/pdf"
                className="hidden"
                onChange={(event) => {
                  const nextFile = event.target.files?.[0] ?? null;
                  setFile(nextFile);
                  setResumeUploaded(false);
                }}
              />
              <div>
                <Upload className="mx-auto mb-3 h-6 w-6 text-blue-600" />
                <div className="text-sm font-medium text-gray-900">{file ? file.name : "选择 PDF 简历文件"}</div>
                <div className="mt-1 text-xs text-gray-500">上传后可自动解析姓名、电话、邮箱、学历和项目摘要</div>
              </div>
            </label>

            <label className="mt-4 block text-sm text-gray-700">
              解析提示词（可选）
              <input
                value={parseHint}
                onChange={(event) => setParseHint(event.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                placeholder="例如：重点提取 Java 后端经历"
              />
            </label>

            <button
              type="button"
              onClick={() => void handleParseResume()}
              disabled={!file || parsing}
              className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {parsing ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
              上传并解析简历
            </button>

            {parseMessage && (
              <div className="mt-3 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
                {parseMessage}
              </div>
            )}
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900">入池或分发</h2>
            <div className="mt-4 space-y-3">
              <label className="flex items-start gap-3 rounded-lg border border-gray-200 px-4 py-3">
                <input
                  type="radio"
                  checked={assignmentMode === "POOL"}
                  onChange={() => setAssignmentMode("POOL")}
                  className="mt-1"
                />
                <div>
                  <div className="font-medium text-gray-900">先进入简历池</div>
                  <div className="text-sm text-gray-500">留存简历和解析结果，后续由 HR 再决定是否推进</div>
                </div>
              </label>
              <label className="flex items-start gap-3 rounded-lg border border-gray-200 px-4 py-3">
                <input
                  type="radio"
                  checked={assignmentMode === "ASSIGN"}
                  onChange={() => {
                    setAssignmentMode("ASSIGN");
                    void ensureLookups();
                  }}
                  className="mt-1"
                />
                <div>
                  <div className="font-medium text-gray-900">立即分发到部门</div>
                  <div className="text-sm text-gray-500">创建后直接进入部门待办，等待负责人反馈</div>
                </div>
              </label>
            </div>

            {assignmentMode === "ASSIGN" && (
              <div className="mt-4 space-y-4">
                <label className="block text-sm text-gray-700">
                  部门
                  <select
                    value={departmentId}
                    onChange={(event) => setDepartmentId(event.target.value ? Number(event.target.value) : "")}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                    disabled={loadingLookups}
                  >
                    <option value="">请选择部门</option>
                    {departments.map((department) => (
                      <option key={department.id} value={department.id}>
                        {department.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="block text-sm text-gray-700">
                  部门负责人
                  <select
                    value={reviewerId}
                    onChange={(event) => setReviewerId(event.target.value ? Number(event.target.value) : "")}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5"
                    disabled={loadingLookups}
                  >
                    <option value="">请选择负责人</option>
                    {filteredReviewers.map((reviewer) => (
                      <option key={reviewer.id} value={reviewer.id}>
                        {reviewer.displayName}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            )}
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <div className="flex items-start gap-3 text-sm text-gray-600">
              <FileText className="mt-0.5 h-5 w-5 text-blue-600" />
              <div className="space-y-1">
                <p>如果简历解析不到完整信息，保留手动填写即可。</p>
                <p>首次点击“上传并解析简历”时，系统会自动创建一个候选人草稿并上传简历。</p>
              </div>
            </div>
          </div>

          <div className="flex gap-3">
            <Link
              to="/candidates"
              className="flex-1 rounded-lg border border-gray-300 px-4 py-3 text-center text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              取消
            </Link>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 rounded-lg bg-blue-600 px-4 py-3 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting ? "保存中..." : "保存候选人"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
