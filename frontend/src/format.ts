export function timeRange(start: string, end: string): string {
  const from = new Date(start);
  const to = new Date(end);
  const sameDay = from.toDateString() === to.toDateString();
  return `${from.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} – ${sameDay ? '' : to.toLocaleDateString([], { month: 'short', day: 'numeric' }) + ', '}${to.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', timeZoneName: 'short' })}`;
}
export function gameDate(start: string): string {
  return new Date(start).toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
}
