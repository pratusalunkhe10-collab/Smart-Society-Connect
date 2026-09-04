import { Eye, EyeOff, FileText, Plus, Trash2, Upload } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { societyService } from '../../services/societyService.js';
import { formatCurrency, formatDate, formatDateTime } from '../../utils/format.js';
import Badge from '../ui/Badge.jsx';
import EmptyState from '../ui/EmptyState.jsx';
import LoadingState from '../ui/LoadingState.jsx';
import Modal from '../ui/Modal.jsx';
import PageHeader from '../ui/PageHeader.jsx';
import useLiveRefresh from '../../hooks/useLiveRefresh.js';
import { prepareFileViewer, showBlobInViewer } from '../../utils/fileViewer.js';

const categories = ['UTILITY_BILL', 'VENDOR_INVOICE', 'INSURANCE', 'LEGAL', 'NOTICE', 'OTHER'];
const initialForm = { category: 'UTILITY_BILL', title: '', vendor: '', billingMonth: '', amount: '', dueDate: '', paidDate: '', published: false };
const displayCategory = (category) => String(category || 'OTHER').replaceAll('_', ' ');

export default function SocietyDocumentsPanel() {
  const { user } = useAuth();
  const { push } = useToast();
  const fileInput = useRef(null);
  const canManage = ['ADMIN', 'SECRETARY'].includes(user?.role);
  const canUpload = canManage || user?.role === 'ACCOUNTANT';
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [file, setFile] = useState(null);
  const [form, setForm] = useState(initialForm);

  const load = async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try { setItems(await societyService.listSocietyDocuments()); }
    catch (error) { push(error.message || 'Unable to load society documents.', 'error'); }
    finally { if (!silent) setLoading(false); }
  };

  useEffect(() => { load(); }, [user?.role]);
  useLiveRefresh(() => load({ silent: true }), {
    enabled: Boolean(user?.id),
    refreshKey: `${user?.id}-${user?.role}-society-documents`,
  });

  const upload = async (event) => {
    event.preventDefault();
    if (!file) { push('Choose a PDF or image file to upload.', 'error'); return; }
    setBusy(true);
    try {
      await societyService.uploadSocietyDocument(form, file);
      push(form.published ? 'Society document uploaded and published.' : 'Society document uploaded as private.');
      setOpen(false); setFile(null); setForm(initialForm);
      if (fileInput.current) fileInput.current.value = '';
      await load();
    } catch (error) { push(error.message || 'Unable to upload society document.', 'error'); }
    finally { setBusy(false); }
  };

  const view = async (item) => {
    const viewer = prepareFileViewer();
    setBusy(true);
    try {
      const response = await societyService.getSocietyDocumentFile(item.id);
      showBlobInViewer(response, viewer);
    } catch (error) {
      viewer?.close();
      push(error.message || 'Unable to open this document.', 'error');
    }
    finally { setBusy(false); }
  };

  const setVisibility = async (item) => {
    setBusy(true);
    try {
      await societyService.publishSocietyDocument(item.id, !item.published);
      push(item.published ? 'Document is now private.' : 'Document published for residents.');
      await load();
    } catch (error) { push(error.message || 'Unable to update document visibility.', 'error'); }
    finally { setBusy(false); }
  };

  const remove = async (item) => {
    if (!window.confirm(`Delete “${item.title}”? This also removes its stored file.`)) return;
    setBusy(true);
    try { await societyService.deleteSocietyDocument(item.id); push('Society document deleted.'); await load(); }
    catch (error) { push(error.message || 'Unable to delete society document.', 'error'); }
    finally { setBusy(false); }
  };

  const availableCategories = user?.role === 'ACCOUNTANT' ? ['UTILITY_BILL', 'VENDOR_INVOICE'] : categories;
  const filtered = items.filter((item) => {
    const query = search.trim().toLowerCase();
    return !query || [item.title, item.vendor, item.category, item.billingMonth].some((value) => String(value || '').toLowerCase().includes(query));
  });
  const accessMessage = canManage ? 'Manage society bills, records, notices, and resident-visible documents.' : canUpload ? 'Upload utility bills and vendor invoices. Published records are visible to residents.' : 'Browse notices and society records that the management team has published.';

  return <>
    <PageHeader eyebrow="Society records" title="Society documents" subtitle={accessMessage} actions={canUpload && <button type="button" className="btn-primary" onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> Upload document</button>} />
    <section className="panel overflow-hidden">
      <div className="panel-header">
        <div><h2 className="font-extrabold text-slate-900">Society document register</h2><p className="text-xs text-slate-500">{filtered.length} of {items.length} records shown</p></div>
        <input className="field-input w-full sm:w-72" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search title, vendor, or category" />
      </div>
      {loading ? <div className="p-6"><LoadingState /></div> : filtered.length === 0 ? <EmptyState title="No society documents found" description={canUpload ? 'Upload the first utility bill, vendor invoice, notice, or society record.' : 'Published society records will appear here when the management team shares them.'} /> : <div className="table-responsive"><table className="data-table"><thead><tr><th>Document</th><th>Vendor / period</th><th>Amount / due date</th><th>Visibility</th><th>Uploaded</th><th className="text-right">Actions</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td><p className="font-bold text-slate-800">{item.title}</p><p className="mt-0.5 text-xs text-slate-500">{displayCategory(item.category)}</p></td><td><p className="font-medium text-slate-800">{item.vendor || '—'}</p><p className="text-xs text-slate-500">{item.billingMonth || 'No billing period'}</p></td><td><p className="font-medium text-slate-800">{item.amount ? formatCurrency(item.amount) : '—'}</p><p className="text-xs text-slate-500">{item.dueDate ? `Due ${formatDate(item.dueDate)}` : 'No due date'}</p></td><td><Badge value={item.published ? 'Published' : 'Private'} /></td><td>{formatDateTime(item.createdAt)}</td><td><div className="flex justify-end gap-1"><button type="button" disabled={busy} className="rounded-lg p-2 text-slate-500 hover:bg-emerald-50 hover:text-emerald-700" onClick={() => view(item)} title="View document"><FileText className="h-4 w-4" /></button>{canManage && <button type="button" disabled={busy} className="rounded-lg p-2 text-slate-500 hover:bg-blue-50 hover:text-blue-700" onClick={() => setVisibility(item)} title={item.published ? 'Make private' : 'Publish for residents'}>{item.published ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</button>}{canManage && <button type="button" disabled={busy} className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700" onClick={() => remove(item)} title="Delete document"><Trash2 className="h-4 w-4" /></button>}</div></td></tr>)}</tbody></table></div>}
    </section>
    <Modal open={open} onClose={() => setOpen(false)} title="Upload society document" description="Add a utility bill, vendor invoice, certificate, notice, or other society record."><form onSubmit={upload} className="grid gap-4 sm:grid-cols-2"><div><label className="field-label">Category</label><select className="field-input" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}>{availableCategories.map((category) => <option key={category} value={category}>{displayCategory(category)}</option>)}</select></div><div><label className="field-label">Document title</label><input className="field-input" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="July electricity bill" required /></div><div><label className="field-label">Vendor / provider</label><input className="field-input" value={form.vendor} onChange={(event) => setForm({ ...form, vendor: event.target.value })} placeholder="MSEDCL" /></div><div><label className="field-label">Amount</label><input className="field-input" type="number" min="0" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} placeholder="0" /></div><div><label className="field-label">Billing month</label><input className="field-input" value={form.billingMonth} onChange={(event) => setForm({ ...form, billingMonth: event.target.value })} placeholder="July 2026" /></div><div><label className="field-label">Due date</label><input className="field-input" type="date" value={form.dueDate} onChange={(event) => setForm({ ...form, dueDate: event.target.value })} /></div><div className="sm:col-span-2"><label className="field-label">File</label><input ref={fileInput} className="field-input" type="file" accept="image/jpeg,image/png,image/webp,application/pdf" onChange={(event) => setFile(event.target.files?.[0] || null)} required /><p className="mt-1 text-xs text-slate-500">PDF, JPG, PNG, or WEBP · {file?.name || 'No file selected'}</p></div>{canManage && <label className="sm:col-span-2 flex items-center gap-2 text-sm font-semibold text-slate-700"><input type="checkbox" checked={form.published} onChange={(event) => setForm({ ...form, published: event.target.checked })} /> Publish for residents and Security staff</label>}<div className="sm:col-span-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}><Upload className="h-4 w-4" /> {busy ? 'Uploading…' : 'Upload document'}</button></div></form></Modal>
  </>;
}
