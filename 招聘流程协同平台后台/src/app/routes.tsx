import { createBrowserRouter } from "react-router";
import { MainLayout } from "./components/layout/MainLayout";
import { DepartmentLayout } from "./components/layout/DepartmentLayout";
import { Dashboard } from "./pages/Dashboard";
import { CandidateList } from "./pages/CandidateList";
import { CandidateCreate } from "./pages/CandidateCreate";
import { CandidateDetail } from "./pages/CandidateDetail";
import { DepartmentFeedback } from "./pages/DepartmentFeedback";
import { DailyReport } from "./pages/DailyReport";
import { DepartmentPending } from "./pages/DepartmentPending";
import { DepartmentCompleted } from "./pages/DepartmentCompleted";
import { MyInterviews } from "./pages/MyInterviews";
import { NotificationsPage } from "./pages/NotificationsPage";
import { InterviewerInterviewDetail } from "./pages/InterviewerInterviewDetail";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: MainLayout,
    children: [
      { index: true, Component: Dashboard },
      { path: "candidates", Component: CandidateList },
      { path: "candidates/new", Component: CandidateCreate },
      { path: "candidates/:id", Component: CandidateDetail },
      { path: "interviews/mine", Component: MyInterviews },
      { path: "interviews/:id", Component: InterviewerInterviewDetail },
      { path: "notifications", Component: NotificationsPage },
      { path: "report", Component: DailyReport },
    ],
  },
  {
    path: "/dept",
    Component: DepartmentLayout,
    children: [
      { index: true, Component: DepartmentPending },
      { path: "completed", Component: DepartmentCompleted },
    ],
  },
  {
    path: "/feedback/:id",
    Component: DepartmentFeedback,
  },
]);
