import type { GameInput } from './types.ts';

function localDateTime(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function defaultStartTime(now = new Date()): string {
  return localDateTime(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1));
}

export function oneHourAfter(start: string): string {
  const end = new Date(start);
  if (!Number.isFinite(end.getTime())) return '';
  end.setHours(end.getHours() + 1);
  return localDateTime(end);
}

export function gameInput(start: string, end: string, threshold: string): GameInput {
  const startTime = new Date(start);
  const endTime = new Date(end);
  const minimum = Number(threshold);
  if (!Number.isFinite(startTime.getTime()) || !Number.isFinite(endTime.getTime())) throw new Error('Choose a valid start and end time.');
  if (startTime.getTime() <= Date.now()) throw new Error('Choose a start time in the future.');
  if (endTime <= startTime) throw new Error('The end time must be after the start time.');
  if (!Number.isInteger(minimum) || minimum < 2 || minimum > 2147483647) throw new Error('The minimum must be a whole number of at least 2.');
  return { startTime: startTime.toISOString(), endTime: endTime.toISOString(), threshold: minimum };
}
