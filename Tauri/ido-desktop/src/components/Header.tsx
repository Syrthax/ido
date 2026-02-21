import React from "react";
import { motion } from "framer-motion";
import { Settings } from "lucide-react";
import { useStore } from "../store/useStore";
import { format } from "date-fns";

interface HeaderProps {
  onSettingsClick: () => void;
}

const Header: React.FC<HeaderProps> = ({ onSettingsClick }) => {
  const { auth } = useStore();
  const now = new Date();
  const dateStr = format(now, "EEEE, MMMM d — h:mm a");
  const user = auth.user;

  return (
    // Single DOM root avoids React Fragment ref warnings.
    // pointer-events: none so the overlay doesn't block page interactions.
    <div style={{ position: "fixed", inset: 0, pointerEvents: "none", zIndex: 40 }}>
      {/* User card — fixed top-left */}
      <motion.button
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        onClick={onSettingsClick}
        style={{
          position: "fixed",
          top: "20px",
          left: "20px",
          zIndex: 40,
          pointerEvents: "auto",
          display: "flex",
          alignItems: "center",
          gap: "8px",
          padding: "8px 12px",
          borderRadius: "12px",
          background: "transparent",
          border: "none",
          cursor: "pointer",
          transition: "background 150ms ease",
        }}
        onMouseEnter={(e) => (e.currentTarget.style.background = "rgba(255,255,255,0.05)")}
        onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
      >
        <Avatar name={user?.name ?? "G"} url={user?.avatarUrl} />
        <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-start", gap: "2px" }}>
          <span style={{ fontSize: "13px", fontWeight: 600, color: "var(--ido-text)", lineHeight: 1 }}>
            {user?.name ?? "Guest"}
          </span>
          <span style={{ fontSize: "11px", color: "var(--ido-text-muted)", lineHeight: 1 }}>
            {user?.email ?? "guest@local"}
          </span>
        </div>
      </motion.button>

      {/* Date — centered at top */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
        style={{
          position: "fixed",
          top: "0",
          left: "50%",
          transform: "translateX(-50%)",
          zIndex: 40,
          padding: "22px 0 0",
          pointerEvents: "none",
        }}
      >
        <span style={{ fontSize: "13px", fontWeight: 500, color: "var(--ido-text-muted)", whiteSpace: "nowrap" }}>
          {dateStr}
        </span>
      </motion.div>

      {/* Settings icon — fixed top-right */}
      <motion.button
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
        whileHover={{ rotate: 20 }}
        whileTap={{ scale: 0.9 }}
        onClick={onSettingsClick}
        aria-label="Settings"
        style={{
          position: "fixed",
          top: "20px",
          right: "20px",
          zIndex: 40,
          pointerEvents: "auto",
          width: "34px",
          height: "34px",
          borderRadius: "10px",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "transparent",
          border: "none",
          cursor: "pointer",
          color: "var(--ido-text-muted)",
          transition: "background 150ms ease",
        }}
        onMouseEnter={(e) => (e.currentTarget.style.background = "rgba(255,255,255,0.08)")}
        onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
      >
        <Settings size={18} />
      </motion.button>
    </div>
  );
};

const Avatar: React.FC<{ name: string; url?: string }> = ({ name, url }) => {
  const initials = name
    .split(" ")
    .map((w) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <div
      style={{
        width: "32px",
        height: "32px",
        borderRadius: "50%",
        overflow: "hidden",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: "12px",
        fontWeight: 700,
        flexShrink: 0,
        userSelect: "none",
        background: url ? "transparent" : "var(--ido-accent)",
        color: "white",
      }}
    >
      {url ? (
        <img src={url} alt={name} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
      ) : (
        initials
      )}
    </div>
  );
};

export default Header;
