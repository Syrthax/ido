/**
 * Bidirectional schema adapter between:
 *   mobile / web ido-data.json  ←→  desktop Task interface
 *
 * Mobile schema (what lives on Google Drive):
 *   { id, text, done: bool, priority: bool, dueDate: ISO, reminderTime, deleted, createdAt, updatedAt }
 *
 * Desktop schema (our TypeScript Task interface):
 *   { id, title, status: "pending"|"done", priority: "high"|"medium"|"low"|"none",
 *     scheduledDate: "YYYY-MM-DD", scheduledTime: "HH:MM", ... }
 */

import { Task, TaskPriority, IdoData } from "../types";

const D = (msg: string, ...args: unknown[]) =>
  console.log(`[IDO DEBUG] [adapter] ${msg}`, ...args);

/** Convert any raw task object (mobile OR desktop format) → desktop Task */
export function normalizeTask(raw: Record<string, any>): Task | null {
  if (!raw?.id) return null;

  // Skip deleted tasks
  if (raw.deleted === true) return null;

  // ── title ─────────────────────────────────────────────────────────────
  const title: string = raw.title ?? raw.text ?? "(no title)";

  // ── status ────────────────────────────────────────────────────────────
  let status: "pending" | "done";
  if (typeof raw.status === "string" && (raw.status === "pending" || raw.status === "done")) {
    status = raw.status;
  } else {
    status = raw.done === true ? "done" : "pending";
  }

  // ── priority ──────────────────────────────────────────────────────────
  let priority: TaskPriority;
  if (typeof raw.priority === "string") {
    const p = raw.priority as string;
    if (p === "high" || p === "medium" || p === "low" || p === "none") {
      priority = p as TaskPriority;
    } else {
      priority = "none";
    }
  } else if (typeof raw.priority === "boolean") {
    // Mobile: priority=true means high, priority=false means none
    priority = raw.priority ? "high" : "none";
  } else {
    priority = "none";
  }

  // ── scheduledDate / scheduledTime ──────────────────────────────────────
  // Sources in priority order: scheduledDate, dueDate
  let scheduledDate: string | undefined;
  let scheduledTime: string | undefined;

  if (raw.scheduledDate) {
    scheduledDate = raw.scheduledDate;
    scheduledTime = raw.scheduledTime ?? undefined;
  } else if (raw.dueDate) {
    try {
      const d = new Date(raw.dueDate);
      scheduledDate = d.toISOString().split("T")[0];
      // Only capture time if it's not midnight UTC (i.e. actually set)
      const hours = d.getUTCHours();
      const mins = d.getUTCMinutes();
      if (hours !== 0 || mins !== 0) {
        scheduledTime = `${String(hours).padStart(2, "0")}:${String(mins).padStart(2, "0")}`;
      }
    } catch {
      // ignore bad dates
    }
  }

  return {
    id: raw.id,
    title,
    description: raw.description ?? "",
    status,
    priority,
    category: raw.category ?? undefined,
    scheduledDate,
    scheduledTime,
    tags: Array.isArray(raw.tags) ? raw.tags : [],
    reminderAt: raw.reminderAt ?? raw.reminderTime ?? undefined,
    createdAt: raw.createdAt ?? new Date().toISOString(),
    updatedAt: raw.updatedAt ?? new Date().toISOString(),
  };
}

/** Convert desktop Task → mobile-compatible format for Drive storage */
export function toMobileTask(task: Task): Record<string, any> {
  const dueDate = task.scheduledDate
    ? task.scheduledTime
      ? new Date(`${task.scheduledDate}T${task.scheduledTime}:00`).toISOString()
      : new Date(`${task.scheduledDate}T12:00:00`).toISOString()
    : null;

  return {
    // Mobile-compatible fields
    id: task.id,
    text: task.title,
    done: task.status === "done",
    priority: task.priority === "high" || task.priority === "medium",
    dueDate,
    reminderTime: task.reminderAt ?? null,
    notified: false,
    deleted: false,
    // Desktop-extended fields (ignored by mobile, preserved for desktop)
    title: task.title,
    description: task.description ?? "",
    status: task.status,
    priorityLevel: task.priority,
    scheduledDate: task.scheduledDate ?? null,
    scheduledTime: task.scheduledTime ?? null,
    category: task.category ?? null,
    tags: task.tags ?? [],
    createdAt: task.createdAt,
    updatedAt: task.updatedAt,
  };
}

/** Normalize an entire IdoData payload from Drive (handles schema differences) */
export function normalizeIdoData(raw: Record<string, any>): {
  tasks: Task[];
  categories: string[];
  updatedAt: string;
  version: number;
} {
  const rawTasks: Record<string, any>[] = Array.isArray(raw.tasks) ? raw.tasks : [];
  D("normalizeIdoData: raw task count =", rawTasks.length);

  const tasks: Task[] = [];
  let skipped = 0;
  for (const t of rawTasks) {
    const normalized = normalizeTask(t);
    if (normalized) {
      tasks.push(normalized);
    } else {
      skipped++;
    }
  }

  D(`normalizeIdoData: kept ${tasks.length} tasks, skipped ${skipped} (deleted/invalid)`);

  const defaultCategories = ["Finance", "Product", "Client", "Personal", "Work"];
  const categories: string[] = Array.isArray(raw.categories) && raw.categories.length > 0
    ? raw.categories
    : defaultCategories;

  return {
    version: raw.version ?? 1,
    tasks,
    categories,
    updatedAt: raw.updatedAt ?? new Date().toISOString(),
  };
}

/** Convert desktop store data back to Drive format */
export function toIdoData(tasks: Task[], categories: string[]): string {
  const mobileTasks = tasks.map(toMobileTask);
  const payload = {
    version: 1,
    tasks: mobileTasks,
    categories,
    updatedAt: new Date().toISOString(),
  };
  return JSON.stringify(payload);
}
