#!/usr/bin/env python3
import os

base = "/Users/sarthakghosh/projects/ido/Tauri/ido-desktop"

files = {
    f"{base}/src/App.css": "/* styles in index.css */\n",
}

for path, content in files.items():
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(content)
    print(f"Written: {path}")

# App.tsx
app_tsx = (
    'import React, { useEffect } from "react";\n'
    'import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";\n'
    'import { useStore, loadPersistedData } from "./store/useStore";\n'
    'import { useTheme } from "./hooks/useTheme";\n'
    'import LoginPage from "./pages/LoginPage";\n'
    'import TasksPage from "./pages/TasksPage";\n'
    'import CalendarPage from "./pages/CalendarPage";\n'
    'import SettingsPage from "./pages/SettingsPage";\n'
    'import Layout from "./components/Layout";\n'
    '\n'
    'function RequireAuth({ children }: { children: React.ReactNode }) {\n'
    '  const isAuthenticated = useStore((s) => s.auth.isAuthenticated);\n'
    '  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;\n'
    '}\n'
    '\n'
    'function AppTheme() {\n'
    '  useTheme();\n'
    '  return null;\n'
    '}\n'
    '\n'
    'function App() {\n'
    '  const { setTasks, setCategories, updateSettings } = useStore();\n'
    '\n'
    '  useEffect(() => {\n'
    '    loadPersistedData().then((data) => {\n'
    '      if (data.tasks.length > 0) setTasks(data.tasks);\n'
    '      if (data.categories.length > 0) setCategories(data.categories);\n'
    '      if (data.settings) updateSettings(data.settings);\n'
    '    });\n'
    '  }, []);\n'
    '\n'
    '  return (\n'
    '    <BrowserRouter>\n'
    '      <AppTheme />\n'
    '      <Routes>\n'
    '        <Route path="/login" element={<LoginPage />} />\n'
    '        <Route path="/" element={<RequireAuth><Layout /></RequireAuth>}>\n'
    '          <Route index element={<Navigate to="/tasks" replace />} />\n'
    '          <Route path="tasks" element={<TasksPage />} />\n'
    '          <Route path="calendar" element={<CalendarPage />} />\n'
    '          <Route path="settings" element={<SettingsPage />} />\n'
    '        </Route>\n'
    '        <Route path="*" element={<Navigate to="/login" replace />} />\n'
    '      </Routes>\n'
    '    </BrowserRouter>\n'
    '  );\n'
    '}\n'
    '\n'
    'export default App;\n'
)

with open(f"{base}/src/App.tsx", "w") as f:
    f.write(app_tsx)
print("Written App.tsx")

# index.css
index_css = (
    '@import "tailwindcss";\n'
    '\n'
    ':root {\n'
    '  --ido-bg: #0d1117;\n'
    '  --ido-surface: #161b22;\n'
    '  --ido-surface-2: #1e2530;\n'
    '  --ido-border: #30363d;\n'
    '  --ido-text: #e6edf3;\n'
    '  --ido-text-muted: #8b949e;\n'
    '  --ido-accent: #2563eb;\n'
    '  --ido-accent-hover: #1d4ed8;\n'
    '  --ido-priority-high: #ef4444;\n'
    '  --ido-priority-medium: #f97316;\n'
    '  --ido-priority-low: #22c55e;\n'
    '}\n'
    '\n'
    '[data-theme="light"] {\n'
    '  --ido-bg: #f4f6f8;\n'
    '  --ido-surface: #ffffff;\n'
    '  --ido-surface-2: #f0f2f5;\n'
    '  --ido-border: #d0d7de;\n'
    '  --ido-text: #1f2328;\n'
    '  --ido-text-muted: #656d76;\n'
    '  --ido-accent: #2563eb;\n'
    '  --ido-accent-hover: #1d4ed8;\n'
    '}\n'
    '\n'
    '* { box-sizing: border-box; margin: 0; padding: 0; }\n'
    '\n'
    'html, body, #root {\n'
    '  height: 100%;\n'
    '  width: 100%;\n'
    '  overflow: hidden;\n'
    '  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI", sans-serif;\n'
    '  background: var(--ido-bg);\n'
    '  color: var(--ido-text);\n'
    '  -webkit-font-smoothing: antialiased;\n'
    '}\n'
    '\n'
    '::-webkit-scrollbar { width: 6px; }\n'
    '::-webkit-scrollbar-track { background: transparent; }\n'
    '::-webkit-scrollbar-thumb { background: var(--ido-border); border-radius: 3px; }\n'
    '::selection { background: var(--ido-accent); color: white; }\n'
)

with open(f"{base}/src/index.css", "w") as f:
    f.write(index_css)
print("Written index.css")

print("ALL DONE")
