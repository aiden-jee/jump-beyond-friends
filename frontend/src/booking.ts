import { api } from './api.ts';

export async function confirmBooking(id: number, confirm: (text: string) => boolean | Promise<boolean>): Promise<boolean> {
  const current = await api.game(id);
  if (current.status !== 'PROPOSED') throw new Error('This game is no longer proposed. Its current status will be refreshed.');
  const warning = current.meetsAttendanceThreshold ? '' : `Only ${current.goingCount} of ${current.threshold} required players are going. The attendance minimum has not been met.\n\n`;
  if (!await confirm(`${warning}Confirm only after you have reserved the court separately.`)) return false;
  await api.bookGame(id);
  return true;
}
