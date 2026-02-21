import React, { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Calendar, Clock, AlertCircle, RefreshCcw } from "lucide-react";
import { Task, TaskPriority } from "../types";
import { useStore, persistData } from "../store/useStore";
import { useSync } from "../hooks/useSync";

interface CreateTaskModalProps {
  open: boolean;
  onClose: () => void;
  editTask?: Task | null;
}

const PRIORITIES: { value: TaskPriority; label: string; color: string }[] = [
  { value: "high", label: "High", color: "#ef4444" },
  { value: "medium", label: "Medium", color: "#f97316" },
  { value: "low", label: "Low", color: "#22c55e" },
  { value: "none", label: "No priority", color: "#6b7280" },
];

const CreateTaskModal: React.FC<CreateTaskModalProps> = ({ open, onClose, editTask }) => {
  const { addTask, updateTask, categories } = useStore();
  const { uploadData } = useSync();
  const inputRef = useRef<HTMLInputElement>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [date, setDate] = useState(new Date().toISOString().split("T")[0]);
  const [time, setTime] = useState("");
  const [priority, setPriority] = useState<TaskPriority>("none");
  const [category, setCategory] = useState("");
  const [showPriorityMenu, setShowPriorityMenu] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      if (editTask) {
        setTitle(editTask.title);
        setDescription(editTask.description ?? "");
        setDate(editTask.scheduledDate ?? new Date().toISOString().split("T")[0]);
        setTime(editTask.scheduledTime ?? "");
        setPriority(editTask.priority);
        setCategory(editTask.category ?? "");
      } else {
        setTitle("");
        setDescription("");
        setDate(new Date().toISOString().split("T")[0]);
        setTime("");
        setPriority("none");
        setCategory("");
      }
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [open, editTask]);

  const handleSubmit = async () => {
    if (!title.trim()) return;
    setLoading(true);
    try {
      const now = new Date().toISOString();
      if (editTask) {
        updateTask(editTask.id, {
          title: title.trim(),
          description: description.trim() || undefined,
          scheduledDate: date,
          scheduledTime: time || undefined,
          priority,
          category: category || undefined,
          updatedAt: now,
        });
      } else {
        const newTask: Task = {
          id: crypto.randomUUID(),
          title: title.trim(),
          description: description.trim() || undefined,
          scheduledDate: date,
          scheduledTime: time || undefined,
          priority,
          category: category || undefined,
          status: "pending",
          createdAt: now,
          updatedAt: now,
        };
        addTask(newTask);
      }
      setTimeout(() => {
        const s = useStore.getState();
        persistData({ tasks: s.tasks, categories: s.categories });
        uploadData();
      }, 50);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const selectedPriority = PRIORITIES.find((p) => p.value === priority)!;

  return (
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop */}
          <motion.div
            key="backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40"
            style={{ background: "rgba(0,0,0,0.5)", backdropFilter: "blur(4px)" }}
            onClick={onClose}
          />

          {/* Modal */}
          <motion.div
            key="modal"
            initial={{ opacity: 0, y: 40, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 40, scale: 0.97 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
            style={{
              position: "fixed",
              zIndex: 50,
              left: "50%",
              top: "50%",
              transform: "translate(-50%, -50%)",
              width: "520px",
              maxWidth: "calc(100vw - 48px)",
            }}
          >
            <div
              style={{
                borderRadius: "16px",
                padding: "24px",
                background: "rgba(22,22,26,0.97)",
                border: "1px solid rgba(255,255,255,0.09)",
                boxShadow: "0 24px 80px rgba(0,0,0,0.7), 0 2px 12px rgba(0,0,0,0.3)",
                backdropFilter: "blur(20px)",
                WebkitBackdropFilter: "blur(20px)",
              }}
            >
              {/* Header */}
              <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-2">
                  <RefreshCcw size={14} style={{ color: "var(--ido-accent)" }} />
                  <span className="text-xs font-bold tracking-widest" style={{ color: "var(--ido-accent)" }}>
                    {editTask ? "EDIT TASK" : "NEW TASK"}
                  </span>
                </div>
                <button
                  onClick={onClose}
                  className="p-1 rounded-lg transition-colors"
                  style={{ color: "var(--ido-text-muted)" }}
                >
                  <X size={18} />
                </button>
              </div>

              {/* Title input */}
              <input
                ref={inputRef}
                type="text"
                placeholder="What needs to be done?"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
                style={{
                  width: "100%",
                  height: "44px",
                  background: "transparent",
                  fontSize: "16px",
                  fontWeight: 500,
                  color: "var(--ido-text)",
                  caretColor: "var(--ido-accent)",
                  outline: "none",
                  border: "none",
                  borderBottom: "1px solid rgba(255,255,255,0.1)",
                  paddingBottom: "10px",
                  marginBottom: "16px",
                  display: "block",
                }}
              />

              {/* Description */}
              <textarea
                placeholder="Add description (optional)"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
                style={{
                  width: "100%",
                  background: "transparent",
                  fontSize: "13px",
                  outline: "none",
                  resize: "none",
                  marginBottom: "16px",
                  color: "var(--ido-text-muted)",
                  caretColor: "var(--ido-accent)",
                  border: "none",
                  display: "block",
                }}
              />

              {/* Controls row */}
              <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap", marginBottom: "20px" }}>
                {/* Date */}
                <label
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg cursor-pointer text-sm"
                  style={{ background: "var(--ido-surface-2)", color: "var(--ido-text-muted)" }}
                >
                  <Calendar size={14} />
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="bg-transparent outline-none text-sm cursor-pointer"
                    style={{ color: "var(--ido-text-muted)", width: "100px" }}
                  />
                </label>

                {/* Time */}
                <label
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg cursor-pointer text-sm"
                  style={{ background: "var(--ido-surface-2)", color: "var(--ido-text-muted)" }}
                >
                  <Clock size={14} />
                  <input
                    type="time"
                    value={time}
                    onChange={(e) => setTime(e.target.value)}
                    className="bg-transparent outline-none text-sm cursor-pointer"
                    style={{ color: "var(--ido-text-muted)", width: "80px" }}
                  />
                </label>

                {/* Priority */}
                <div className="relative">
                  <button
                    onClick={() => setShowPriorityMenu(!showPriorityMenu)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm"
                    style={{ background: "var(--ido-surface-2)", color: "var(--ido-text-muted)" }}
                  >
                    <AlertCircle size={14} style={{ color: selectedPriority.color }} />
                    {selectedPriority.label}
                  </button>
                  <AnimatePresence>
                    {showPriorityMenu && (
                      <motion.div
                        initial={{ opacity: 0, y: 6, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 6, scale: 0.95 }}
                        className="absolute bottom-full mb-1 left-0 rounded-xl overflow-hidden shadow-xl z-10"
                        style={{ background: "var(--ido-surface)", border: "1px solid var(--ido-border)", minWidth: "140px" }}
                      >
                        {PRIORITIES.map((p) => (
                          <button
                            key={p.value}
                            onClick={() => { setPriority(p.value); setShowPriorityMenu(false); }}
                            className="w-full flex items-center gap-2 px-3 py-2 text-sm text-left transition-colors hover:bg-white/5"
                            style={{ color: "var(--ido-text)" }}
                          >
                            <span className="w-2 h-2 rounded-full" style={{ background: p.color }} />
                            {p.label}
                          </button>
                        ))}
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>

                {/* Category */}
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="px-3 py-1.5 rounded-lg text-sm outline-none cursor-pointer"
                  style={{
                    background: "var(--ido-surface-2)",
                    color: category ? "var(--ido-text)" : "var(--ido-text-muted)",
                    border: "none",
                  }}
                >
                  <option value="">Category (optional)</option>
                  {categories.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              {/* Footer buttons */}
              <div style={{ display: "flex", alignItems: "center", gap: "12px", justifyContent: "flex-end", marginTop: "20px" }}>
                <button
                  onClick={onClose}
                  style={{
                    height: "36px",
                    paddingLeft: "16px",
                    paddingRight: "16px",
                    borderRadius: "10px",
                    fontSize: "13px",
                    fontWeight: 500,
                    color: "var(--ido-text-muted)",
                    background: "rgba(255,255,255,0.05)",
                    border: "1px solid rgba(255,255,255,0.08)",
                    cursor: "pointer",
                    transition: "all 0.15s",
                  }}
                >
                  Cancel
                </button>
                <motion.button
                  whileHover={{ scale: 1.03 }}
                  whileTap={{ scale: 0.97 }}
                  onClick={handleSubmit}
                  disabled={!title.trim() || loading}
                  style={{
                    height: "36px",
                    paddingLeft: "18px",
                    paddingRight: "18px",
                    borderRadius: "10px",
                    fontSize: "13px",
                    fontWeight: 600,
                    color: "white",
                    background: "var(--ido-accent)",
                    border: "none",
                    cursor: !title.trim() || loading ? "default" : "pointer",
                    opacity: !title.trim() || loading ? 0.5 : 1,
                    display: "flex",
                    alignItems: "center",
                    gap: "6px",
                    boxShadow: "0 2px 12px rgba(37,99,235,0.3)",
                    transition: "all 0.15s",
                  }}
                >
                  {loading ? (
                    <svg style={{ animation: "spin 1s linear infinite" }} width="13" height="13" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeOpacity="0.3"/>
                      <path d="M12 2a10 10 0 0110 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round"/>
                    </svg>
                  ) : (
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none"><path d="M5 3l14 9-14 9V3z" fill="currentColor"/></svg>
                  )}
                  {editTask ? "Save Changes" : "Create Task"}
                </motion.button>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

export default CreateTaskModal;
