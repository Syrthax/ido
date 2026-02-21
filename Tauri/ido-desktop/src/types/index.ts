export type TaskPriority = "high" | "medium" | "low" | "none";
export type TaskStatus = "pending" | "done";
export type ThemeMode = "dark" | "light" | "system";
export type CalendarView = "day" | "week" | "month" | "year";

export interface Task {
  id: string;
  title: string;
  description?: string;
  scheduledDate?: string; // YYYY-MM-DD
  scheduledTime?: string; // "HH:MM"
  priority: TaskPriority;
  status: TaskStatus;
  category?: string;
  tags?: string[];
  reminderAt?: string;    // ISO string for reminder time
  reminderMinutes?: number;
  createdAt: string;
  updatedAt: string;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  isGuest: boolean;
  user: UserProfile | null;
  accessToken: string | null;
  refreshToken: string | null;
  expiresAt: number | null;
}

export interface SyncState {
  lastSynced: string | null;
  isSyncing: boolean;
  syncError: string | null;
}

export interface IdoData {
  version: number;
  tasks: Task[];
  categories: string[];
  updatedAt: string;
}

export interface AppSettings {
  theme: ThemeMode;
  notificationsEnabled: boolean;
  calendarView: CalendarView;
  defaultCalendarView: CalendarView;
}
