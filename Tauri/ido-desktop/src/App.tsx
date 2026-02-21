import React, { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useStore, loadPersistedData } from "./store/useStore";
import { useTheme } from "./hooks/useTheme";
import LoginPage from "./pages/LoginPage";
import TasksPage from "./pages/TasksPage";
import CalendarPage from "./pages/CalendarPage";
import Layout from "./components/Layout";

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useStore((s) => s.auth.isAuthenticated);
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

function AppTheme() {
  useTheme();
  return null;
}

function App() {
  const { setTasks, setCategories, updateSettings } = useStore();

  useEffect(() => {
    loadPersistedData().then((data) => {
      if (data.tasks.length > 0) setTasks(data.tasks);
      if (data.categories.length > 0) setCategories(data.categories);
      if (data.settings) updateSettings(data.settings);
    });
  }, []);

  return (
    <BrowserRouter>
      <AppTheme />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<RequireAuth><Layout /></RequireAuth>}>
          <Route index element={<Navigate to="/tasks" replace />} />
          <Route path="tasks" element={<TasksPage />} />
          <Route path="calendar" element={<CalendarPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
