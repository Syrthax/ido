import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Clock, Folder, Trash2, Edit3 } from "lucide-react";
import { Task, TaskPriority } from "../types";
import { useStore } from "../store/useStore";
import { persistData } from "../store/useStore";
import { useSync } from "../hooks/useSync";

interface TaskCardProps {
  task: Task;
  onEdit?: (task: Task) => void;
}

const PRIORITY_COLOR: Record<TaskPriority, string> = {
  high: "#ef4444",
  medium: "#f97316",
  low: "#22c55e",
  none: "#6b7280",
};

const TaskCard: React.FC<TaskCardProps> = ({ task, onEdit }) => {
  const { toggleTaskDone, deleteTask, tasks, categories } = useStore();
  const { uploadData } = useSync();
  const [hovering, setHovering] = useState(false);

  const handleToggle = async () => {
    toggleTaskDone(task.id);
    setTimeout(() => {
      persistData({ tasks: useStore.getState().tasks, categories });
      uploadData();
    }, 100);
  };

  const handleDelete = async () => {
    deleteTask(task.id);
    setTimeout(() => {
      persistData({ tasks: useStore.getState().tasks, categories });
      uploadData();
    }, 100);
  };

  const isDone = task.status === "done";

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: -20 }}
      onHoverStart={() => setHovering(true)}
      onHoverEnd={() => setHovering(false)}
      style={{
        display: "flex",
        alignItems: "center",
        gap: "12px",
        paddingLeft: "16px",
        paddingRight: "16px",
        minHeight: "56px",
        position: "relative",
        background: hovering ? "rgba(255,255,255,0.06)" : "rgba(255,255,255,0.02)",
        opacity: isDone ? 0.6 : 1,
        transition: "background 0.15s cubic-bezier(0.4,0,0.2,1)",
        cursor: "default",
      }}
    >
      {/* Priority bar */}
      <div
        style={{
          position: "absolute",
          left: 0,
          top: "10px",
          bottom: "10px",
          width: "3px",
          borderRadius: "0 3px 3px 0",
          background: PRIORITY_COLOR[task.priority],
        }}
      />

      {/* Checkbox */}
      <motion.button
        whileTap={{ scale: 0.8 }}
        onClick={handleToggle}
        style={{
          flexShrink: 0,
          width: "20px",
          height: "20px",
          borderRadius: "6px",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          borderWidth: "2px",
          borderStyle: "solid",
          borderColor: isDone ? "var(--ido-accent)" : "rgba(255,255,255,0.2)",
          background: isDone ? "var(--ido-accent)" : "transparent",
          transition: "all 0.15s",
          cursor: "pointer",
        }}
        aria-label={isDone ? "Mark as pending" : "Mark as done"}
      >
        {isDone && (
          <motion.svg
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            width="10"
            height="10"
            viewBox="0 0 10 10"
            fill="none"
          >
            <path d="M1.5 5L4 7.5L8.5 2" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          </motion.svg>
        )}
      </motion.button>

      {/* Content */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <p
            style={{
              fontSize: "14px",
              fontWeight: 500,
              color: "var(--ido-text)",
              textDecoration: isDone ? "line-through" : "none",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
              opacity: isDone ? 0.6 : 1,
            }}
          >
            {task.title}
          </p>
        </div>

        {(task.scheduledTime || task.category || task.description) && (
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginTop: "3px" }}>
            {task.scheduledTime && (
              <span style={{ display: "flex", alignItems: "center", gap: "4px", fontSize: "12px", color: "var(--ido-text-muted)" }}>
                <Clock size={11} />
                {task.scheduledTime}
              </span>
            )}
            {task.category && (
              <span style={{ display: "flex", alignItems: "center", gap: "4px", fontSize: "12px", color: "var(--ido-text-muted)" }}>
                <Folder size={11} />
                {task.category}
              </span>
            )}
            {task.description && !task.scheduledTime && !task.category && (
              <span style={{ fontSize: "12px", color: "var(--ido-text-muted)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                {task.description}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Actions */}
      <AnimatePresence>
        {hovering && (
          <motion.div
            initial={{ opacity: 0, x: 6 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 6 }}
            transition={{ duration: 0.12 }}
            style={{ display: "flex", alignItems: "center", gap: "4px", flexShrink: 0 }}
          >
            {onEdit && (
              <button
                onClick={() => onEdit(task)}
                style={{
                  padding: "6px",
                  borderRadius: "8px",
                  background: "rgba(255,255,255,0.06)",
                  color: "var(--ido-text-muted)",
                  border: "none",
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
                aria-label="Edit task"
              >
                <Edit3 size={14} />
              </button>
            )}
            <button
              onClick={handleDelete}
              style={{
                padding: "6px",
                borderRadius: "8px",
                background: "rgba(239,68,68,0.1)",
                color: "#f87171",
                border: "none",
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
              aria-label="Delete task"
            >
              <Trash2 size={14} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default TaskCard;
