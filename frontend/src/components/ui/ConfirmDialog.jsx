import Modal from './Modal.jsx';

export default function ConfirmDialog({ open, onClose, onConfirm, title = 'Confirm action', description, busy = false }) {
  return (
    <Modal open={open} onClose={onClose} title={title} description={description} maxWidth="max-w-md">
      <div className="flex justify-end gap-3">
        <button type="button" className="btn-secondary" onClick={onClose} disabled={busy}>Cancel</button>
        <button type="button" className="btn-danger" onClick={onConfirm} disabled={busy}>{busy ? 'Please wait…' : 'Confirm'}</button>
      </div>
    </Modal>
  );
}
