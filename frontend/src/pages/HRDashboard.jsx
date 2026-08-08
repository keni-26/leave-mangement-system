import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import api from "../services/api";

const AUTH_STORAGE_KEYS = ["token", "userId", "employeeId", "email", "role"];
const EMPTY_EMPLOYEE = { email: "", password: "", role: "EMPLOYEE", employeeCode: "", name: "", phone: "", department: "", designation: "", managerId: "" };
const EMPTY_LEAVE_TYPE = { name: "", description: "", allocatedDays: "", approvalRequired: true, active: true };
const EMPTY_HOLIDAY = { name: "", holidayDate: "", description: "" };

function responseMessage(error, fallback) {
  const data = error.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  return data?.message || data?.error || fallback;
}

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(`${value.slice(0, 10)}T00:00:00`);
  return Number.isNaN(date.getTime()) ? "-" : date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function HRDashboard() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = localStorage.getItem("email");
  const requestedSection = location.state?.hrSection;
  const [activeTab, setActiveTab] = useState(() => ["employees", "leave-types", "holidays"].includes(requestedSection) ? requestedSection : "employees");
  const [employees, setEmployees] = useState([]);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [holidays, setHolidays] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [employeeForm, setEmployeeForm] = useState(EMPTY_EMPLOYEE);
  const [employeeEditingId, setEmployeeEditingId] = useState(null);
  const [leaveTypeForm, setLeaveTypeForm] = useState(EMPTY_LEAVE_TYPE);
  const [leaveTypeEditingId, setLeaveTypeEditingId] = useState(null);
  const [holidayForm, setHolidayForm] = useState(EMPTY_HOLIDAY);
  const [holidayEditingId, setHolidayEditingId] = useState(null);

  const clearAuthentication = useCallback(() => {
    AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
    navigate("/login");
  }, [navigate]);

  const handleError = useCallback((requestError, fallback) => {
    const status = requestError.response?.status;
    if (status === 401) { clearAuthentication(); return; }
    if (status === 403) { setError("You are not authorized to perform this action"); return; }
    if (status === 404) { setError("Resource not found"); return; }
    if (status === 409) { setError(responseMessage(requestError, "This conflicts with an existing record.")); return; }
    setError(responseMessage(requestError, fallback));
  }, [clearAuthentication]);

  const loadData = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [employeeResponse, leaveTypeResponse, holidayResponse] = await Promise.all([api.get("/employees"), api.get("/leave-types"), api.get("/holidays")]);
      setEmployees(employeeResponse.data ?? []);
      setLeaveTypes(leaveTypeResponse.data ?? []);
      setHolidays(holidayResponse.data ?? []);
    } catch (requestError) {
      handleError(requestError, "We could not load the HR dashboard. Please try again.");
    } finally { setLoading(false); }
  }, [handleError]);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    if (loading || !requestedSection || requestedSection === "profile") return undefined;
    const tab = ["employees", "leave-types", "holidays"].includes(requestedSection) ? requestedSection : "employees";
    const timer = window.setTimeout(() => {
      setActiveTab(tab);
      window.requestAnimationFrame(() => document.getElementById(requestedSection)?.scrollIntoView({ behavior: "smooth", block: "start" }));
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loading, requestedSection]);

  const resetEmployeeForm = () => { setEmployeeForm(EMPTY_EMPLOYEE); setEmployeeEditingId(null); };
  const resetLeaveTypeForm = () => { setLeaveTypeForm(EMPTY_LEAVE_TYPE); setLeaveTypeEditingId(null); };
  const resetHolidayForm = () => { setHolidayForm(EMPTY_HOLIDAY); setHolidayEditingId(null); };

  const submitEmployee = async (event) => {
    event.preventDefault(); setError(""); setSuccess(""); setSubmitting(true);
    const payload = { ...employeeForm, managerId: employeeForm.managerId ? Number(employeeForm.managerId) : null };
    if (employeeEditingId) delete payload.email, delete payload.password, delete payload.role;
    try {
      if (employeeEditingId) await api.put(`/employees/${employeeEditingId}`, payload);
      else await api.post("/employees", payload);
      setSuccess(employeeEditingId ? "Employee updated successfully." : "Employee created successfully.");
      resetEmployeeForm(); await loadData();
    } catch (requestError) { handleError(requestError, "We could not save the employee. Please check the details and try again."); }
    finally { setSubmitting(false); }
  };

  const editEmployee = async (id) => {
    setError("");
    try {
      const { data } = await api.get(`/employees/${id}`);
      setEmployeeEditingId(id);
      setEmployeeForm({ email: data.email || "", password: "", role: data.role || "EMPLOYEE", employeeCode: data.employeeCode || "", name: data.name || "", phone: data.phone || "", department: data.department || "", designation: data.designation || "", managerId: data.manager?.id ? String(data.manager.id) : "" });
      setActiveTab("employees");
    } catch (requestError) { handleError(requestError, "We could not load that employee."); }
  };

  const changeEmployeeStatus = async (employee, enable) => {
    if (!enable && !window.confirm(`Disable ${employee.name}'s account?`)) return;
    setError(""); setSuccess(""); setSubmitting(true);
    try {
      await api.put(`/employees/${employee.id}/${enable ? "enable" : "disable"}`);
      setSuccess(`Employee account ${enable ? "enabled" : "disabled"} successfully.`); await loadData();
    } catch (requestError) { handleError(requestError, "We could not update the employee account."); }
    finally { setSubmitting(false); }
  };

  const submitLeaveType = async (event) => {
    event.preventDefault(); setError(""); setSuccess(""); setSubmitting(true);
    const payload = { ...leaveTypeForm, allocatedDays: Number(leaveTypeForm.allocatedDays), active: true };
    try {
      if (leaveTypeEditingId) await api.put(`/leave-types/${leaveTypeEditingId}`, payload);
      else await api.post("/leave-types", payload);
      setSuccess(leaveTypeEditingId ? "Leave type updated successfully." : "Leave type created successfully.");
      resetLeaveTypeForm(); await loadData();
    } catch (requestError) { handleError(requestError, "We could not save the leave type. Please check the details and try again."); }
    finally { setSubmitting(false); }
  };

  const editLeaveType = async (id) => {
    setError("");
    try {
      const { data } = await api.get(`/leave-types/${id}`);
      setLeaveTypeEditingId(id);
      setLeaveTypeForm({ name: data.name || "", description: data.description || "", allocatedDays: data.allocatedDays ?? "", approvalRequired: Boolean(data.approvalRequired), active: data.active !== false });
      setActiveTab("leave-types");
    } catch (requestError) { handleError(requestError, "We could not load that leave type."); }
  };

  const deactivateLeaveType = async (id) => {
    if (!window.confirm("Deactivate this leave type? Existing leave records will not be deleted.")) return;
    setError(""); setSuccess(""); setSubmitting(true);
    try { await api.delete(`/leave-types/${id}`); setSuccess("Leave type deactivated successfully."); await loadData(); }
    catch (requestError) { handleError(requestError, "We could not deactivate the leave type."); }
    finally { setSubmitting(false); }
  };

  const submitHoliday = async (event) => {
    event.preventDefault(); setError(""); setSuccess(""); setSubmitting(true);
    try {
      if (holidayEditingId) await api.put(`/holidays/${holidayEditingId}`, holidayForm);
      else await api.post("/holidays", holidayForm);
      setSuccess(holidayEditingId ? "Holiday updated successfully." : "Holiday created successfully.");
      resetHolidayForm(); await loadData();
    } catch (requestError) { handleError(requestError, "We could not save the holiday. Please check the details and try again."); }
    finally { setSubmitting(false); }
  };

  const editHoliday = async (id) => {
    setError("");
    try {
      const { data } = await api.get(`/holidays/${id}`);
      setHolidayEditingId(id); setHolidayForm({ name: data.name || "", holidayDate: data.holidayDate || "", description: data.description || "" }); setActiveTab("holidays");
    } catch (requestError) { handleError(requestError, "We could not load that holiday."); }
  };

  const deleteHoliday = async (holiday) => {
    if (!window.confirm(`Delete the ${holiday.name} holiday?`)) return;
    setError(""); setSuccess(""); setSubmitting(true);
    try { await api.delete(`/holidays/${holiday.id}`); setSuccess("Holiday deleted successfully."); await loadData(); }
    catch (requestError) { handleError(requestError, "We could not delete the holiday."); }
    finally { setSubmitting(false); }
  };

  if (loading) return <main className="dashboard-loading">Loading your HR dashboard...</main>;

  const activeEmployees = employees.filter((employee) => employee.enabled).length;
  const managers = employees.filter((employee) => employee.role === "MANAGER").length;
  return <main className="hr-dashboard">
    <header className="dashboard-header"><div><h1>HR Dashboard</h1><p>{email || "Signed-in HR user"}</p></div><button type="button" className="secondary-button" onClick={clearAuthentication}>Logout</button></header>
    {error && <p className="dashboard-message error-message" role="alert">{error}</p>}
    {success && <p className="dashboard-message success-message" role="status">{success}</p>}
    <section id="dashboard" className="balance-grid" aria-label="HR summary"><article className="balance-card"><h3>Total employees</h3><dl><div><dt>All records</dt><dd>{employees.length}</dd></div></dl></article><article className="balance-card"><h3>Active employees</h3><dl><div><dt>Enabled accounts</dt><dd>{activeEmployees}</dd></div></dl></article><article className="balance-card"><h3>Managers</h3><dl><div><dt>Team leads</dt><dd>{managers}</dd></div></dl></article></section>
    <nav className="dashboard-tabs" aria-label="HR management sections"><button className={activeTab === "employees" ? "active-tab" : "secondary-button"} onClick={() => setActiveTab("employees")}>Employees</button><button className={activeTab === "leave-types" ? "active-tab" : "secondary-button"} onClick={() => setActiveTab("leave-types")}>Leave Types</button><button className={activeTab === "holidays" ? "active-tab" : "secondary-button"} onClick={() => setActiveTab("holidays")}>Holidays</button></nav>

    {activeTab === "employees" && <section id="employees" className="dashboard-section"><h2>{employeeEditingId ? "Edit Employee" : "Add Employee"}</h2><form className="admin-form" onSubmit={submitEmployee}>
      {!employeeEditingId && <><label>Email<input type="email" value={employeeForm.email} onChange={(e) => setEmployeeForm({ ...employeeForm, email: e.target.value })} required /></label><label>Password<input type="password" value={employeeForm.password} onChange={(e) => setEmployeeForm({ ...employeeForm, password: e.target.value })} required /></label><label>Role<select value={employeeForm.role} onChange={(e) => setEmployeeForm({ ...employeeForm, role: e.target.value })}><option value="EMPLOYEE">Employee</option><option value="MANAGER">Manager</option><option value="HR">HR</option></select></label></>}
      <label>Employee code<input value={employeeForm.employeeCode} onChange={(e) => setEmployeeForm({ ...employeeForm, employeeCode: e.target.value })} required /></label><label>Name<input value={employeeForm.name} onChange={(e) => setEmployeeForm({ ...employeeForm, name: e.target.value })} required /></label><label>Phone<input type="tel" value={employeeForm.phone} onChange={(e) => setEmployeeForm({ ...employeeForm, phone: e.target.value })} /></label><label>Department<input value={employeeForm.department} onChange={(e) => setEmployeeForm({ ...employeeForm, department: e.target.value })} required /></label><label>Designation<input value={employeeForm.designation} onChange={(e) => setEmployeeForm({ ...employeeForm, designation: e.target.value })} required /></label><label>Manager <span>(optional)</span><select value={employeeForm.managerId} onChange={(e) => setEmployeeForm({ ...employeeForm, managerId: e.target.value })}><option value="">No manager assigned</option>{employees.filter((employee) => employee.role === "MANAGER" && employee.id !== employeeEditingId).map((manager) => <option key={manager.id} value={manager.id}>{manager.name} ({manager.employeeCode})</option>)}</select></label><div className="form-actions"><button type="submit" disabled={submitting}>{submitting ? "Saving..." : employeeEditingId ? "Update Employee" : "Create Employee"}</button>{employeeEditingId && <button type="button" className="secondary-button" onClick={resetEmployeeForm}>Cancel edit</button>}</div></form>
      <h2 className="admin-list-heading">Employees</h2><div className="table-wrapper"><table><thead><tr><th>Code</th><th>Name</th><th>Email</th><th>Phone</th><th>Department</th><th>Designation</th><th>Manager</th><th>Status</th><th>Actions</th></tr></thead><tbody>{employees.map((employee) => <tr key={employee.id}><td>{employee.employeeCode}</td><td>{employee.name}</td><td>{employee.email}</td><td>{employee.phone || "-"}</td><td>{employee.department}</td><td>{employee.designation}</td><td>{employee.manager ? `${employee.manager.name} (${employee.manager.employeeCode})` : "-"}</td><td><span className={`status ${employee.enabled ? "status-approved" : "status-rejected"}`}>{employee.enabled ? "Enabled" : "Disabled"}</span></td><td><div className="request-actions"><button type="button" className="secondary-button" onClick={() => editEmployee(employee.id)}>Edit</button><button type="button" className={employee.enabled ? "danger-button" : "secondary-button"} disabled={submitting} onClick={() => changeEmployeeStatus(employee, !employee.enabled)}>{employee.enabled ? "Disable" : "Enable"}</button></div></td></tr>)}</tbody></table></div></section>}

    {activeTab === "leave-types" && <section id="leave-types" className="dashboard-section"><h2>{leaveTypeEditingId ? "Edit Leave Type" : "Add Leave Type"}</h2><form className="admin-form" onSubmit={submitLeaveType}><label>Name<input value={leaveTypeForm.name} onChange={(e) => setLeaveTypeForm({ ...leaveTypeForm, name: e.target.value })} required /></label><label>Allocated days<input type="number" min="0" step="0.5" value={leaveTypeForm.allocatedDays} onChange={(e) => setLeaveTypeForm({ ...leaveTypeForm, allocatedDays: e.target.value })} required /></label><label className="checkbox-label"><input type="checkbox" checked={leaveTypeForm.approvalRequired} onChange={(e) => setLeaveTypeForm({ ...leaveTypeForm, approvalRequired: e.target.checked })} /> Approval required</label><label className="form-full-width">Description <span>(optional)</span><textarea rows="3" value={leaveTypeForm.description} onChange={(e) => setLeaveTypeForm({ ...leaveTypeForm, description: e.target.value })} /></label><div className="form-actions"><button type="submit" disabled={submitting}>{submitting ? "Saving..." : leaveTypeEditingId ? "Update Leave Type" : "Create Leave Type"}</button>{leaveTypeEditingId && <button type="button" className="secondary-button" onClick={resetLeaveTypeForm}>Cancel edit</button>}</div></form>
      <h2 className="admin-list-heading">Active Leave Types</h2><p className="helper-text">The backend lists active leave types only. Deactivated types cannot be listed or reactivated here without an existing all-types endpoint.</p><div className="table-wrapper"><table><thead><tr><th>Name</th><th>Description</th><th>Allocated days</th><th>Approval required</th><th>Active</th><th>Actions</th></tr></thead><tbody>{leaveTypes.map((type) => <tr key={type.id}><td>{type.name}</td><td>{type.description || "-"}</td><td>{type.allocatedDays}</td><td>{type.approvalRequired ? "Yes" : "No"}</td><td><span className="status status-approved">Active</span></td><td><div className="request-actions"><button type="button" className="secondary-button" onClick={() => editLeaveType(type.id)}>Edit</button><button type="button" className="danger-button" disabled={submitting} onClick={() => deactivateLeaveType(type.id)}>Deactivate</button></div></td></tr>)}</tbody></table></div></section>}

    {activeTab === "holidays" && <section id="holidays" className="dashboard-section"><h2>{holidayEditingId ? "Edit Holiday" : "Add Holiday"}</h2><form className="admin-form" onSubmit={submitHoliday}><label>Name<input value={holidayForm.name} onChange={(e) => setHolidayForm({ ...holidayForm, name: e.target.value })} required /></label><label>Holiday date<input type="date" value={holidayForm.holidayDate} onChange={(e) => setHolidayForm({ ...holidayForm, holidayDate: e.target.value })} required /></label><label className="form-full-width">Description <span>(optional)</span><textarea rows="3" value={holidayForm.description} onChange={(e) => setHolidayForm({ ...holidayForm, description: e.target.value })} /></label><div className="form-actions"><button type="submit" disabled={submitting}>{submitting ? "Saving..." : holidayEditingId ? "Update Holiday" : "Create Holiday"}</button>{holidayEditingId && <button type="button" className="secondary-button" onClick={resetHolidayForm}>Cancel edit</button>}</div></form>
      <h2 className="admin-list-heading">Holidays</h2><div className="table-wrapper"><table><thead><tr><th>Name</th><th>Date</th><th>Description</th><th>Actions</th></tr></thead><tbody>{holidays.map((holiday) => <tr key={holiday.id}><td>{holiday.name}</td><td>{formatDate(holiday.holidayDate)}</td><td>{holiday.description || "-"}</td><td><div className="request-actions"><button type="button" className="secondary-button" onClick={() => editHoliday(holiday.id)}>Edit</button><button type="button" className="danger-button" disabled={submitting} onClick={() => deleteHoliday(holiday)}>Delete</button></div></td></tr>)}</tbody></table></div></section>}
  </main>;
}

export default HRDashboard;
