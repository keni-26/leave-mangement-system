import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

const AUTH_STORAGE_KEYS = ["token", "userId", "employeeId", "email", "role"];

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  return Number.isNaN(date.getTime())
    ? "-"
    : date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function getErrorMessage(requestError, fallbackMessage) {
  const data = requestError.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  return data?.message || data?.error || fallbackMessage;
}

function ManagerDashboard() {
  const navigate = useNavigate();
  const email = localStorage.getItem("email");
  // Manager identity always comes from the authenticated login session.
  const managerId = localStorage.getItem("employeeId");
  const userId = localStorage.getItem("userId");
  const [leaveRequests, setLeaveRequests] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [updatingId, setUpdatingId] = useState(null);
  const [rejectingRequest, setRejectingRequest] = useState(null);
  const [rejectionReason, setRejectionReason] = useState("");
  const [rejectionError, setRejectionError] = useState("");

  const clearAuthentication = useCallback(() => {
    AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
    navigate("/login");
  }, [navigate]);

  const handleRequestError = useCallback((requestError, fallbackMessage) => {
    const status = requestError.response?.status;
    if (status === 401) {
      clearAuthentication();
      return true;
    }
    if (status === 403) {
      setError("You are not authorized to perform this action.");
      return true;
    }
    setError(getErrorMessage(requestError, fallbackMessage));
    return false;
  }, [clearAuthentication]);

  const loadDashboardData = useCallback(async () => {
    if (!managerId || !userId) {
      setError("Your account is missing manager information. Please sign in again.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const [requestsResponse, notificationsResponse] = await Promise.all([
        api.get(`/leave-requests/manager/${managerId}`),
        api.get(`/notifications/user/${userId}`),
      ]);
      setLeaveRequests(requestsResponse.data ?? []);
      setNotifications(notificationsResponse.data ?? []);
    } catch (requestError) {
      handleRequestError(requestError, "We could not load your dashboard. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [handleRequestError, managerId, userId]);

  useEffect(() => { loadDashboardData(); }, [loadDashboardData]);

  const handleApprove = async (requestId) => {
    if (!window.confirm("Approve this leave request?")) return;

    setError("");
    setSuccess("");
    setUpdatingId(`leave-${requestId}`);
    try {
      await api.put(`/leave-requests/${requestId}/approve`, { managerId: Number(managerId) });
      setSuccess("The leave request has been approved.");
      await loadDashboardData();
    } catch (requestError) {
      handleRequestError(requestError, "We could not approve that leave request. Please try again.");
    } finally {
      setUpdatingId(null);
    }
  };

  const openRejectForm = (request) => {
    setRejectingRequest(request);
    setRejectionReason("");
    setRejectionError("");
    setError("");
  };

  const handleReject = async (event) => {
    event.preventDefault();
    const trimmedReason = rejectionReason.trim();
    if (!trimmedReason) {
      setRejectionError("Please provide a reason for rejecting this leave request.");
      return;
    }

    setError("");
    setSuccess("");
    setRejectionError("");
    setUpdatingId(`leave-${rejectingRequest.id}`);
    try {
      await api.put(`/leave-requests/${rejectingRequest.id}/reject`, {
        managerId: Number(managerId),
        rejectionReason: trimmedReason,
      });
      setRejectingRequest(null);
      setSuccess("The leave request has been rejected.");
      await loadDashboardData();
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        clearAuthentication();
      } else if (requestError.response?.status === 403) {
        setRejectionError("You are not authorized to perform this action.");
      } else {
        setRejectionError(getErrorMessage(requestError, "We could not reject that leave request. Please try again."));
      }
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

  const unreadNotifications = notifications.some((notification) => !notification.isRead);

  if (loading) return <main className="dashboard-loading">Loading your manager dashboard...</main>;

  return (
    <main className="manager-dashboard">
      <header className="dashboard-header">
        <div><h1>Manager Dashboard</h1><p>{email || "Signed-in manager"}</p></div>
        <button type="button" className="secondary-button" onClick={clearAuthentication}>Logout</button>
      </header>

      {error && <p className="dashboard-message error-message" role="alert">{error}</p>}
      {success && <p className="dashboard-message success-message" role="status">{success}</p>}

      <section className="dashboard-section">
        <h2>Team Leave Requests</h2>
        {leaveRequests.length === 0 ? <p className="empty-state">There are no leave requests from your team.</p> : (
          <div className="table-wrapper"><table><thead><tr><th>Employee</th><th>Leave type</th><th>Dates</th><th>Days</th><th>Reason</th><th>Status</th><th>Created</th><th>Action</th></tr></thead><tbody>
            {leaveRequests.map((request) => <tr key={request.id}>
              <td>{request.employee?.name || "-"}{request.employee?.employeeCode && <small className="employee-code">{request.employee.employeeCode}</small>}</td>
              <td>{request.leaveType?.name || "-"}</td>
              <td>{formatDate(request.startDate)} – {formatDate(request.endDate)}</td>
              <td>{request.leaveDays ?? 0}</td>
              <td>{request.reason || "-"}{request.rejectionReason && <small className="rejection-reason">Rejection reason: {request.rejectionReason}</small>}</td>
              <td><span className={`status status-${String(request.status).toLowerCase()}`}>{request.status}</span></td>
              <td>{formatDate(request.createdAt)}</td>
              <td>{request.status === "PENDING" && <div className="request-actions"><button type="button" disabled={updatingId === `leave-${request.id}`} onClick={() => handleApprove(request.id)}>{updatingId === `leave-${request.id}` ? "Updating..." : "Approve"}</button><button type="button" className="danger-button" disabled={updatingId === `leave-${request.id}`} onClick={() => openRejectForm(request)}>Reject</button></div>}</td>
            </tr>)}
          </tbody></table></div>
        )}
      </section>

      <section className="dashboard-section">
        <div className="section-heading-row"><h2>Notifications</h2><button type="button" className="secondary-button" disabled={!unreadNotifications || updatingId === "notifications-all"} onClick={handleMarkAllAsRead}>{updatingId === "notifications-all" ? "Updating..." : "Mark all as read"}</button></div>
        {notifications.length === 0 ? <p className="empty-state">You have no notifications.</p> : <div className="notification-list">{notifications.map((notification) => <article className={`notification ${notification.isRead ? "read" : "unread"}`} key={notification.id}><div><p>{notification.message}</p><small>{notification.type} · {formatDate(notification.createdAt)}</small></div>{!notification.isRead && <button type="button" className="secondary-button" disabled={updatingId === `notification-${notification.id}`} onClick={() => handleMarkAsRead(notification.id)}>{updatingId === `notification-${notification.id}` ? "Updating..." : "Mark as read"}</button>}</article>)}</div>}
      </section>

      {rejectingRequest && <div className="modal-backdrop" role="presentation"><section className="reject-modal" role="dialog" aria-modal="true" aria-labelledby="reject-title"><h2 id="reject-title">Reject Leave Request</h2><p>Provide a reason for rejecting {rejectingRequest.employee?.name || "this employee"}'s request.</p><form onSubmit={handleReject}><label>Rejection reason<textarea value={rejectionReason} onChange={(event) => setRejectionReason(event.target.value)} rows="4" required autoFocus /></label>{rejectionError && <p className="error-message" role="alert">{rejectionError}</p>}<div className="modal-actions"><button type="button" className="secondary-button" disabled={updatingId === `leave-${rejectingRequest.id}`} onClick={() => setRejectingRequest(null)}>Cancel</button><button type="submit" className="danger-button" disabled={updatingId === `leave-${rejectingRequest.id}`}>{updatingId === `leave-${rejectingRequest.id}` ? "Rejecting..." : "Reject request"}</button></div></form></section></div>}
    </main>
  );
}

export default ManagerDashboard;
