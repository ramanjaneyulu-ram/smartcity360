export function parseApiDate(value) {
  if (!value) return null;

  const hasTimezone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(value);
  return new Date(hasTimezone ? value : `${value}Z`);
}

export function formatApiDateTime(value, options = { dateStyle: 'medium', timeStyle: 'short' }) {
  const date = parseApiDate(value);
  return date ? date.toLocaleString([], options) : '';
}