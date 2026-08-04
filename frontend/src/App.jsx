import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import EmployeeDashboard from "./pages/EmployeeDashboard";
import ManagerDashboard from "./pages/ManagerDashboard";
import HRDashboard from "./pages/HRDashboard";

function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/login" element={<Login />} />

        <Route
          path="/employee"
          element={<EmployeeDashboard />}
        />

        <Route
          path="/manager"
          element={<ManagerDashboard />}
        />

        <Route
          path="/hr"
          element={<HRDashboard />}
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