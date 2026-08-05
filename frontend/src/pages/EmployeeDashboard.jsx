import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

const AUTH_STORAGE_KEYS = ["token", "userId", "employeeId", "email", "role"];

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  return Number.isNaN(date.getTime()) ? "-" : date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function getErrorMessage(requestError, fallbackMessage) {
  const data = requestError.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  return data?.message || data?.error || fallbackMessage;
}

function EmployeeDashboard() {
  const navigate = useNavigate();
  const email = localStorage.getItem("email");
  const employeeId = localStorage.getItem("employeeId");
  const userId = localStorage.getItem("userId");
  const [balances, setBalances] = useState([]);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [leaveRequests, setLeaveRequests] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [holidays, setHolidays] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [updatingId, setUpdatingId] = useState(null);
  const [form, setForm] = useState({ leaveTypeId: "", startDate: "", endDate: "", reason: "" });

  const clearAuthentication = useCallback(() => {
    AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
    navigate("/login");
  }, [navigate]);

  const handleRequestError = useCallback((requestError, fallbackMessage) => {
    if (requestError.response?.status === 401) {
      clearAuthentication();
      return true;
    }
    setError(getErrorMessage(requestError, fallbackMessage));
    return false;
  }, [clearAuthentication]);

  const loadDashboardData = useCallback(async () => {
    if (!employeeId || !userId) {
      setError("Your account is missing employee information. Please sign in again.");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    try {
      const [balancesResponse, leaveTypesResponse, requestsResponse, notificationsResponse, holidaysResponse] = await Promise.all([
        api.get(`/leave-balances/employee/${employeeId}`),
        api.get("/leave-types"),
        api.get(`/leave-requests/employee/${employeeId}`),
        api.get(`/notifications/user/${userId}`),
        api.get("/holidays"),
      ]);
      setBalances(balancesResponse.data ?? []);
      setLeaveTypes(leaveTypesResponse.data ?? []);
      setLeaveRequests(requestsResponse.data ?? []);
      setNotifications(notificationsResponse.data ?? []);
      setHolidays(holidaysResponse.data ?? []);
    } catch (requestError) {
      handleRequestError(requestError, "We could not load your dashboard. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [employeeId, handleRequestError, userId]);

  useEffect(() => { loadDashboardData(); }, [loadDashboardData]);

  const handleFormChange = (event) => {
    const { name, value } = event.target;
    setForm((currentForm) => ({ ...currentForm, [name]: value }));
  };

  const handleApplyLeave = async (event) => {
    event.preventDefault();
    setSuccess("");
    setError("");
    setSubmitting(true);
    try {
      await api.post("/leave-requests", { employeeId: Number(employeeId), leaveTypeId: Number(form.leaveTypeId), startDate: form.startDate, endDate: form.endDate, reason: form.reason.trim() });
      setForm({ leaveTypeId: "", startDate: "", endDate: "", reason: "" });
      setSuccess("Your leave request has been submitted.");
      await loadDashboardData();
    } catch (requestError) {
      handleRequestError(requestError, "We could not submit your leave request. Please check the details and try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelLeave = async (requestId) => {
    setSuccess("");
    setError("");
    setUpdatingId(`leave-${requestId}`);
    try {
      await api.put(`/leave-requests/${requestId}/cancel`);
      setSuccess("The leave request has been cancelled.");
      await loadDashboardData();
    } catch (requestError) {
      handleRequestError(requestError, "We could not cancel that leave request. Please try again.");
    } finally {
      setUpdatingId(null);
    }
  };

  const handleMarkAsRead = async (notificationId) => {
    setError("");
    setUpdatingId(`notification-${notificationId}`);
    try {
      await api.put(`/notifications/${notificationId}/read`);
      setNotifications((items) => items.map((item) => (item.id === notificationId ? { ...item, isRead: true } : item)));
    } catch (requestError) {
      handleRequestError(requestError, "We could not update the notification. Please try again.");
    } finally {
      setUpdatingId(null);
    }
  };

  const handleMarkAllAsRead = async () => {
    setError("");
    setUpdatingId("notifications-all");
    try {
      await api.put(`/notifications/user/${userId}/read-all`);
      setNotifications((items) => items.map((item) => ({ ...item, isRead: true })));
    } catch (requestError) {
      handleRequestError(requestError, "We could not mark all notifications as read. Please try again.");
    } finally {
      setUpdatingId(null);
    }
  };

  const upcomingHolidays = holidays.filter((holiday) => holiday.holidayDate >= new Date().toISOString().slice(0, 10));
  const unreadNotifications = notifications.some((notification) => !notification.isRead);

  if (loading) return <main className="dashboard-loading">Loading your employee dashboard...</main>;

  return (
    <main className="employee-dashboard">
      <header id="dashboard" className="dashboard-header"><div><p className="eyebrow">Personal workspace</p><h1>Welcome, {(email || "Employee").split("@")[0]}</h1><p>Review your balance, submit leave, and track requests.</p></div><button type="button" className="secondary-button" onClick={clearAuthentication}>Logout</button></header>
      {error && <p className="dashboard-message error-message" role="alert">{error}</p>}
      {success && <p className="dashboard-message success-message" role="status">{success}</p>}

      <section id="balance" className="dashboard-section"><div className="section-heading-row"><h2>Leave Balance</h2><a className="secondary-action" href="#apply-leave">Apply leave</a></div>{balances.length === 0 ? <p className="empty-state">No leave balances are available yet.</p> : <div className="balance-grid">{balances.map((balance) => <article className="balance-card" key={balance.id}><h3>{balance.leaveTypeName || "Leave Type"}</h3><dl><div><dt>Allocated</dt><dd>{balance.allocatedDays ?? 0}</dd></div><div><dt>Used</dt><dd>{balance.usedDays ?? 0}</dd></div><div><dt>Remaining</dt><dd>{balance.remainingDays ?? 0}</dd></div></dl></article>)}</div>}</section>

      <section id="apply-leave" className="dashboard-section"><h2>Apply for Leave</h2><form className="leave-form" onSubmit={handleApplyLeave}><label>Leave type<select name="leaveTypeId" value={form.leaveTypeId} onChange={handleFormChange} required><option value="">Select a leave type</option>{leaveTypes.map((leaveType) => <option key={leaveType.id} value={leaveType.id}>{leaveType.name}</option>)}</select></label><label>Start date<input type="date" name="startDate" min={new Date().toISOString().slice(0, 10)} value={form.startDate} onChange={handleFormChange} required /></label><label>End date<input type="date" name="endDate" min={form.startDate || new Date().toISOString().slice(0, 10)} value={form.endDate} onChange={handleFormChange} required /></label><label className="form-full-width">Reason<textarea name="reason" value={form.reason} onChange={handleFormChange} rows="4" maxLength="2000" placeholder="Briefly describe the reason for your leave" required /></label><button type="submit" disabled={submitting}>{submitting ? "Submitting..." : "Submit Leave Request"}</button></form></section>

      <section id="requests" className="dashboard-section"><h2>My Leave Requests</h2>{leaveRequests.length === 0 ? <p className="empty-state">You have not submitted any leave requests.</p> : <div className="table-wrapper"><table><thead><tr><th>Leave type</th><th>Dates</th><th>Days</th><th>Reason</th><th>Status</th><th>Created</th><th>Action</th></tr></thead><tbody>{leaveRequests.map((request) => <tr key={request.id}><td>{request.leaveTypeName || "-"}</td><td>{formatDate(request.startDate)} – {formatDate(request.endDate)}</td><td>{request.leaveDays ?? 0}</td><td>{request.reason}{request.rejectionReason && <small className="rejection-reason">Rejection reason: {request.rejectionReason}</small>}</td><td><span className={`status status-${String(request.status).toLowerCase()}`}>{request.status}</span></td><td>{formatDate(request.createdAt)}</td><td>{request.status === "PENDING" ? <button type="button" className="danger-button" disabled={updatingId === `leave-${request.id}`} onClick={() => handleCancelLeave(request.id)}>{updatingId === `leave-${request.id}` ? "Cancelling..." : "Cancel"}</button> : <span className="muted-action">Processed</span>}</td></tr>)}</tbody></table></div>}</section>

      {/*<section className="dashboard-section"><div className="section-heading-row"><h2>Notifications</h2><button type="button" className="secondary-button" disabled={!unreadNotifications || updatingId === "notifications-all"} onClick={handleMarkAllAsRead}>{updatingId === "notifications-all" ? "Updating..." : "Mark all as read"}</button></div>{notifications.length === 0 ? <p className="empty-state">You have no notifications.</p> : <div className="notification-list">{notifications.map((notification) => <article className={`notification ${notification.isRead ? "read" : "unread"}`} key={notification.id}><div><p>{notification.message}</p><small>{notification.type} · {formatDate(notification.createdAt)}</small></div>{!notification.isRead && <button type="button" className="secondary-button" disabled={updatingId === `notification-${notification.id}`} onClick={() => handleMarkAsRead(notification.id)}>{updatingId === `notification-${notification.id}` ? "Updating..." : "Mark as read"}</button>}</article>)}</div>}</section>*/}

      <section id="holidays" className="dashboard-section"><h2>Upcoming Holidays</h2>{upcomingHolidays.length === 0 ? <p className="empty-state">No upcoming holidays are listed.</p> : <div className="holiday-list">{upcomingHolidays.map((holiday) => <article key={holiday.id}><h3>{holiday.name}</h3><p>{formatDate(holiday.holidayDate)}</p><p>{holiday.description || "No description provided."}</p></article>)}</div>}</section>
    </main>
  );
}

export default EmployeeDashboard;
