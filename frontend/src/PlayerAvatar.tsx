import { useState } from 'react';

export function PlayerAvatar({ name, avatarUrl }: { name: string; avatarUrl?: string | null }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const url = avatarUrl?.trim() || null;
  const initials = name.trim().split(/\s+/).slice(0, 2).map(part => part.charAt(0)).join('').toLocaleUpperCase() || '?';

  return (
    <span className="avatar player-avatar" aria-hidden="true">
      {url && url !== failedUrl ? (
        <img key={url} src={url} alt="" width={34} height={34} loading="lazy" decoding="async"
          referrerPolicy="no-referrer" onError={() => setFailedUrl(url)} />
      ) : initials}
    </span>
  );
}
