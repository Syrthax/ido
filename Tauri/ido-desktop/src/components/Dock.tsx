import React from "react";
import { motion } from "framer-motion";
import { useNavigate, useLocation } from "react-router-dom";
import { CheckSquare, Calendar, Plus } from "lucide-react";

interface DockProps {
  onCreateTask: () => void;
}

const Dock: React.FC<DockProps> = ({ onCreateTask }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const isActive = (path: string) => location.pathname === path;

  return (
    // Outer div handles centering via flexbox — avoids conflict with Framer Motion's transform
    <div
      style={{
        position: "fixed",
        bottom: "32px",
        left: 0,
        right: 0,
        display: "flex",
        justifyContent: "center",
        pointerEvents: "none",
        zIndex: 50,
      }}
    >
      <motion.div
        initial={{ y: 80, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 280, damping: 26, delay: 0.15 }}
        style={{
          pointerEvents: "auto",
          padding: "8px",
          borderRadius: "16px",
          background: "rgba(30,30,30,0.6)",
          backdropFilter: "blur(20px)",
          WebkitBackdropFilter: "blur(20px)",
          border: "1px solid rgba(255,255,255,0.06)",
          boxShadow: "0 8px 30px rgba(0,0,0,0.35)",
          display: "flex",
          alignItems: "center",
          gap: "8px",
        }}
      >
      {/* Tasks */}
      <NavButton
        icon={<CheckSquare size={20} strokeWidth={1.8} />}
        label="Tasks"
        active={isActive("/tasks")}
        onClick={() => navigate("/tasks")}
      />

      {/* Plus */}
      <motion.button
        whileHover={{ scale: 1.08 }}
        whileTap={{ scale: 0.92 }}
        onClick={onCreateTask}
        aria-label="Create task"
        style={{
          width: "56px",
          height: "56px",
          borderRadius: "14px",
          background: "linear-gradient(145deg, #3b82f6, #2563eb)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "white",
          flexShrink: 0,
          border: "none",
          cursor: "pointer",
          boxShadow: "0 6px 24px rgba(80,120,255,0.38), 0 2px 8px rgba(80,120,255,0.2)",
          transition: "box-shadow 0.18s cubic-bezier(0.4,0,0.2,1)",
        }}
      >
        <Plus size={24} strokeWidth={2.2} />
      </motion.button>

      {/* Calendar */}
      <NavButton
        icon={<Calendar size={20} strokeWidth={1.8} />}
        label="Calendar"
        active={isActive("/calendar")}
        onClick={() => navigate("/calendar")}
      />
      </motion.div>
    </div>
  );
};

const NavButton: React.FC<{
  icon: React.ReactNode;
  label: string;
  active: boolean;
  onClick: () => void;
}> = ({ icon, label, active, onClick }) => (
  <motion.button
    whileHover={{ scale: 1.05 }}
    whileTap={{ scale: 0.95 }}
    onClick={onClick}
    style={{
      width: "44px",
      height: "44px",
      borderRadius: "12px",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: "3px",
      background: active ? "rgba(80,120,255,0.15)" : "transparent",
      color: active ? "#6080ff" : "rgba(255,255,255,0.45)",
      border: "none",
      cursor: "pointer",
      flexShrink: 0,
      transition: "background 0.18s cubic-bezier(0.4,0,0.2,1), color 0.18s cubic-bezier(0.4,0,0.2,1)",
    }}
  >
    {icon}
    <span style={{ fontSize: "11px", fontWeight: 500, marginTop: "1px", lineHeight: 1 }}>{label}</span>
  </motion.button>
);

export default Dock;
