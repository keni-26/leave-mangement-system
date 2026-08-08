import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

const AUTH_KEYS = ["token", "userId", "employeeId", "email", "role"];
const navigation = {
  EMPLOYEE: [["Dashboard", "#dashboard", "⌂"], ["Apply leave", "#apply-leave", "＋"], ["My requests", "#requests", "▤"], ["Leave balance", "#balance", "◫"], ["Holidays", "#holidays", "◷"], ["Profile", "#profile", "◌"]],
  MANAGER: [["Dashboard", "#dashboard", "⌂"], ["Team requests", "#requests", "▤"], ["Profile", "#profile", "◌"]],
  HR: [["Dashboard", "/hr", "⌂", "dashboard"], ["Employees", "/hr", "▤", "employees"], ["Leave Management", "/hr/leave-management", "▤"], ["Leave Types", "/hr", "◫", "leave-types"], ["Holidays", "/hr", "◷", "holidays"], ["Profile", "/hr", "◌", "profile"]],
};

export default function AppShell({ title, children, unreadCount = 0 }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [activeHash, setActiveHash] = useState(() => window.location.hash);
  const [profileOpen, setProfileOpen] = useState(false);
  const email = localStorage.getItem("email") || "Signed-in user";
  const role = localStorage.getItem("role") || "EMPLOYEE";
  const initial = email.charAt(0).toUpperCase();
  const active = `${location.pathname}${location.hash || activeHash}`;
  const requestedProfile = role === "HR" && location.state?.hrSection === "profile";
  const logout = () => { AUTH_KEYS.forEach((key) => localStorage.removeItem(key)); navigate("/login"); };
  const closeProfile = () => {
    setProfileOpen(false);
    if (requestedProfile) navigate("/hr", { replace: true, state: null });
  };
  const goTo = (event, href, section) => {
    event.preventDefault();
    if (href.startsWith("/")) {
      navigate(href, { state: section ? { hrSection: section } : null });
      setActiveHash("");
      setProfileOpen(false);
      setOpen(false);
      return;
    }
    window.history.replaceState(null, "", href);
    setActiveHash(href);
    if (href === "#profile") { setProfileOpen(true); setOpen(false); return; }
    window.dispatchEvent(new CustomEvent("elms:navigate", { detail: href }));
    requestAnimationFrame(() => {
      const fallbackIndex = { "#apply-leave": 1, "#requests": 2, "#notifications": 3, "#holidays": 4 }[href];
      const target = document.getElementById(href.slice(1)) || (fallbackIndex !== undefined ? document.querySelectorAll(".dashboard-section")[fallbackIndex] : null);
      target?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
    setOpen(false);
  };
  useEffect(() => {
    if (!requestedProfile) return undefined;
    const timer = window.setTimeout(() => setProfileOpen(true), 0);
    return () => window.clearTimeout(timer);
  }, [requestedProfile]);

  return <div className="app-shell">
    <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
      <div className="brand"><span className="brand-mark">E</span><span>ELMS</span><button className="icon-button mobile-close" onClick={() => setOpen(false)} aria-label="Close navigation">×</button></div>
      <div className="sidebar-user"><span className="avatar">{initial}</span><div><strong>{email}</strong><small>{role}</small></div></div>
      <nav className="sidebar-nav" aria-label="Application navigation">
        {navigation[role]?.map(([label, href, icon, section]) => <a key={`${href}-${section || label}`} href={href} className={(role === "HR" ? (location.pathname === href && (section ? location.state?.hrSection === section : !location.state?.hrSection)) : active.endsWith(href)) ? "active" : ""} onClick={(event) => goTo(event, href, section)}><span>{icon}</span>{label}</a>)}
      </nav>
      <button className="sidebar-logout" type="button" onClick={logout}><span>↪</span>Logout</button>
    </aside>
    {open && <button className="sidebar-overlay" aria-label="Close navigation" onClick={() => setOpen(false)} />}
    <div className="app-content">
      <header className="topbar"><button className="icon-button menu-button" onClick={() => setOpen(true)} aria-label="Open navigation">☰</button><div><p className="eyebrow">ELMS Portal</p><h1>{title}</h1></div><div className="topbar-actions"><a href="#notifications" className="notification-button" aria-label={`${unreadCount} unread notifications`}>◉{unreadCount > 0 && <span>{unreadCount > 99 ? "99+" : unreadCount}</span>}</a><div className="topbar-profile"><span className="avatar">{initial}</span><div><strong>{email}</strong><small>{role}</small></div></div></div></header>
      <main className="page-content">{children}</main>
    </div>
    {profileOpen && <div className="modal-backdrop" role="presentation"><section className="profile-modal" role="dialog" aria-modal="true" aria-labelledby="profile-title"><div className="panel-heading"><div><p className="eyebrow">Account</p><h2 id="profile-title">My profile</h2></div><button className="icon-button" onClick={closeProfile} aria-label="Close profile">×</button></div><dl><div><dt>Name</dt><dd>{email.split("@")[0]}</dd></div><div><dt>Email</dt><dd>{email}</dd></div><div><dt>Role</dt><dd>{role}</dd></div></dl></section></div>}
  </div>;
}
