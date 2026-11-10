import { useEffect, useId, useRef } from 'react';

export function ConfirmationDialog({ title, description, dismissLabel, confirmLabel, destructive = false, onDismiss, onConfirm }: {
  title: string;
  description: string;
  dismissLabel: string;
  confirmLabel: string;
  destructive?: boolean;
  onDismiss: () => void;
  onConfirm: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const element = dialog.current!;
    element.showModal();
    return () => element.close();
  }, []);

  return (
    <dialog
      ref={dialog}
      className="confirmation-dialog"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      onCancel={event => {
        event.preventDefault();
        onDismiss();
      }}
    >
      <h2 id={titleId}>{title}</h2>
      <p id={descriptionId}>{description}</p>
      <div className="confirmation-actions">
        <button type="button" className="secondary" autoFocus onClick={onDismiss}>
          {dismissLabel}
        </button>
        <button type="button" className={destructive ? 'danger' : 'primary'} onClick={onConfirm}>
          {confirmLabel}
        </button>
      </div>
    </dialog>
  );
}
