export interface Player { playerId: number; name: string }
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
export interface PlayerRsvp { playerId: number; status: RsvpStatus }
