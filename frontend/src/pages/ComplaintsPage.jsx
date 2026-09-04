import { Edit3, FileText, MessageSquareWarning, Plus, Search, Trash2, Upload } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import Badge from '../components/ui/Badge.jsx';
import ConfirmDialog from '../components/ui/ConfirmDialog.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import Modal from '../components/ui/Modal.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { societyService } from '../services/societyService.js';
import { formatDateTime } from '../utils/format.js';
import useLiveRefresh from '../hooks/useLiveRefresh.js';

const emptyForm = {
  title: '',
  category: 'ELECTRICAL',
  description: '',
  priority: 'MEDIUM',
  residentId: '',
  attachment: '',
  raisedBy: '',
  flat: '',
};

export default function ComplaintsPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [closing, setClosing] = useState(null);
  const [details, setDetails] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [attachmentFile, setAttachmentFile] = useState(null);
  const attachmentInput = useRef(null);

  const isManager = ['ADMIN', 'SECRETARY'].includes(user?.role);

  const load = async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try {
      if (user?.role === 'RESIDENT') {
        const resident = await societyService.getResidentForUser(user.id);
        setItems(await societyService.listComplaints(resident.id));
      } else if (/^\d+$/.test(query.trim())) {
        setItems([await societyService.getComplaintByNumber(query.trim())]);
      } else if (status !== 'ALL') {
        setItems(await societyService.listComplaintsByStatus(status));
      } else if (priorityFilter !== 'ALL') {
        setItems(await societyService.listComplaintsByPriority(priorityFilter));
      } else if (categoryFilter !== 'ALL') {
        setItems(await societyService.listComplaintsByCategory(categoryFilter));
      } else {
        setItems(await societyService.listComplaints());
      }
    } catch (error) {
      push(error.message || 'Unable to load complaints.', 'error');
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => { load(); }, [user?.id, user?.role, status, priorityFilter, categoryFilter, query]);
  useLiveRefresh(() => load({ silent: true }), {
    enabled: Boolean(user?.id),
    refreshKey: `${user?.id}-${user?.role}-${status}-${priorityFilter}-${categoryFilter}-${query}`,
  });

  const filtered = useMemo(() => items.filter((item) => {
    const text = `${item.title} ${item.category} ${item.raisedBy} ${item.flat}`.toLowerCase();
    return text.includes(query.toLowerCase()) && (status === 'ALL' || item.status === status);
  }), [items, query, status]);

  const openCreate = async () => {
    setEditing(null);
    let nextForm = { ...emptyForm };
    if (user?.role === 'RESIDENT') {
      try {
        const resident = await societyService.getResidentForUser(user.id);
        nextForm = { ...nextForm, residentId: String(resident.id), raisedBy: resident.name, flat: `${resident.wing || ''}-${resident.flatNo || ''}`.replace(/^-|-$/g, '') };
      } catch (error) {
        push(error.message || 'Unable to find the resident profile for this account.', 'error');
        return;
      }
    }
    setForm(nextForm);
    setAttachmentFile(null);
    if (attachmentInput.current) attachmentInput.current.value = '';
    setModalOpen(true);
  };

  const openEdit = (item) => {
    setEditing(item);
    setForm({ ...item });
    setModalOpen(true);
  };

  const resolveResident = async () => {
    if (!form.residentId) return;
    try {
      const resident = await societyService.getResident(form.residentId);
      setForm((current) => ({ ...current, raisedBy: resident.name, flat: `${resident.wing || ''}-${resident.flatNo || ''}`.replace(/^-|-$/g, '') }));
    } catch {
      setForm((current) => ({ ...current, raisedBy: '', flat: '' }));
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      if (editing) {
        await societyService.updateComplaint(editing.id, {
          title: form.title,
          description: form.description,
          category: form.category,
          priority: form.priority,
          attachment: form.attachment || null,
        });
        if (isManager && form.status && form.status !== editing.status) {
          await societyService.updateComplaint(editing.id, {
            status: form.status,
            assignedTo: form.assignedTo || null,
            resolutionRemarks: form.resolutionRemarks || null,
          });
        }
        push('Complaint updated successfully.');
      } else {
        const createdComplaint = await societyService.createComplaint({
          residentId: Number(form.residentId),
          title: form.title,
          description: form.description,
          category: form.category,
          priority: form.priority,
          attachment: null,
        });
        if (attachmentFile) await societyService.uploadComplaintAttachment(createdComplaint.id, attachmentFile);
        push('Complaint raised successfully.');
      }
      setModalOpen(false);
      await load();
    } catch (error) {
      push(error.message || 'Unable to save complaint.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const updateStatus = async (item, nextStatus) => {
    setBusy(true);
    try {
      await societyService.updateComplaint(item.id, { status: nextStatus });
      push(`Complaint marked as ${nextStatus.replace('_', ' ')}.`);
      await load();
    } catch (error) {
      push(error.message || 'Unable to update complaint status.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const confirmDelete = async () => {
    setBusy(true);
    try {
      await societyService.deleteComplaint(deleting.id);
      push('Complaint deleted.');
      setDeleting(null);
      await load();
    } catch (error) {
      push(error.message || 'Unable to delete complaint.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const confirmClose = async () => {
    if (!closing) return;
    setBusy(true);
    try {
      await societyService.updateComplaint(closing.id, { status: 'CLOSED' });
      push('Complaint closed and moved to history.');
      setClosing(null);
      await load();
    } catch (error) {
      push(error.message || 'Unable to close complaint.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const openDetails = async (item) => {
    setBusy(true);
    try { setDetails(await societyService.getComplaint(item.id)); }
    catch (error) { push(error.message || 'Unable to load complaint details.', 'error'); }
    finally { setBusy(false); }
  };

  const openAttachment = async (item) => {
    setBusy(true);
    try {
      const response = await societyService.getComplaintAttachment(item.id);
      const url = URL.createObjectURL(new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' }));
      window.open(url, '_blank', 'noopener,noreferrer');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (error) { push(error.message || 'Unable to open the attachment.', 'error'); }
    finally { setBusy(false); }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Complaint service"
        title="Complaints & resolution"
        subtitle="Raise issues, assign responsibility, track progress and maintain a transparent resolution history."
        actions={<button className="btn-primary" onClick={openCreate}><Plus className="h-4 w-4" /> Raise complaint</button>}
      />

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Open', items.filter((item) => item.status === 'OPEN').length, 'bg-amber-50 text-amber-700'],
          ['In progress', items.filter((item) => item.status === 'IN_PROGRESS').length, 'bg-blue-50 text-blue-700'],
          ['Resolved', items.filter((item) => item.status === 'RESOLVED').length, 'bg-emerald-50 text-emerald-700'],
          ['High priority', items.filter((item) => item.priority === 'HIGH' && item.status !== 'RESOLVED').length, 'bg-rose-50 text-rose-700'],
        ].map(([label, value, tone]) => (
          <div key={label} className="panel flex items-center gap-4 p-4">
            <div className={`grid h-11 w-11 place-items-center rounded-xl ${tone}`}><MessageSquareWarning className="h-5 w-5" /></div>
            <div><p className="text-2xl font-extrabold text-slate-900">{value}</p><p className="text-xs font-semibold text-slate-500">{label}</p></div>
          </div>
        ))}
      </section>

      <section className="panel mt-6 overflow-hidden">
        <div className="panel-header">
          <div><h2 className="font-extrabold text-slate-900">Complaint register</h2><p className="text-xs text-slate-500">{filtered.length} complaint records shown</p></div>
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row">
            <label className="relative block sm:w-64"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><input className="field-input pl-9" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search complaint" /></label>
            <select className="field-input sm:w-36" value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value)}><option value="ALL">All priority</option><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option><option value="URGENT">Urgent</option></select>
            <select className="field-input sm:w-40" value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}><option value="ALL">All categories</option>{['PLUMBING', 'ELECTRICAL', 'LIFT', 'PARKING', 'SECURITY', 'HOUSEKEEPING', 'WATER', 'GARDEN', 'OTHER'].map((category) => <option key={category}>{category}</option>)}</select>
            <select className="field-input sm:w-44" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All status</option><option value="OPEN">Open</option><option value="IN_PROGRESS">In progress</option><option value="RESOLVED">Resolved</option><option value="CLOSED">Closed</option></select>
          </div>
        </div>

        {loading ? <div className="p-5"><LoadingState /></div> : filtered.length === 0 ? <EmptyState /> : (
          <div className="table-responsive">
            <table className="data-table">
              <thead><tr><th>Complaint</th><th>Resident</th><th>Priority</th><th>Assigned to</th><th>Created</th><th>Status</th><th className="text-right">Actions</th></tr></thead>
              <tbody>{filtered.map((item) => (
                <tr key={item.id}>
                  <td><p className="font-bold text-slate-800">#{item.id} · {item.title}</p><p className="mt-0.5 max-w-xs truncate text-xs text-slate-400">{item.category} · {item.description}</p></td>
                  <td><p className="font-semibold text-slate-700">{item.raisedBy}</p><p className="mt-0.5 text-xs text-slate-400">{item.flat}</p></td>
                  <td><Badge value={item.priority} /></td>
                  <td>{item.assignedTo || 'Unassigned'}</td>
                  <td>{formatDateTime(item.createdAt)}</td>
                  <td><Badge value={item.status} /></td>
                  <td><div className="flex justify-end gap-1"><button className="rounded-lg bg-blue-50 px-2 py-1 text-xs font-bold text-blue-700 hover:bg-blue-100" onClick={() => openDetails(item)}>View details</button>{item.attachment && <button className="rounded-lg p-2 text-slate-500 hover:bg-emerald-50 hover:text-emerald-700" onClick={() => openAttachment(item)} title="View attachment"><FileText className="h-4 w-4" /></button>}
                    {item.status === 'OPEN' && (isManager || item.raisedBy === user?.name) && <button className="rounded-lg p-2 text-slate-500 hover:bg-blue-50 hover:text-blue-700" onClick={() => openEdit(item)} title="Edit open complaint"><Edit3 className="h-4 w-4" /></button>}
                    {isManager && item.status === 'OPEN' && <button disabled={busy} onClick={() => updateStatus(item, 'IN_PROGRESS')} className="rounded-lg px-2 py-1 text-xs font-bold text-blue-700 hover:bg-blue-50">Start</button>}
                    {isManager && item.status === 'IN_PROGRESS' && <button disabled={busy} onClick={() => updateStatus(item, 'RESOLVED')} className="rounded-lg px-2 py-1 text-xs font-bold text-emerald-700 hover:bg-emerald-50">Resolve</button>}
                    {isManager && item.status === 'RESOLVED' && <button disabled={busy} onClick={() => setClosing(item)} className="rounded-lg bg-slate-800 px-2 py-1 text-xs font-bold text-white hover:bg-slate-950 disabled:opacity-60">Close</button>}
                    {item.status === 'OPEN' && (isManager || item.raisedBy === user?.name) && <button className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700" onClick={() => setDeleting(item)} title="Delete open complaint"><Trash2 className="h-4 w-4" /></button>}
                  </div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Update complaint' : 'Raise complaint'} description="Provide enough detail for the committee or maintenance team to act quickly.">
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><label className="field-label">Complaint title</label><input className="field-input" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} required /></div>
          <div><label className="field-label">Category</label><select className="field-input" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}><option value="ELECTRICAL">Electrical</option><option value="PLUMBING">Plumbing</option><option value="SECURITY">Security</option><option value="HOUSEKEEPING">Housekeeping</option><option value="LIFT">Lift</option><option value="PARKING">Parking</option><option value="WATER">Water</option><option value="GARDEN">Garden</option><option value="OTHER">Other</option></select></div>
          <div><label className="field-label">Priority</label><select className="field-input" value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value })}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></div>
          {!editing && <div><label className="field-label">Resident ID</label><input className="field-input" type="number" min="1" value={form.residentId} onChange={(event) => setForm({ ...form, residentId: event.target.value, raisedBy: '', flat: '' })} onBlur={resolveResident} readOnly={user?.role === 'RESIDENT'} required />{form.raisedBy && <p className="mt-1 text-xs font-semibold text-emerald-600">{form.raisedBy}{form.flat ? ` · Flat ${form.flat}` : ''}</p>}{form.residentId && !form.raisedBy && <p className="mt-1 text-xs font-semibold text-rose-600">Enter a valid resident ID.</p>}</div>}
          {!editing && <div><label className="field-label">Photo or document (optional)</label><input ref={attachmentInput} className="field-input" type="file" accept="image/jpeg,image/png,image/webp,application/pdf" onChange={(event) => setAttachmentFile(event.target.files?.[0] || null)} /><p className="mt-1 text-xs text-slate-500">{attachmentFile ? attachmentFile.name : 'JPG, PNG, WEBP or PDF · maximum 5 MB'}</p></div>}
          <div className="sm:col-span-2"><label className="field-label">Description</label><textarea className="field-input min-h-28 resize-y" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} required /></div>
          {editing && isManager && <><div><label className="field-label">Assigned to</label><input className="field-input" value={form.assignedTo || ''} onChange={(event) => setForm({ ...form, assignedTo: event.target.value })} /></div><div><label className="field-label">Status</label><select className="field-input" value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}><option value="OPEN">Open</option><option value="IN_PROGRESS">Start work</option></select><p className="mt-1 text-xs text-slate-500">Further status changes use the Resolve and Close actions.</p></div></>}
          <div className="sm:col-span-2 mt-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save changes' : 'Submit complaint'}</button></div>
        </form>
      </Modal>

      <Modal open={Boolean(details)} onClose={() => setDetails(null)} title={details?.title || 'Complaint details'} description={`Complaint #${details?.id || ''}`}>{details && <div className="space-y-3 rounded-xl bg-slate-50 p-4 text-sm">{[['Category', details.category], ['Priority', details.priority], ['Status', details.status], ['Raised by', details.raisedBy], ['Flat', details.flat], ['Assigned to', details.assignedTo || 'Unassigned'], ['Description', details.description], ['Resolution', details.resolutionRemarks || '—']].map(([label, value]) => <div key={label}><span className="font-bold text-slate-500">{label}: </span><span className="text-slate-800">{value}</span></div>)}</div>}</Modal>

      <ConfirmDialog open={Boolean(closing)} onClose={() => setClosing(null)} onConfirm={confirmClose} busy={busy} title="Close complaint?" description={`Complaint #${closing?.id || ''} will be finalized as read-only history. Use this after confirming that the resolution is accepted.`} />
      <ConfirmDialog open={Boolean(deleting)} onClose={() => setDeleting(null)} onConfirm={confirmDelete} busy={busy} title="Delete complaint?" description={`Complaint #${deleting?.id || ''} will be permanently removed. Only open complaints can be deleted.`} />
    </div>
  );
}
