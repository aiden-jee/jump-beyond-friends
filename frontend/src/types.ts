export interface Player { id: number; name: string; avatarUrl: string | null }
export type GameStatus = 'PROPOSED' | 'BOOKED' | 'CANCELLED';
export type RsvpStatus = 'GOING' | 'MAYBE' | 'NOT_GOING';
export interface Game {
  gameId: number;
  startTime: string;
  endTime: string;
  threshold: number;
  status: GameStatus;
  goingCount: number;
  meetsAttendanceThreshold: boolean;
}
export interface GameInput { startTime: string; endTime: string; threshold: number }
export interface PlayerRsvp { playerId: number; name: string; avatarUrl: string | null; status: RsvpStatus }
