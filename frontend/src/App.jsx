import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import EmployeeDashboard from "./pages/EmployeeDashboard";
import ManagerDashboard from "./pages/ManagerDashboard";
import HRDashboard from "./pages/HRDashboard";
import AppShell from "./components/AppShell";

function RoleRoute({ role, children }) {
  const token = localStorage.getItem("token");
  const userRole = localStorage.getItem("role");
  if (!token) return <Navigate to="/login" replace />;
  if (userRole !== role) return <Navigate to={`/${String(userRole || "").toLowerCase()}`} replace />;
  return <AppShell title={`${role.charAt(0)}${role.slice(1).toLowerCase()} workspace`}>{children}</AppShell>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/login" element={<Login />} />

        <Route
          path="/employee"
          element={<RoleRoute role="EMPLOYEE"><EmployeeDashboard /></RoleRoute>}
        />

        <Route
          path="/manager"
          element={<RoleRoute role="MANAGER"><ManagerDashboard /></RoleRoute>}
        />

        <Route
          path="/hr"
          element={<RoleRoute role="HR"><HRDashboard /></RoleRoute>}
        />

        <Route
          path="*"
          element={<Navigate to="/login" />}
        />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
