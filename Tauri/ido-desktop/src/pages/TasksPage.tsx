import React, { useState, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useStore } from "../store/useStore";
import TaskCard from "../components/TaskCard";
import CreateTaskModal from "../components/CreateTaskModal";
import { Task } from "../types";
import { format } from "date-fns";

const TasksPage: React.FC = () => {
  const { tasks, activeFilter, setActiveFilter } = useStore();
  const [editTask, setEditTask] = useState<Task | null>(null);
  const [editOpen, setEditOpen] = useState(false);

  const today = new Date().toISOString().split("T")[0];
  const greeting = useMemo(() => {
    const h = new Date().getHours();
    if (h < 12) return "Good Morning";
    if (h < 17) return "Good Afternoon";
    return "Good Evening";
  }, []);
  const userName = useStore((s) => s.auth.user?.name?.split(" ")[0] ?? "");

  const todayTasks = useMemo(
    () =>
      tasks
        .filter((t) => t.scheduledDate === today)
        .sort((a, b) => {
          if (activeFilter === "priority") {
            const order = { high: 0, medium: 1, low: 2, none: 3 };
            return order[a.priority] - order[b.priority];
          }
          const ta = a.scheduledTime ?? "99:99";
          const tb = b.scheduledTime ?? "99:99";
          return ta.localeCompare(tb);
        }),
    [tasks, today, activeFilter]
  );

  const pendingCount = todayTasks.filter((t) => t.status === "pending").length;

  const handleEdit = (task: Task) => {
    setEditTask(task);
    setEditOpen(true);
  };

  return (
    <motion.div
      key="tasks"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.22 }}
      style={{ height: "100%", overflowY: "auto", paddingBottom: "120px" }}
    >
      {/* Centered content container */}
      <div style={{ maxWidth: "720px", marginLeft: "auto", marginRight: "auto", paddingTop: "56px", paddingLeft: "24px", paddingRight: "24px" }}>

        {/* Greeting */}
        <h1 style={{ fontSize: "34px", fontWeight: 600, color: "var(--ido-text)", letterSpacing: "-0.6px", marginBottom: "32px", lineHeight: 1.15 }}>
          {greeting}, {userName}
        </h1>

        {/* Today's Focus header */}
        <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", marginBottom: "12px" }}>
          <div>
            <h2 style={{ fontSize: "13px", fontWeight: 700, color: "var(--ido-text-muted)", textTransform: "uppercase", letterSpacing: "0.7px" }}>
              Today&apos;s Focus
            </h2>
            <p style={{ fontSize: "12px", color: "var(--ido-text-muted)", marginTop: "3px" }}>
              {pendingCount === 0
                ? "All done! Great work."
                : `${pendingCount} task${pendingCount !== 1 ? "s" : ""} remaining`}
            </p>
          </div>
          <div style={{ display: "flex", gap: "6px" }}>
            {(["all", "priority"] as const).map((f) => (
              <button
                key={f}
                onClick={() => setActiveFilter(f)}
                style={{
                  padding: "5px 12px",
                  borderRadius: "999px",
                  fontSize: "12px",
                  fontWeight: 500,
                  background: activeFilter === f ? "var(--ido-accent)" : "rgba(255,255,255,0.05)",
                  color: activeFilter === f ? "white" : "var(--ido-text-muted)",
                  border: `1px solid ${activeFilter === f ? "transparent" : "rgba(255,255,255,0.07)"}`,
                  cursor: "pointer",
                  transition: "all 0.15s",
                }}
              >
                {f === "all" ? "All" : "Priority"}
              </button>
            ))}
          </div>
        </div>

        {/* Task list */}
        {todayTasks.length === 0 ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            style={{
              display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center",
              padding: "48px 24px", gap: "10px",
              borderRadius: "14px",
              background: "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.06)",
            }}
          >
            <div style={{ fontSize: "40px", lineHeight: 1 }}>✅</div>
            <p style={{ fontSize: "14px", fontWeight: 500, color: "var(--ido-text-muted)" }}>No tasks for today</p>
            <p style={{ fontSize: "12px", color: "rgba(255,255,255,0.25)" }}>Hit + to add one</p>
          </motion.div>
        ) : (
          <div
            style={{
              borderRadius: "14px",
              background: "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.06)",
              overflow: "hidden",
            }}
          >
            <AnimatePresence mode="popLayout">
              {todayTasks.map((task, i) => (
                <React.Fragment key={task.id}>
                  <TaskCard task={task} onEdit={handleEdit} />
                  {i < todayTasks.length - 1 && (
                    <div style={{ height: "1px", background: "rgba(255,255,255,0.05)", marginLeft: "52px" }} />
                  )}
                </React.Fragment>
              ))}
            </AnimatePresence>
          </div>
        )}

        {/* Upcoming tasks */}
        {(() => {
          const upcoming = tasks
            .filter((t) => t.scheduledDate && t.scheduledDate > today && t.status === "pending")
            .sort((a, b) => (a.scheduledDate ?? "").localeCompare(b.scheduledDate ?? ""))
            .slice(0, 5);
          if (upcoming.length === 0) return null;
          return (
            <div style={{ marginTop: "32px" }}>
              <h3 style={{ fontSize: "13px", fontWeight: 700, color: "var(--ido-text-muted)", textTransform: "uppercase", letterSpacing: "0.7px", marginBottom: "10px" }}>
                Upcoming
              </h3>
              <div
                style={{
                  borderRadius: "14px",
                  background: "rgba(255,255,255,0.03)",
                  border: "1px solid rgba(255,255,255,0.06)",
                  overflow: "hidden",
                }}
              >
                {upcoming.map((t, i) => (
                  <React.Fragment key={t.id}>
                    <TaskCard task={t} onEdit={handleEdit} />
                    {i < upcoming.length - 1 && (
                      <div style={{ height: "1px", background: "rgba(255,255,255,0.05)", marginLeft: "52px" }} />
                    )}
                  </React.Fragment>
                ))}
              </div>
            </div>
          );
        })()}
      </div>

      <CreateTaskModal
        open={editOpen}
        onClose={() => { setEditOpen(false); setEditTask(null); }}
        editTask={editTask}
      />
    </motion.div>
  );
};

export default TasksPage;
