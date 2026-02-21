import React, { useState } from "react";
import { motion } from "framer-motion";
import { invoke } from "@tauri-apps/api/core";
import { useStore, loadPersistedData } from "../store/useStore";
import { useNavigate } from "react-router-dom";
import { normalizeIdoData, toIdoData } from "../utils/taskAdapter";

const D = (msg: string, ...args: unknown[]) =>
  console.log(`[IDO DEBUG] ${msg}`, ...args);
const E = (msg: string, ...args: unknown[]) =>
  console.error(`[IDO DEBUG] FAIL — ${msg}`, ...args);

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState<null | "google" | "guest">(null);
  const [loginError, setLoginError] = useState<string | null>(null);
  const { setAuth, setTasks, setCategories, updateSettings } = useStore();
  const navigate = useNavigate();

  const handleGoogleLogin = async () => {
    setLoading("google");
    setLoginError(null);
    try {
      D("Starting OAuth flow...");
      const result = await invoke<{
        access_token: string;
        refresh_token: string;
        expires_in: number;
        user_id: string;
        user_name: string;
        user_email: string;
        avatar_url: string;
      }>("start_oauth_flow");

      D(`OAuth success — user: ${result.user_email}`);
      D(`Access token acquired: YES, length: ${result.access_token.length}`);

      // 1. Set auth state
      setAuth({
        isAuthenticated: true,
        isGuest: false,
        accessToken: result.access_token,
        refreshToken: result.refresh_token,
        expiresAt: Date.now() + result.expires_in * 1000,
        user: {
          id: result.user_id,
          name: result.user_name,
          email: result.user_email,
          avatarUrl: result.avatar_url,
        },
      });

      // 2. Load local persisted data as fallback
      D("Loading local persisted data...");
      const persisted = await loadPersistedData();
      D(`Local tasks count: ${persisted.tasks.length}`);
      if (persisted.tasks.length > 0) setTasks(persisted.tasks);
      if (persisted.categories.length > 0) setCategories(persisted.categories);
      if (persisted.settings) updateSettings(persisted.settings);

      // 3. INITIAL DRIVE SYNC — pull remote data immediately on login
      D("Starting initial Drive sync...");
      try {
        const rawJson = await invoke<string>("drive_download_data", {
          accessToken: result.access_token,
        });
        D(`Drive download: SUCCESS — ${rawJson.length} bytes`);

        let rawData: Record<string, any>;
        try {
          rawData = JSON.parse(rawJson);
        } catch {
          throw new Error("Drive JSON parse failed: " + rawJson.slice(0, 200));
        }

        const normalized = normalizeIdoData(rawData);
        D(`Remote JSON parsed — raw task count: ${rawData.tasks?.length ?? 0}, after normalization: ${normalized.tasks.length}`);
        console.log("[IDO DEBUG] Remote JSON content:", JSON.stringify(rawData, null, 2));

        if (normalized.tasks.length > 0) {
          D(`Applying ${normalized.tasks.length} tasks from Drive to UI state`);
          setTasks(normalized.tasks);
          if (normalized.categories.length > 0) setCategories(normalized.categories);
          D(`UI state updated: SUCCESS`);
          D(`Task count in store now: ${useStore.getState().tasks.length}`);
        } else {
          D(`Drive file empty or all tasks deleted — checking local state`);
          const localTasks = useStore.getState().tasks;
          if (localTasks.length > 0) {
            D(`Uploading ${localTasks.length} local tasks to Drive...`);
            await invoke("drive_upload_data", {
              accessToken: result.access_token,
              data: toIdoData(localTasks, useStore.getState().categories),
            });
            D("Upload complete");
          }
        }

        useStore.getState().setSyncState({
          lastSynced: new Date().toISOString(),
          isSyncing: false,
          syncError: null,
        });
        D(`Initial sync COMPLETE — final task count: ${useStore.getState().tasks.length}`);

      } catch (syncErr: any) {
        E(`Initial Drive sync error: ${syncErr?.toString()}`);
        useStore.getState().setSyncState({
          isSyncing: false,
          syncError: syncErr?.toString() ?? "Initial sync failed",
        });
      }

      navigate("/tasks");
    } catch (e: any) {
      E(`Login failed: ${e?.toString()}`);
      setLoginError(e?.toString() ?? "Login failed");
    } finally {
      setLoading(null);
    }
  };

  const handleGuestLogin = async () => {
    setLoading("guest");
    setLoginError(null);
    try {
      const persisted = await loadPersistedData();
      if (persisted.tasks.length > 0) setTasks(persisted.tasks);
      if (persisted.categories.length > 0) setCategories(persisted.categories);
      if (persisted.settings) updateSettings(persisted.settings);
      setAuth({
        isAuthenticated: true,
        isGuest: true,
        user: { id: "guest", name: "Guest", email: "guest@local" },
        accessToken: null,
        refreshToken: null,
        expiresAt: null,
      });
      navigate("/tasks");
    } finally {
      setLoading(null);
    }
  };

  return (
    <div
      className="w-full h-full flex items-center justify-center"
      style={{ background: "var(--ido-bg)" }}
    >
      <div className="flex flex-row items-center gap-24 max-w-4xl w-full px-12">
        {/* Left: Branding */}
        <motion.div
          initial={{ opacity: 0, x: -30 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
          className="flex flex-col gap-4 flex-1"
        >
          <h1 className="text-5xl font-bold tracking-tight" style={{ color: "var(--ido-text)" }}>
            Welcome to IDo
          </h1>
          <p className="text-base" style={{ color: "var(--ido-text-muted)" }}>
            iDo is an open source ToDo list app made by Sarthak and KrispLabs
          </p>
        </motion.div>

        {/* Right: Buttons */}
        <motion.div
          initial={{ opacity: 0, x: 30 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, ease: "easeOut", delay: 0.1 }}
          className="flex flex-col gap-3 w-64"
        >
          {loginError && (
            <div
              className="p-3 rounded-xl text-sm"
              style={{
                background: "rgba(239,68,68,0.1)",
                color: "#ef4444",
                border: "1px solid rgba(239,68,68,0.2)",
              }}
            >
              {loginError}
            </div>
          )}

          {/* Google login */}
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.97 }}
            onClick={handleGoogleLogin}
            disabled={loading !== null}
            className="flex items-center gap-3 px-5 py-3.5 rounded-xl text-sm font-semibold transition-all disabled:opacity-60"
            style={{
              background: "var(--ido-surface)",
              color: "var(--ido-text)",
              border: "1px solid var(--ido-border)",
            }}
          >
            {loading === "google" ? (
              <>
                <span className="w-5 h-5 rounded-full border-2 border-current border-t-transparent animate-spin" />
                Syncing from Drive...
              </>
            ) : (
              <>
                <svg width="18" height="18" viewBox="0 0 48 48" style={{ flexShrink: 0 }}>
                  <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                  <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                  <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                  <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                </svg>
                Login with Google
              </>
            )}
          </motion.button>

          {/* Guest login */}
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.97 }}
            onClick={handleGuestLogin}
            disabled={loading !== null}
            className="flex items-center justify-center gap-2 px-5 py-3.5 rounded-xl text-sm font-medium transition-all disabled:opacity-60"
            style={{
              background: "transparent",
              color: "var(--ido-text-muted)",
              border: "1px solid var(--ido-border)",
            }}
          >
            {loading === "guest" ? (
              <span className="w-4 h-4 rounded-full border-2 border-current border-t-transparent animate-spin" />
            ) : (
              "Use as guest"
            )}
          </motion.button>
        </motion.div>
      </div>
    </div>
  );
};

export default LoginPage;
