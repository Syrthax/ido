import { useEffect, useCallback } from "react";
import { useStore } from "../store/useStore";
import { invoke } from "@tauri-apps/api/core";
import { normalizeIdoData, toIdoData } from "../utils/taskAdapter";

const D = (msg: string, ...args: unknown[]) =>
  console.log(`[IDO DEBUG] ${msg}`, ...args);
const E = (msg: string, ...args: unknown[]) =>
  console.error(`[IDO DEBUG] FAIL — ${msg}`, ...args);

export function useSync() {
  const setSyncState = useStore((s) => s.setSyncState);
  const setTasks     = useStore((s) => s.setTasks);
  const setCategories = useStore((s) => s.setCategories);
  const sync         = useStore((s) => s.sync);

  const uploadData = useCallback(async () => {
    const { auth, tasks, categories } = useStore.getState();
    if (!auth.isAuthenticated || auth.isGuest) return;
    D(`uploadData: uploading ${tasks.length} tasks to Drive`);
    try {
      const payload = toIdoData(tasks, categories);
      await invoke("drive_upload_data", {
        accessToken: auth.accessToken,
        data: payload,
      });
      D("uploadData: SUCCESS");
    } catch (e: any) {
      E("uploadData error:", e?.toString());
    }
  }, []);

  const syncNow = useCallback(async () => {
    const { auth, tasks } = useStore.getState();

    D("syncNow called");
    D(`Access token acquired: ${!!auth.accessToken ? "YES" : "NO"}`);
    D(`Access token length: ${auth.accessToken?.length ?? 0}`);

    if (!auth.isAuthenticated || auth.isGuest) {
      E("syncNow aborted — not authenticated or guest");
      return;
    }

    setSyncState({ isSyncing: true, syncError: null });

    try {
      // ── Step 1: Download remote JSON ──────────────────────────────────
      D("Drive file search: requesting ido-data.json...");
      let rawJson: string;
      try {
        rawJson = await invoke<string>("drive_download_data", {
          accessToken: auth.accessToken,
        });
        D(`Drive download: SUCCESS — ${rawJson.length} bytes`);
      } catch (invokeErr: any) {
        E("Drive download invoke failed:", invokeErr?.toString());
        setSyncState({ isSyncing: false, syncError: invokeErr?.toString() ?? "Drive download failed" });
        return;
      }

      // ── Step 2: Parse + normalize JSON ────────────────────────────────
      let rawData: Record<string, any>;
      try {
        rawData = JSON.parse(rawJson);
      } catch (parseErr: any) {
        E("JSON parse failed:", parseErr?.toString());
        E("Raw content (first 500):", rawJson.slice(0, 500));
        setSyncState({ isSyncing: false, syncError: "JSON parse error" });
        return;
      }

      // ── Step 3: Normalize schema (mobile ↔ desktop) ───────────────────
      const normalized = normalizeIdoData(rawData);
      D(`Remote JSON content (normalized): ${normalized.tasks.length} tasks`);
      console.log("[IDO DEBUG] Remote JSON content:", JSON.stringify(rawData, null, 2));

      D(`Remote tasks count: ${normalized.tasks.length}`);
      D(`Local tasks count (before merge): ${tasks.length}`);

      // ── Step 4: Merge ──────────────────────────────────────────────────
      const localUpdated = tasks.reduce((latest, t) => {
        const d = new Date(t.updatedAt).getTime();
        return d > latest ? d : latest;
      }, 0);
      const remoteUpdated = normalized.updatedAt
        ? new Date(normalized.updatedAt).getTime()
        : 0;

      D(`localUpdated epoch: ${localUpdated}  remoteUpdated epoch: ${remoteUpdated}`);
      D(`Decision: remote newer? ${remoteUpdated >= localUpdated}`);

      if (normalized.tasks.length > 0 && remoteUpdated >= localUpdated) {
        D(`Applying ${normalized.tasks.length} tasks from Drive to UI state`);
        setTasks(normalized.tasks);
        if (normalized.categories.length > 0) setCategories(normalized.categories);
        D(`UI state updated: SUCCESS — final task count: ${useStore.getState().tasks.length}`);
      } else if (tasks.length > 0) {
        D("Local is newer or remote empty — uploading local data");
        await uploadData();
      } else {
        D("Both sides empty — nothing to sync");
      }

      setSyncState({
        isSyncing: false,
        lastSynced: new Date().toISOString(),
        syncError: null,
      });
      D(`syncNow COMPLETE — final task count: ${useStore.getState().tasks.length}`);

    } catch (e: any) {
      E("syncNow unexpected error:", e?.toString());
      setSyncState({ isSyncing: false, syncError: e?.toString() ?? "Sync failed" });
    }
  }, [setSyncState, setTasks, setCategories, uploadData]);

  // ── Background auto-sync every 3 minutes ───────────────────────────────
  useEffect(() => {
    const { auth } = useStore.getState();
    if (!auth.isAuthenticated || auth.isGuest) return;
    D("useSync: setting up 3-minute auto-sync interval");
    const interval = setInterval(() => {
      D("Auto-sync interval fired");
      syncNow();
    }, 3 * 60 * 1000);
    return () => clearInterval(interval);
  }, [syncNow]);

  return {
    syncNow,
    uploadData,
    isSyncing: sync.isSyncing,
    lastSynced: sync.lastSynced,
    syncError: sync.syncError,
  };
}
