import { test } from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { copyInvitation, gameInvitation, shareInvitation } from '../src/sharing.ts';
import type { Game } from '../src/types.ts';

const game: Game = {
  gameId: 123,
  startTime: '2026-11-22T01:00:00Z',
  endTime: '2026-11-22T03:00:00Z',
  status: 'PROPOSED',
  threshold: 8,
  goingCount: 6,
  meetsAttendanceThreshold: false,
};
const invitation = gameInvitation(game, 'https://volleyball.example')!;

test('invitation includes attendance and exactly one frontend game URL', () => {
  assert.match(invitation, /We’re planning volleyball!/);
  assert.match(invitation, /^👥 6 going$/m);
  assert.match(invitation, /RSVP here:\nhttps:\/\/volleyball\.example\/games\/123$/);
  assert.equal(invitation.match(/https:\/\//g)?.length, 1);
  assert.doesNotMatch(invitation, /\/api\/|venue|location|needed/i);
  assert.match(gameInvitation({ ...game, goingCount: 7 }, 'https://volleyball.example')!, /^👥 7 going$/m);
});

test('formats UTC instants in the viewer’s local timezone, including overnight games', () => {
  const moduleUrl = new URL('../src/sharing.ts', import.meta.url).href;
  const output = execFileSync(process.execPath, ['--experimental-strip-types', '--input-type=module', '-e', `
    import { gameInvitation } from ${JSON.stringify(moduleUrl)};
    const game = ${JSON.stringify(game)};
    console.log(JSON.stringify([
      gameInvitation(game, 'https://volleyball.example'),
      gameInvitation({ ...game, endTime: '2026-11-22T09:00:00Z' }, 'https://volleyball.example'),
    ]));
  `], { env: { ...process.env, TZ: 'America/Los_Angeles', LANG: 'en_US.UTF-8', LC_ALL: 'en_US.UTF-8' }, encoding: 'utf8' });
  const [sameDay, overnight] = JSON.parse(output);
  assert.match(sameDay, /Saturday, Nov 21, 2026/);
  assert.match(sameDay, /5:00 PM – 7:00 PM PST/);
  assert.match(overnight, /5:00 PM – Nov 22, 1:00 AM PST/);
});

test('booked games are confirmed and cancelled games have no invitation', () => {
  const booked = gameInvitation({ ...game, status: 'BOOKED' }, 'https://volleyball.example')!;
  assert.match(booked, /Volleyball is confirmed!/);
  assert.doesNotMatch(booked, /planning/);
  assert.equal(gameInvitation({ ...game, status: 'CANCELLED' }, 'https://volleyball.example'), null);
});

test('native sharing starts synchronously and supplies only text with the full invitation', async () => {
  let received: ShareData | undefined;
  const pending = shareInvitation(invitation, {
    share: async data => { received = data; },
    clipboard: { writeText: async () => assert.fail('Native sharing should not copy') },
  });
  assert.deepEqual(received, { text: invitation });
  assert.equal(await pending, 'shared');
});

test('dismissing native sharing is silent and does not copy', async () => {
  assert.equal(await shareInvitation(invitation, {
    share: async () => { throw new DOMException('Dismissed', 'AbortError'); },
    clipboard: { writeText: async () => assert.fail('Dismissal should not copy') },
  }), 'dismissed');
  // Error objects can originate from another browser realm.
  assert.equal(await shareInvitation(invitation, {
    share: async () => { throw { name: 'AbortError' }; },
  }), 'dismissed');
});

test('native errors offer a fresh copy action without an unhandled rejection', async () => {
  assert.equal(await shareInvitation(invitation, {
    share: async () => { throw new DOMException('Blocked', 'NotAllowedError'); },
    clipboard: { writeText: async () => assert.fail('Copy should wait for another tap') },
  }), 'share-failed');
});

test('without native sharing, clipboard fallback copies the complete message', async () => {
  let copied = '';
  assert.equal(await shareInvitation(invitation, {
    clipboard: { writeText: async text => { copied = text; } },
  }), 'copied');
  assert.equal(copied, invitation);
});

test('missing or denied clipboard access offers manual copying', async () => {
  assert.equal(await shareInvitation(invitation, {}), 'manual');
  assert.equal(await copyInvitation(invitation, {
    clipboard: { writeText: async () => { throw new DOMException('Denied', 'NotAllowedError'); } },
  }), 'manual');
});
