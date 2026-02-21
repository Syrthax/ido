import React, { useState } from "react";
import { createPortal } from "react-dom";
import { motion } from "framer-motion";
import { Sun, Moon, Monitor, Shield, Code2, RefreshCw, Bell, X, ExternalLink } from "lucide-react";
import { useStore } from "../store/useStore";
import { useTheme } from "../hooks/useTheme";
import { useSync } from "../hooks/useSync";
import { invoke } from "@tauri-apps/api/core";
import { open } from "@tauri-apps/plugin-shell";

interface SettingsModalProps {
  onClose: () => void;
}

const SettingsModal: React.FC<SettingsModalProps> = ({ onClose }) => {
  const { auth, logout } = useStore();
  const { theme, setTheme } = useTheme();
  const { syncNow, isSyncing, lastSynced, syncError } = useSync();
  const [notifSent, setNotifSent] = useState(false);

  const D = (msg: string) => console.log("[IDO DEBUG] " + msg);

  const handleLogout = async () => {
    try { await invoke("clear_auth_tokens"); } catch {}
    logout();
    onClose();
  };

  const handleSyncNow = async () => {
    D("Manual sync triggered");
    D("Task count before sync: " + useStore.getState().tasks.length);
    await syncNow();
    D("Task count after sync: " + useStore.getState().tasks.length);
  };

  const handleTestNotification = async () => {
    try {
      await invoke("send_test_notification");
      setNotifSent(true);
      setTimeout(() => setNotifSent(false), 3000);
    } catch (e: any) {
      const msg = e?.toString() ?? "Unknown error";
      if (msg.includes("denied") || msg.includes("permission") || msg.includes("NotDetermined")) {
        alert("Notification permission denied.\n\nTo enable:\nSystem Settings -> Notifications -> iDo -> Allow Notifications");
      } else {
        console.warn("Notification error:", msg);
      }
    }
  };

  const user = auth.user;
  const lastSyncStr = lastSynced
    ? "Last sync: " + Math.round((Date.now() - new Date(lastSynced).getTime()) / 60000) + " mins ago"
    : "Never synced";

  const modal = (
    <motion.div
      key="settings-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
      style={{
        position: "fixed", inset: 0, zIndex: 200,
        display: "flex", alignItems: "center", justifyContent: "center",
        background: "rgba(0,0,0,0.55)",
        backdropFilter: "blur(6px)",
        WebkitBackdropFilter: "blur(6px)",
      }}
    >
      <motion.div
        key="settings-card"
        initial={{ opacity: 0, scale: 0.95, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 8 }}
        transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
        style={{
          width: "600px", maxHeight: "calc(100vh - 80px)", overflowY: "auto",
          background: "rgba(18,18,20,0.92)",
          backdropFilter: "blur(30px)", WebkitBackdropFilter: "blur(30px)",
          border: "1px solid rgba(255,255,255,0.09)", borderRadius: "18px",
          padding: "28px",
          boxShadow: "0 32px 80px rgba(0,0,0,0.6), 0 0 0 0.5px rgba(255,255,255,0.04)",
          display: "flex", flexDirection: "column", gap: "20px",
        }}
      >
        {/* ===== Modal Header ===== */}
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {/* Top row: profile left, close right */}
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
              <UserAvatar name={user?.name ?? "G"} url={user?.avatarUrl} size={42} />
              <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
                <span style={{ fontSize: "15px", fontWeight: 700, color: "var(--ido-text)", lineHeight: 1.2 }}>
                  {user?.name ?? "Guest"}
                </span>
                <span style={{ fontSize: "12px", color: "var(--ido-text-muted)", lineHeight: 1.2 }}>
                  {user?.email ?? "guest@local"}
                </span>
              </div>
            </div>
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={onClose}
              style={{
                width: "30px", height: "30px", borderRadius: "50%",
                display: "flex", alignItems: "center", justifyContent: "center",
                background: "rgba(255,255,255,0.06)",
                border: "1px solid rgba(255,255,255,0.08)",
                color: "var(--ido-text-muted)", cursor: "pointer", flexShrink: 0,
              }}
            >
              <X size={14} />
            </motion.button>
          </div>

          {/* Centered title */}
          <h1 style={{ textAlign: "center", fontSize: "20px", fontWeight: 700, color: "var(--ido-text)", letterSpacing: "-0.3px" }}>
            Settings
          </h1>

          {/* Divider */}
          <hr style={{ border: "none", borderTop: "1px solid rgba(255,255,255,0.07)", margin: 0 }} />
        </div>

        {/* ===== Appearance ===== */}
        <SectionCard title="Appearance" icon="🎨">
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "8px" }}>
            {(["light", "dark", "system"] as const).map((t) => (
              <motion.button
                key={t}
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.97 }}
                onClick={() => setTheme(t)}
                style={{
                  display: "flex", flexDirection: "column", alignItems: "center",
                  gap: "8px", padding: "16px 8px", borderRadius: "12px",
                  border: theme === t
                    ? "2px solid var(--ido-accent)"
                    : "1.5px solid rgba(255,255,255,0.07)",
                  background: theme === t ? "rgba(37,99,235,0.12)" : "rgba(255,255,255,0.03)",
                  color: theme === t ? "var(--ido-accent)" : "var(--ido-text-muted)",
                  cursor: "pointer", transition: "all 0.15s",
                  boxShadow: theme === t
                    ? "0 0 0 1px rgba(37,99,235,0.25), 0 2px 12px rgba(37,99,235,0.12)"
                    : "none",
                }}
              >
                {t === "light" ? <Sun size={20} /> : t === "dark" ? <Moon size={20} /> : <Monitor size={20} />}
                <span style={{ fontSize: "13px", fontWeight: 600, textTransform: "capitalize" }}>{t}</span>
              </motion.button>
            ))}
          </div>
        </SectionCard>

        {/* ===== Google Sync ===== */}
        {!auth.isGuest && auth.isAuthenticated && (
          <SectionCard title="Google Sync" icon="🔄">
            <div style={{ display: "flex", alignItems: "center", gap: "12px", paddingBottom: "12px" }}>
              <UserAvatar name={user?.name ?? "G"} url={user?.avatarUrl} size={44} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{
                  fontSize: "13px", fontWeight: 600, color: "var(--ido-text)",
                  overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
                }}>
                  {user?.email ?? ""}
                </p>
                <p style={{ fontSize: "11px", color: "var(--ido-text-muted)", marginTop: "3px" }}>
                  {lastSyncStr}
                </p>
              </div>
              <SyncBadge isSyncing={isSyncing} syncError={syncError} lastSynced={lastSynced} />
            </div>
            <motion.button
              whileHover={{ scale: 1.02, filter: "brightness(1.1)" }}
              whileTap={{ scale: 0.97 }}
              onClick={handleSyncNow}
              disabled={isSyncing}
              style={{
                display: "inline-flex", alignItems: "center", gap: "8px",
                padding: "10px 18px", borderRadius: "10px",
                background: "var(--ido-accent)", color: "white",
                fontSize: "13px", fontWeight: 600,
                cursor: isSyncing ? "default" : "pointer",
                opacity: isSyncing ? 0.7 : 1, border: "none",
                transition: "all 0.15s",
                boxShadow: "0 2px 12px rgba(37,99,235,0.28)",
              }}
            >
              <RefreshCw
                size={13}
                style={{ animation: isSyncing ? "spin 1s linear infinite" : "none" }}
              />
              {isSyncing ? "Syncing…" : "Sync Now"}
            </motion.button>
          </SectionCard>
        )}

        {/* ===== Notifications ===== */}
        <SectionCard title="Notifications" icon="🔔">
          <RowButton
            icon={<Bell size={14} />}
            label={notifSent ? "Notification sent!" : "Send test notification"}
            onClick={handleTestNotification}
            highlight={notifSent}
          />
        </SectionCard>

        {/* ===== About ===== */}
        <SectionCard title="About" icon="ℹ️">
          <RowButton
            icon={<Shield size={14} />}
            label="Privacy Policy"
            subtitle="Read our terms and data usage"
            onClick={() => open("https://sarthakg.tech/ido/privacy-policy.html")}
            external
          />
          <div style={{ height: "1px", background: "rgba(255,255,255,0.05)" }} />
          <RowButton
            icon={<Code2 size={14} />}
            label="GitHub Repository"
            subtitle="View source code and contribute"
            onClick={() => open("https://github.com/Syrthax/ido")}
            external
          />
        </SectionCard>

        {/* ===== Sign Out ===== */}
        {auth.isAuthenticated && (
          <motion.button
            whileHover={{ scale: 1.01, filter: "brightness(1.12)" }}
            whileTap={{ scale: 0.98 }}
            onClick={handleLogout}
            style={{
              width: "100%", padding: "13px", borderRadius: "12px",
              background: "rgba(239,68,68,0.1)", color: "#ef4444",
              border: "1px solid rgba(239,68,68,0.22)",
              fontSize: "14px", fontWeight: 600, cursor: "pointer",
              transition: "all 0.15s",
            }}
          >
            Sign Out
          </motion.button>
        )}

        {/* Version */}
        <p style={{ textAlign: "center", fontSize: "11px", color: "var(--ido-text-muted)" }}>
          iDo v2.4.0
        </p>
      </motion.div>
    </motion.div>
  );

  return createPortal(modal, document.body);
};

/* ================== Sub-components ================== */

const SectionCard: React.FC<{ title: string; icon: string; children: React.ReactNode }> = ({
  title, icon, children,
}) => (
  <div
    style={{
      borderRadius: "14px",
      background: "rgba(255,255,255,0.03)",
      border: "1px solid rgba(255,255,255,0.07)",
      padding: "14px 16px",
      display: "flex", flexDirection: "column", gap: "10px",
    }}
  >
    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
      <span style={{ fontSize: "14px" }}>{icon}</span>
      <span style={{
        fontSize: "11px", fontWeight: 700, color: "var(--ido-text-muted)",
        textTransform: "uppercase", letterSpacing: "0.7px",
      }}>
        {title}
      </span>
    </div>
    {children}
  </div>
);

const RowButton: React.FC<{
  icon: React.ReactNode;
  label: string;
  subtitle?: string;
  onClick: () => void;
  external?: boolean;
  highlight?: boolean;
}> = ({ icon, label, subtitle, onClick, external, highlight }) => (
  <motion.button
    whileHover={{ background: "rgba(255,255,255,0.05)" }}
    onClick={onClick}
    style={{
      width: "100%", display: "flex", alignItems: "center", gap: "12px",
      padding: "10px 8px", borderRadius: "10px", cursor: "pointer",
      textAlign: "left", border: "none", background: "transparent",
      transition: "background 0.15s",
    }}
  >
    <span style={{ color: "var(--ido-text-muted)", flexShrink: 0 }}>{icon}</span>
    <div style={{ flex: 1, minWidth: 0 }}>
      <p style={{ fontSize: "13px", fontWeight: 500, color: highlight ? "#22c55e" : "var(--ido-text)" }}>
        {label}
      </p>
      {subtitle && (
        <p style={{ fontSize: "11px", color: "var(--ido-text-muted)", marginTop: "2px" }}>{subtitle}</p>
      )}
    </div>
    {external && <ExternalLink size={13} style={{ color: "var(--ido-text-muted)", flexShrink: 0 }} />}
  </motion.button>
);

const SyncBadge: React.FC<{
  isSyncing: boolean;
  syncError: string | null;
  lastSynced: string | null;
}> = ({ isSyncing, syncError, lastSynced }) => {
  if (isSyncing) {
    return (
      <div style={{
        display: "flex", alignItems: "center", gap: "5px",
        padding: "4px 10px", borderRadius: "999px",
        background: "rgba(37,99,235,0.15)", color: "#60a5fa",
        fontSize: "11px", fontWeight: 600, flexShrink: 0,
      }}>
        <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "#60a5fa" }} />
        Syncing…
      </div>
    );
  }
  if (syncError) {
    return (
      <div title={syncError ?? ""} style={{
        display: "flex", alignItems: "center", gap: "5px",
        padding: "4px 10px", borderRadius: "999px",
        background: "rgba(239,68,68,0.12)", color: "#f87171",
        fontSize: "11px", fontWeight: 600, flexShrink: 0,
      }}>
        <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "#f87171" }} />
        Failed
      </div>
    );
  }
  if (lastSynced) {
    return (
      <div style={{
        display: "flex", alignItems: "center", gap: "5px",
        padding: "4px 10px", borderRadius: "999px",
        background: "rgba(34,197,94,0.12)", color: "#4ade80",
        fontSize: "11px", fontWeight: 600, flexShrink: 0,
      }}>
        <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "#4ade80" }} />
        Synced
      </div>
    );
  }
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: "5px",
      padding: "4px 10px", borderRadius: "999px",
      background: "rgba(107,114,128,0.12)", color: "#9ca3af",
      fontSize: "11px", fontWeight: 600, flexShrink: 0,
    }}>
      <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "#9ca3af" }} />
      Not synced
    </div>
  );
};

const UserAvatar: React.FC<{ name: string; url?: string; size?: number }> = ({
  name, url, size = 40,
}) => {
  const initials = name
    .split(" ")
    .map((w: string) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
  return (
    <div
      style={{
        width: size, height: size, borderRadius: "50%",
        overflow: "hidden", flexShrink: 0, display: "flex",
        alignItems: "center", justifyContent: "center",
        background: url ? "transparent" : "var(--ido-accent)",
        color: "white", fontSize: size * 0.36, fontWeight: 700,
        boxShadow: "0 0 0 2px rgba(255,255,255,0.08)",
      }}
    >
      {url
        ? <img src={url} alt={name} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        : initials}
    </div>
  );
};

export default SettingsModal;
