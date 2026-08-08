import { useCallback, useEffect, useMemo, useState } from "react";
import api from "../services/api";
import StatusBadge from "../components/StatusBadge";

const EMPTY_FILTERS = { employee: "", status: "", leaveType: "", date: "" };

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  return Number.isNaN(date.getTime()) ? "-" : date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function LeaveManagement() {
  const [requests, setRequests] = useState([]);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [requestsResponse, typesResponse] = await Promise.all([api.get("/leave-requests"), api.get("/leave-types")]);
      setRequests(requestsResponse.data ?? []);
      setLeaveTypes(typesResponse.data ?? []);
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        ["token", "userId", "employeeId", "email", "role"].forEach((key) => localStorage.removeItem(key));
        window.location.assign("/login");
        return;
      }
      setError(requestError.response?.data?.message || "We could not load leave requests. Please try again.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(loadData, 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  const filteredRequests = useMemo(() => requests.filter((request) => {
    const employee = `${request.employeeName || ""} ${request.employeeCode || ""}`.toLowerCase();
    const isInDateRange = !filters.date || (request.startDate <= filters.date && request.endDate >= filters.date);
    return (!filters.employee || employee.includes(filters.employee.trim().toLowerCase()))
      && (!filters.status || request.status === filters.status)
      && (!filters.leaveType || String(request.leaveTypeId) === filters.leaveType)
      && isInDateRange;
  }), [filters, requests]);

  const today = new Date().toLocaleDateString("en-CA");
  const summary = {
    activeToday: requests.filter((request) => ["APPROVED", "AUTO_APPROVED"].includes(request.status) && request.startDate <= today && request.endDate >= today).length,
    pending: requests.filter((request) => request.status === "PENDING").length,
    approved: requests.filter((request) => request.status === "APPROVED").length,
    rejected: requests.filter((request) => request.status === "REJECTED").length,
  };

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  if (loading) return <main className="dashboard-loading">Loading leave management...</main>;

  return <main className="leave-management">
    <header className="dashboard-header"><div><h1>Leave Management</h1><p>Review leave activity across your organization.</p></div></header>
    {error && <p className="dashboard-message error-message" role="alert">{error}</p>}

    <section className="balance-grid" aria-label="Leave request summary">
      <article className="balance-card"><h3>Employees On Leave Today</h3><dl><div><dt>Approved active leave</dt><dd>{summary.activeToday}</dd></div></dl></article>
      <article className="balance-card"><h3>Pending Requests</h3><dl><div><dt>Awaiting review</dt><dd>{summary.pending}</dd></div></dl></article>
      <article className="balance-card"><h3>Approved Requests</h3><dl><div><dt>Manager or HR approved</dt><dd>{summary.approved}</dd></div></dl></article>
      <article className="balance-card"><h3>Rejected Requests</h3><dl><div><dt>Not approved</dt><dd>{summary.rejected}</dd></div></dl></article>
    </section>

    <section className="dashboard-section leave-filter-panel" aria-label="Filter leave requests">
      <div className="section-heading-row"><div><h2>Leave Requests</h2><p className="helper-text">Search and filter centralized leave records.</p></div><button type="button" className="secondary-button" onClick={() => setFilters(EMPTY_FILTERS)}>Reset Filters</button></div>
      <div className="leave-filters">
        <label>Employee Name<input value={filters.employee} onChange={(event) => updateFilter("employee", event.target.value)} placeholder="Search name or code" /></label>
        <label>Status<select value={filters.status} onChange={(event) => updateFilter("status", event.target.value)}><option value="">All</option><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option><option value="CANCELLED">Cancelled</option><option value="AUTO_APPROVED">Auto Approved</option></select></label>
        <label>Leave Type<select value={filters.leaveType} onChange={(event) => updateFilter("leaveType", event.target.value)}><option value="">All</option>{leaveTypes.map((type) => <option key={type.id} value={type.id}>{type.name}</option>)}</select></label>
        <label>Date<input type="date" value={filters.date} onChange={(event) => updateFilter("date", event.target.value)} /></label>
      </div>
    </section>

    <section className="dashboard-section leave-table-section">
      <p className="helper-text">Showing {filteredRequests.length} of {requests.length} request{requests.length === 1 ? "" : "s"}.</p>
      {filteredRequests.length === 0 ? <p className="empty-state">No leave requests match these filters.</p> : <div className="table-wrapper"><table><thead><tr><th>Employee Name</th><th>Employee Code</th><th>Leave Type</th><th>Start Date</th><th>End Date</th><th>Leave Days</th><th>Status</th><th>Reviewed By</th><th>Action</th></tr></thead><tbody>{filteredRequests.map((request) => <tr key={request.id}><td>{request.employeeName || "-"}</td><td>{request.employeeCode || "-"}</td><td>{request.leaveTypeName || "-"}</td><td>{formatDate(request.startDate)}</td><td>{formatDate(request.endDate)}</td><td>{request.leaveDays ?? "-"}</td><td><StatusBadge status={request.status} /></td><td>{request.reviewedBy || "-"}</td><td><button type="button" className="secondary-button" onClick={() => setSelectedRequest(request)}>View Details</button></td></tr>)}</tbody></table></div>}
    </section>

    {selectedRequest && <div className="modal-backdrop" role="presentation" onMouseDown={() => setSelectedRequest(null)}><section className="profile-modal leave-details-modal" role="dialog" aria-modal="true" aria-labelledby="leave-details-title" onMouseDown={(event) => event.stopPropagation()}><div className="panel-heading"><div><p className="eyebrow">Leave request</p><h2 id="leave-details-title">Request details</h2></div><button type="button" className="icon-button" onClick={() => setSelectedRequest(null)} aria-label="Close details">×</button></div><dl>
      <div><dt>Employee Name</dt><dd>{selectedRequest.employeeName || "-"}</dd></div><div><dt>Employee Code</dt><dd>{selectedRequest.employeeCode || "-"}</dd></div><div><dt>Email</dt><dd>{selectedRequest.employeeEmail || "-"}</dd></div><div><dt>Manager Name</dt><dd>{selectedRequest.managerName || "-"}</dd></div><div><dt>Leave Type</dt><dd>{selectedRequest.leaveTypeName || "-"}</dd></div><div><dt>Reason</dt><dd>{selectedRequest.reason || "-"}</dd></div><div><dt>Applied Date</dt><dd>{formatDate(selectedRequest.createdAt)}</dd></div><div><dt>Start Date</dt><dd>{formatDate(selectedRequest.startDate)}</dd></div><div><dt>End Date</dt><dd>{formatDate(selectedRequest.endDate)}</dd></div><div><dt>Leave Days</dt><dd>{selectedRequest.leaveDays ?? "-"}</dd></div><div><dt>Status</dt><dd><StatusBadge status={selectedRequest.status} /></dd></div><div><dt>Reviewed By</dt><dd>{selectedRequest.reviewedBy || "-"}</dd></div><div><dt>Reviewed At</dt><dd>{formatDate(selectedRequest.reviewedAt)}</dd></div>
    </dl></section></div>}
  </main>;
}

export default LeaveManagement;
