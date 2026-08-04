export default function StatusBadge({ status }) {
  const value = String(status || "UNKNOWN");
  return <span className={`status status-${value.toLowerCase()}`}>{value.replace("_", " ")}</span>;
}
