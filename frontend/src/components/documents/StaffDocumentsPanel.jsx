import { FileText, Plus, Upload } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { societyService } from '../../services/societyService.js';
import { formatDate, formatDateTime } from '../../utils/format.js';
import Badge from '../ui/Badge.jsx';
import EmptyState from '../ui/EmptyState.jsx';
import LoadingState from '../ui/LoadingState.jsx';
import Modal from '../ui/Modal.jsx';
import PageHeader from '../ui/PageHeader.jsx';
import useLiveRefresh from '../../hooks/useLiveRefresh.js';
import { prepareFileViewer, showBlobInViewer } from '../../utils/fileViewer.js';

const emptyForm = { staffUserId: '', documentType: 'ID_PROOF', documentName: '', expiryDate: '' };
const friendlyType = (type) => String(type || 'OTHER').replaceAll('_', ' ');

export default function StaffDocumentsPanel() {
  const { user } = useAuth();
  const { push } = useToast();
  const input = useRef(null);
  const manager = ['ADMIN', 'SECRETARY'].includes(user?.role);
  const isSecurity = user?.role === 'SECURITY';
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [open, setOpen] = useState(false);
  const [file, setFile] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [search, setSearch] = useState('');

  const load = async ({ silent = false } = {}) => {
    if (!manager && !isSecurity) return;
    if (!silent) setLoading(true);
    try { setItems(await societyService.listStaffDocuments()); }
    catch (error) { push(error.message || 'Unable to load staff documents.', 'error'); }
    finally { if (!silent) setLoading(false); }
  };
  useEffect(() => { load(); }, [user?.role, user?.id]);
  useLiveRefresh(() => load({ silent: true }), {
    enabled: Boolean(user?.id && (manager || isSecurity)),
    refreshKey: `${user?.id}-${user?.role}-staff-documents`,
  });

  const submit = async (event) => {
    event.preventDefault();
    if (!file) { push('Choose a document file to upload.', 'error'); return; }
    setBusy(true);
    try {
      await societyService.uploadStaffDocument({ ...form, staffUserId: Number(form.staffUserId) }, file);
      push('Staff document uploaded for verification.');
      setOpen(false); setFile(null); setForm(emptyForm); if (input.current) input.current.value = '';
      await load();
    } catch (error) { push(error.message || 'Unable to upload staff document.', 'error'); }
    finally { setBusy(false); }
  };
  const view = async (item) => {
    const viewer = prepareFileViewer();
    setBusy(true);
    try {
      const response = await societyService.getStaffDocumentFile(item.id);
      showBlobInViewer(response, viewer);
    }
    catch (error) {
      viewer?.close();
      push(error.message || 'Unable to open this document.', 'error');
    }
    finally { setBusy(false); }
  };
  const filtered = items.filter((item) => { const query = search.trim().toLowerCase(); return !query || [item.staffUserId, item.documentName, item.documentType, item.verificationStatus].some((value) => String(value || '').toLowerCase().includes(query)); });
  const subtitle = manager ? 'Maintain private identity, police-verification, employment, and training records for staff.' : isSecurity ? 'View your own verification and employment documents.' : 'Staff and security documents are private records available only to authorized management and the relevant staff member.';

  return <>
    <PageHeader eyebrow="Staff & security" title={manager ? 'Staff document register' : isSecurity ? 'My staff documents' : 'Staff & security documents'} subtitle={subtitle} actions={manager && <button type="button" className="btn-primary" onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> Upload staff document</button>} />
    {!manager && !isSecurity ? <section className="panel"><EmptyState title="Private staff records" description="Your role does not have access to staff identity, employment, or security-verification files." /></section> : <section className="panel overflow-hidden"><div className="panel-header"><div><h2 className="font-extrabold text-slate-900">{manager ? 'Staff document register' : 'My uploaded documents'}</h2><p className="text-xs text-slate-500">{filtered.length} of {items.length} records shown</p></div>{manager && <input className="field-input w-full sm:w-72" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search staff ID or document" />}</div>{loading ? <div className="p-6"><LoadingState /></div> : filtered.length === 0 ? <EmptyState title="No staff documents found" description={manager ? 'Upload a staff ID proof, police verification, or employment record.' : 'Your management team has not uploaded any documents for you yet.'} /> : <div className="table-responsive"><table className="data-table"><thead><tr><th>Document</th>{manager && <th>Staff user</th>}<th>Expiry</th><th>Uploaded</th><th>Status</th><th className="text-right">Actions</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td><p className="font-bold text-slate-800">{item.documentName}</p><p className="text-xs text-slate-500">{friendlyType(item.documentType)}</p></td>{manager && <td><p className="font-medium text-slate-800">User #{item.staffUserId}</p></td>}<td>{item.expiryDate ? formatDate(item.expiryDate) : 'No expiry'}</td><td>{formatDateTime(item.createdAt)}</td><td><Badge value={item.verificationStatus || 'PENDING'} /></td><td className="text-right"><button type="button" className="rounded-lg p-2 text-slate-500 hover:bg-emerald-50 hover:text-emerald-700" disabled={busy} onClick={() => view(item)} title="View document"><FileText className="h-4 w-4" /></button></td></tr>)}</tbody></table></div>}</section>}
    <Modal open={open} onClose={() => setOpen(false)} title="Upload staff document" description="Attach a staff member's private verification or employment record."><form onSubmit={submit} className="grid gap-4 sm:grid-cols-2"><div className="sm:col-span-2"><label className="field-label">Staff user ID</label><input className="field-input" type="number" min="1" value={form.staffUserId} onChange={(event) => setForm({ ...form, staffUserId: event.target.value })} placeholder="Enter security or staff user ID" required /></div><div><label className="field-label">Document type</label><select className="field-input" value={form.documentType} onChange={(event) => setForm({ ...form, documentType: event.target.value })}>{['ID_PROOF', 'POLICE_VERIFICATION', 'EMPLOYMENT_LETTER', 'TRAINING_CERTIFICATE', 'OTHER'].map((type) => <option key={type} value={type}>{friendlyType(type)}</option>)}</select></div><div><label className="field-label">Document name</label><input className="field-input" value={form.documentName} onChange={(event) => setForm({ ...form, documentName: event.target.value })} placeholder="Police verification certificate" required /></div><div><label className="field-label">Expiry date (optional)</label><input className="field-input" type="date" value={form.expiryDate} onChange={(event) => setForm({ ...form, expiryDate: event.target.value })} /></div><div><label className="field-label">File</label><input ref={input} className="field-input" type="file" accept="image/jpeg,image/png,image/webp,application/pdf" onChange={(event) => setFile(event.target.files?.[0] || null)} required /></div><div className="sm:col-span-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}><Upload className="h-4 w-4" /> {busy ? 'Uploading…' : 'Upload document'}</button></div></form></Modal>
  </>;
}
