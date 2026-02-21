import { create } from "zustand";
import { Task, AuthState, SyncState, AppSettings, IdoData, UserProfile } from "../types";

interface AppStore {
  // Auth
  auth: AuthState;
  setAuth: (auth: Partial<AuthState>) => void;
  logout: () => void;

  // Tasks
  tasks: Task[];
  setTasks: (tasks: Task[]) => void;
  addTask: (task: Task) => void;
  updateTask: (id: string, updates: Partial<Task>) => void;
  deleteTask: (id: string) => void;
  toggleTaskDone: (id: string) => void;

  // Categories
  categories: string[];
  setCategories: (cats: string[]) => void;
  addCategory: (cat: string) => void;

  // Sync
  sync: SyncState;
  setSyncState: (s: Partial<SyncState>) => void;

  // Settings
  settings: AppSettings;
  updateSettings: (s: Partial<AppSettings>) => void;

  // UI state
  activeFilter: "all" | "priority";
  setActiveFilter: (f: "all" | "priority") => void;

  // Derived helpers
  getTodayTasks: () => Task[];
  getPendingTasks: () => Task[];
  getTasksByDate: (date: string) => Task[];
}

const defaultSettings: AppSettings = {
  theme: "system",
  notificationsEnabled: true,
  calendarView: "month",
  defaultCalendarView: "month",
};

export const useStore = create<AppStore>((set, get) => ({
  auth: {
    isAuthenticated: false,
    isGuest: false,
    user: null,
    accessToken: null,
    refreshToken: null,
    expiresAt: null,
  },

  setAuth: (auth) =>
    set((s) => ({ auth: { ...s.auth, ...auth } })),

  logout: () =>
    set({
      auth: {
        isAuthenticated: false,
        isGuest: false,
        user: null,
        accessToken: null,
        refreshToken: null,
        expiresAt: null,
      },
      tasks: [],
    }),

  tasks: [],
  setTasks: (tasks) => set({ tasks }),
  addTask: (task) => set((s) => ({ tasks: [...s.tasks, task] })),
  updateTask: (id, updates) =>
    set((s) => ({
      tasks: s.tasks.map((t) =>
        t.id === id ? { ...t, ...updates, updatedAt: new Date().toISOString() } : t
      ),
    })),
  deleteTask: (id) =>
    set((s) => ({ tasks: s.tasks.filter((t) => t.id !== id) })),
  toggleTaskDone: (id) =>
    set((s) => ({
      tasks: s.tasks.map((t) =>
        t.id === id
          ? {
              ...t,
              status: t.status === "done" ? "pending" : "done",
              updatedAt: new Date().toISOString(),
            }
          : t
      ),
    })),

  categories: ["Finance", "Product", "Client", "Personal", "Work"],
  setCategories: (cats) => set({ categories: cats }),
  addCategory: (cat) =>
    set((s) => ({
      categories: s.categories.includes(cat)
        ? s.categories
        : [...s.categories, cat],
    })),

  sync: {
    lastSynced: null,
    isSyncing: false,
    syncError: null,
  },
  setSyncState: (s) =>
    set((state) => ({ sync: { ...state.sync, ...s } })),

  settings: defaultSettings,
  updateSettings: (s) =>
    set((state) => ({ settings: { ...state.settings, ...s } })),

  activeFilter: "all",
  setActiveFilter: (f) => set({ activeFilter: f }),

  getTodayTasks: () => {
    const today = new Date().toISOString().split("T")[0];
    return get().tasks.filter(
      (t) => t.scheduledDate === today && t.status === "pending"
    );
  },

  getPendingTasks: () =>
    get().tasks.filter((t) => t.status === "pending"),

  getTasksByDate: (date) =>
    get().tasks.filter((t) => t.scheduledDate === date),
}));

// Persist settings & tasks to Tauri store
export async function persistData(data: Partial<IdoData & { settings: AppSettings; auth: Partial<AuthState> }>) {
  try {
    const { Store } = await import("@tauri-apps/plugin-store");
    const store = await Store.load("ido-local.json");
    if (data.tasks !== undefined) await store.set("tasks", data.tasks);
    if (data.categories !== undefined) await store.set("categories", data.categories);
    if (data.settings !== undefined) await store.set("settings", data.settings);
    await store.save();
  } catch (e) {
    console.warn("persist error:", e);
  }
}

export async function loadPersistedData() {
  try {
    const { Store } = await import("@tauri-apps/plugin-store");
    const store = await Store.load("ido-local.json");
    const tasks = (await store.get<Task[]>("tasks")) ?? [];
    const categories = (await store.get<string[]>("categories")) ?? ["Finance", "Product", "Client", "Personal", "Work"];
    const settings = (await store.get<AppSettings>("settings")) ?? null;
    return { tasks, categories, settings };
  } catch (e) {
    console.warn("load error:", e);
    return { tasks: [], categories: [], settings: null };
  }
}
