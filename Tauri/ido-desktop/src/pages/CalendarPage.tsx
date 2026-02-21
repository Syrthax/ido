import React, { useState, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  format,
  startOfMonth,
  endOfMonth,
  startOfWeek,
  endOfWeek,
  eachDayOfInterval,
  isSameMonth,
  isSameDay,
  addMonths,
  subMonths,
  addWeeks,
  subWeeks,
  addDays,
  subDays,
  parseISO,
  isToday,
} from "date-fns";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useStore } from "../store/useStore";
import { Task, TaskPriority } from "../types";
import CreateTaskModal from "../components/CreateTaskModal";
import { DndContext, DragEndEvent, DragOverlay, useDraggable, useDroppable } from "@dnd-kit/core";
import { persistData, useStore as useStoreRaw } from "../store/useStore";
import { useSync } from "../hooks/useSync";

type CalView = "day" | "week" | "month";

// ---- View Mode Button ----
const ViewButton: React.FC<{ label: string; active: boolean; onClick: () => void }> = ({
  label,
  active,
  onClick,
}) => {
  const [hovering, setHovering] = React.useState(false);
  return (
    <button
      onClick={onClick}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={() => setHovering(false)}
      style={{
        height: "32px",
        paddingLeft: "14px",
        paddingRight: "14px",
        borderRadius: "8px",
        fontSize: "13px",
        fontWeight: 500,
        border: "none",
        cursor: "pointer",
        transition: "all 160ms ease",
        background: active
          ? "linear-gradient(180deg, rgba(80,120,255,0.9), rgba(80,120,255,0.7))"
          : hovering
          ? "rgba(255,255,255,0.08)"
          : "transparent",
        color: active ? "white" : "rgba(255,255,255,0.6)",
        boxShadow: active ? "0 4px 12px rgba(80,120,255,0.35)" : "none",
        outline: "none",
      }}
    >
      {label}
    </button>
  );
};

const PRIORITY_COLORS: Record<TaskPriority, string> = {
  high: "#ef4444",
  medium: "#f97316",
  low: "#22c55e",
  none: "#6b7280",
};

const CalendarPage: React.FC = () => {
  const { tasks, getTasksByDate, updateTask } = useStore();
  const { uploadData } = useSync();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [view, setView] = useState<CalView>("month");
  const [selectedDate, setSelectedDate] = useState<Date | null>(new Date());
  const [createOpen, setCreateOpen] = useState(false);
  const [activeId, setActiveId] = useState<string | null>(null);

  const navigate = (dir: 1 | -1) => {
    if (view === "month") setCurrentDate(dir === 1 ? addMonths(currentDate, 1) : subMonths(currentDate, 1));
    else if (view === "week") setCurrentDate(dir === 1 ? addWeeks(currentDate, 1) : subWeeks(currentDate, 1));
    else setCurrentDate(dir === 1 ? addDays(currentDate, 1) : subDays(currentDate, 1));
  };

  const handleDragEnd = (e: DragEndEvent) => {
    setActiveId(null);
    const { active, over } = e;
    if (!over || active.id === over.id) return;
    const taskId = active.id as string;
    const newDate = over.id as string;
    updateTask(taskId, { scheduledDate: newDate });
    setTimeout(() => {
      const s = useStoreRaw.getState();
      persistData({ tasks: s.tasks, categories: s.categories });
      uploadData();
    }, 50);
  };

  const title = useMemo(() => {
    if (view === "month") return format(currentDate, "MMMM yyyy");
    if (view === "week") {
      const start = startOfWeek(currentDate, { weekStartsOn: 1 });
      const end = endOfWeek(currentDate, { weekStartsOn: 1 });
      return `${format(start, "MMM d")} – ${format(end, "MMM d, yyyy")}`;
    }
    return format(currentDate, "EEEE, MMMM d, yyyy");
  }, [currentDate, view]);

  const activeTask = activeId ? tasks.find((t) => t.id === activeId) : null;

  return (
    <motion.div
      key="calendar"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.25 }}
      className="h-full flex flex-col overflow-hidden"
    >
      {/* Calendar header */}
      <div className="flex items-center justify-between px-6 py-3 flex-shrink-0">
        <div className="flex items-center gap-2">
          <motion.button whileTap={{ scale: 0.9 }} onClick={() => navigate(-1)}
            className="p-1.5 rounded-lg" style={{ color: "var(--ido-text-muted)" }}>
            <ChevronLeft size={18} />
          </motion.button>
          <span className="text-base font-semibold min-w-36 text-center" style={{ color: "var(--ido-text)" }}>
            {title}
          </span>
          <motion.button whileTap={{ scale: 0.9 }} onClick={() => navigate(1)}
            className="p-1.5 rounded-lg" style={{ color: "var(--ido-text-muted)" }}>
            <ChevronRight size={18} />
          </motion.button>
        </div>
        <div style={{
          display: "flex",
          gap: "4px",
          background: "rgba(255,255,255,0.04)",
          padding: "4px",
          borderRadius: "12px",
          backdropFilter: "blur(10px)",
          WebkitBackdropFilter: "blur(10px)",
        }}>
          {(["Day", "Week", "Month"] as const).map((label) => {
            const v = label.toLowerCase() as CalView;
            const active = view === v;
            return (
              <ViewButton
                key={v}
                label={label}
                active={active}
                onClick={() => setView(v)}
              />
            );
          })}
        </div>
      </div>

      <DndContext onDragEnd={handleDragEnd} onDragStart={(e) => setActiveId(e.active.id as string)}>
        <div className="flex-1 overflow-auto px-3 pb-28">
          {view === "month" && (
            <MonthView
              currentDate={currentDate}
              tasks={tasks}
              selectedDate={selectedDate}
              onSelectDate={setSelectedDate}
            />
          )}
          {view === "week" && (
            <WeekView currentDate={currentDate} tasks={tasks} />
          )}
          {view === "day" && (
            <DayView date={currentDate} tasks={tasks} />
          )}
        </div>
        <DragOverlay>
          {activeTask && (
            <div className="px-2 py-1 rounded text-xs text-white font-medium shadow-lg"
              style={{ background: PRIORITY_COLORS[activeTask.priority], maxWidth: 120 }}>
              {activeTask.title}
            </div>
          )}
        </DragOverlay>
      </DndContext>

      <CreateTaskModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </motion.div>
  );
};

// ---- Month View ----
const MonthView: React.FC<{
  currentDate: Date;
  tasks: Task[];
  selectedDate: Date | null;
  onSelectDate: (d: Date) => void;
}> = ({ currentDate, tasks, selectedDate, onSelectDate }) => {
  const monthStart = startOfMonth(currentDate);
  const monthEnd = endOfMonth(currentDate);
  const gridStart = startOfWeek(monthStart, { weekStartsOn: 1 });
  const gridEnd = endOfWeek(monthEnd, { weekStartsOn: 1 });
  const days = eachDayOfInterval({ start: gridStart, end: gridEnd });
  const DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

  return (
    <div>
      {/* Day headers */}
      <div className="grid grid-cols-7 mb-1">
        {DAYS.map((d) => (
          <div key={d} className="text-center text-xs font-semibold py-1.5" style={{ color: "var(--ido-text-muted)" }}>
            {d}
          </div>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-0.5">
        {days.map((day) => {
          const dateStr = format(day, "yyyy-MM-dd");
          const dayTasks = tasks.filter((t) => t.scheduledDate === dateStr);
          const inMonth = isSameMonth(day, currentDate);
          const today = isToday(day);
          const selected = selectedDate && isSameDay(day, selectedDate);
          return (
            <DroppableDay key={dateStr} dateStr={dateStr}>
              <motion.div
                whileHover={{ scale: 1.02 }}
                onClick={() => onSelectDate(day)}
                className="min-h-20 p-1.5 rounded-xl cursor-pointer transition-colors"
                style={{
                  background: selected ? "rgba(37,99,235,0.1)" : "var(--ido-surface)",
                  border: today ? "1.5px solid var(--ido-accent)" : "1px solid var(--ido-border)",
                  opacity: inMonth ? 1 : 0.35,
                }}
              >
                <div className="flex items-center justify-between mb-1">
                  <span
                    className={`text-xs font-semibold w-6 h-6 flex items-center justify-center rounded-full`}
                    style={{
                      background: today ? "var(--ido-accent)" : "transparent",
                      color: today ? "white" : "var(--ido-text)",
                    }}
                  >
                    {format(day, "d")}
                  </span>
                  {today && (
                    <span className="text-[9px] font-bold px-1 rounded" style={{ background: "var(--ido-accent)", color: "white" }}>
                      TODAY
                    </span>
                  )}
                </div>
                <div className="flex flex-col gap-0.5">
                  {dayTasks.slice(0, 2).map((t) => (
                    <DraggableTask key={t.id} task={t} />
                  ))}
                  {dayTasks.length > 2 && (
                    <span className="text-[10px]" style={{ color: "var(--ido-text-muted)" }}>
                      +{dayTasks.length - 2} more
                    </span>
                  )}
                  {dayTasks.length === 0 && (
                    <div className="flex gap-0.5 flex-wrap mt-1">
                      {/* Colored dot placeholders that give visual depth */}
                    </div>
                  )}
                </div>
              </motion.div>
            </DroppableDay>
          );
        })}
      </div>
    </div>
  );
};

const DroppableDay: React.FC<{ dateStr: string; children: React.ReactNode }> = ({ dateStr, children }) => {
  const { setNodeRef, isOver } = useDroppable({ id: dateStr });
  return (
    <div
      ref={setNodeRef}
      style={{ outline: isOver ? "2px solid var(--ido-accent)" : "none", borderRadius: 12 }}
    >
      {children}
    </div>
  );
};

const DraggableTask: React.FC<{ task: Task }> = ({ task }) => {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({ id: task.id });
  return (
    <div
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      className="flex items-center gap-1 px-1 py-0.5 rounded text-[10px] font-medium cursor-grab select-none"
      style={{
        opacity: isDragging ? 0.4 : 1,
        maxWidth: "100%",
      }}
    >
      <span
        className="w-1.5 h-1.5 rounded-full flex-shrink-0"
        style={{ background: PRIORITY_COLORS[task.priority] }}
      />
      <span className="truncate" style={{ color: "var(--ido-text)" }}>{task.title}</span>
    </div>
  );
};

// ---- Week View ----
const WeekView: React.FC<{ currentDate: Date; tasks: Task[] }> = ({ currentDate, tasks }) => {
  const start = startOfWeek(currentDate, { weekStartsOn: 1 });
  const days = Array.from({ length: 7 }, (_, i) => addDays(start, i));
  return (
    <div className="grid grid-cols-7 gap-2">
      {days.map((day) => {
        const dateStr = format(day, "yyyy-MM-dd");
        const dayTasks = tasks.filter((t) => t.scheduledDate === dateStr);
        return (
          <DroppableDay key={dateStr} dateStr={dateStr}>
            <div
              className="rounded-xl p-2 min-h-36"
              style={{
                background: "var(--ido-surface)",
                border: isToday(day) ? "1.5px solid var(--ido-accent)" : "1px solid var(--ido-border)",
              }}
            >
              <p className="text-xs font-semibold mb-2 text-center" style={{ color: isToday(day) ? "var(--ido-accent)" : "var(--ido-text)" }}>
                {format(day, "EEE")}<br />
                {format(day, "d")}
              </p>
              <div className="flex flex-col gap-1">
                {dayTasks.map((t) => <DraggableTask key={t.id} task={t} />)}
              </div>
            </div>
          </DroppableDay>
        );
      })}
    </div>
  );
};

// ---- Day View ----
const DayView: React.FC<{ date: Date; tasks: Task[] }> = ({ date, tasks }) => {
  const dateStr = format(date, "yyyy-MM-dd");
  const dayTasks = tasks.filter((t) => t.scheduledDate === dateStr);
  const hours = Array.from({ length: 24 }, (_, i) => i);
  return (
    <div className="flex flex-col gap-0.5">
      {hours.map((h) => {
        const hStr = `${String(h).padStart(2, "0")}:`;
        const hTasks = dayTasks.filter((t) => t.scheduledTime?.startsWith(hStr));
        return (
          <div key={h} className="flex gap-2 min-h-10 items-start">
            <span className="text-xs w-12 pt-2 text-right flex-shrink-0" style={{ color: "var(--ido-text-muted)" }}>
              {format(new Date(2020, 0, 1, h), "h a")}
            </span>
            <div
              className="flex-1 border-t pt-1.5 pb-1.5 flex flex-col gap-1"
              style={{ borderColor: "var(--ido-border)" }}
            >
              {hTasks.map((t) => (
                <div
                  key={t.id}
                  className="px-2 py-1 rounded-lg text-xs font-medium"
                  style={{
                    background: PRIORITY_COLORS[t.priority] + "22",
                    color: "var(--ido-text)",
                    borderLeft: `3px solid ${PRIORITY_COLORS[t.priority]}`,
                  }}
                >
                  {t.scheduledTime} {t.title}
                </div>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default CalendarPage;
