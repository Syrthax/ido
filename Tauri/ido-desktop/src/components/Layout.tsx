import React, { useState } from "react";
import { Outlet } from "react-router-dom";
import { AnimatePresence } from "framer-motion";
import Header from "./Header";
import Dock from "./Dock";
import CreateTaskModal from "./CreateTaskModal";
import SettingsModal from "../pages/SettingsPage";
import { useTheme } from "../hooks/useTheme";
import { useSync } from "../hooks/useSync";

const Layout: React.FC = () => {
  useTheme();
  useSync(); // Bug fix: wire background 3-min auto-sync and expose syncNow globally
  const [createOpen, setCreateOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);

  return (
    <div
      className="flex flex-col w-full h-full overflow-hidden"
      style={{ background: "var(--ido-bg)" }}
    >
      {/* Fixed header elements — user card (top-left), date (top-center), settings (top-right) */}
      <Header onSettingsClick={() => setSettingsOpen(true)} />

      {/* Page content — padded top to clear fixed header zone (20px top + element height + gap) */}
      <div className="flex-1 overflow-hidden relative" style={{ paddingTop: "64px" }}>
        <AnimatePresence mode="wait">
          <Outlet />
        </AnimatePresence>
      </div>

      <Dock onCreateTask={() => setCreateOpen(true)} />

      <CreateTaskModal open={createOpen} onClose={() => setCreateOpen(false)} />

      <AnimatePresence>
        {settingsOpen && <SettingsModal onClose={() => setSettingsOpen(false)} />}
      </AnimatePresence>
    </div>
  );
};

export default Layout;
