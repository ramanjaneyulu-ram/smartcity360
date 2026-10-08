const map = {
  High: 'chip-red', Medium: 'chip-amber', Low: 'chip-gray',
  HIGH: 'chip-red', MEDIUM: 'chip-amber', LOW: 'chip-gray',
  SUBMITTED: 'chip-gray', CLASSIFIED: 'chip-gray', ASSIGNED: 'chip-amber',
  IN_PROGRESS: 'chip-amber', RESOLVED: 'chip-sage', VERIFIED: 'chip-sage',
};
const labels = {
  HIGH: 'High', MEDIUM: 'Medium', LOW: 'Low',
  SUBMITTED: 'Submitted', CLASSIFIED: 'Classified', ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved', VERIFIED: 'Verified',
};

export default function Chip({ value, big }) {
  const cls = map[value] || 'chip-gray';
  const label = labels[value] || value;
  return (
    <span className={`chip ${cls}`} style={big ? { fontSize: 13, padding: '6px 13px' } : undefined}>
      <span className="chip-dot" />{label}
    </span>
  );
}
