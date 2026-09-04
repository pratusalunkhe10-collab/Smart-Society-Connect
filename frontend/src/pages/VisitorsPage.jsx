import { Check, Download, LogIn, LogOut, Plus, QrCode, Search, ShieldCheck, X } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import QRCode from 'react-qr-code';
import Badge from '../components/ui/Badge.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import Modal from '../components/ui/Modal.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { societyService } from '../services/societyService.js';
import { downloadCsv, formatDateTime } from '../utils/format.js';
import useLiveRefresh from '../hooks/useLiveRefresh.js';

const emptyForm = {
  residentId: '', visitorName: '', mobile: '', gender: 'MALE', age: '', address: '',
  idProofType: 'AADHAAR', idProofNumber: '', photoUrl: '', vehicleNumber: '',
  purpose: 'Family visit', visitDate: new Date().toISOString().slice(0, 10), expectedTime: '', remarks: '',
  residentName: '', flat: '',
};

export default function VisitorsPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('ALL');
  const [visitDate, setVisitDate] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [qrVisitor, setQrVisitor] = useState(null);
  const [details, setDetails] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [visitorPhoto, setVisitorPhoto] = useState(null);
  const [residentLookup, setResidentLookup] = useState('');
  const photoInput = useRef(null);

  const load = async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try {
      if (user?.role === 'RESIDENT') {
        const resident = await societyService.getResidentForUser(user.id);
        setItems(await societyService.listVisitors(resident.id));
      } else if (visitDate) {
        setItems(await societyService.listVisitorsByDate(visitDate));
      } else if (status !== 'ALL') {
        setItems(await societyService.listVisitorsByStatus(status === 'PENDING' ? 'REQUESTED' : status));
      } else {
        setItems(await societyService.listVisitors());
      }
    }
    catch (error) { push(error.message || 'Unable to load visitors.', 'error'); }
    finally { if (!silent) setLoading(false); }
  };

  useEffect(() => { load(); }, [user?.id, user?.role, status, visitDate]);
  useLiveRefresh(() => load({ silent: true }), {
    enabled: Boolean(user?.id),
    refreshKey: `${user?.id}-${user?.role}-${status}-${visitDate}-visitors`,
  });

  const filtered = useMemo(() => items.filter((item) => {
    const text = `${item.name} ${item.mobile} ${item.flat} ${item.residentName} ${item.purpose}`.toLowerCase();
    return text.includes(query.toLowerCase()) && (status === 'ALL' || item.status === status);
  }), [items, query, status]);

  const openCreate = async () => {
    let nextForm = { ...emptyForm };
    if (user?.role === 'RESIDENT') {
      try {
        const resident = await societyService.getResidentForUser(user.id);
        nextForm = { ...nextForm, residentId: String(resident.id), residentName: resident.name, flat: `${resident.wing || ''}-${resident.flatNo || ''}`.replace(/^-|-$/g, '') };
      } catch (error) {
        push(error.message || 'Unable to find the resident profile for this account.', 'error');
        return;
      }
    }
    setForm(nextForm);
    setResidentLookup(nextForm.residentName ? `${nextForm.residentName}${nextForm.flat ? ` · ${nextForm.flat}` : ''}` : '');
    setVisitorPhoto(null);
    if (photoInput.current) photoInput.current.value = '';
    setFormOpen(true);
  };

  const resolveResident = async () => {
    if (!form.residentId) { setResidentLookup(''); return; }
    try {
      const resident = await societyService.getResident(form.residentId);
      const flat = `${resident.wing || ''}-${resident.flatNo || ''}`.replace(/^-|-$/g, '');
      setForm((current) => ({ ...current, residentName: resident.name, flat }));
      setResidentLookup(`${resident.name}${flat ? ` · Flat ${flat}` : ''}`);
    } catch (error) {
      setForm((current) => ({ ...current, residentName: '', flat: '' }));
      setResidentLookup(error.response?.status === 404
        ? 'Resident not found'
        : error.message || 'Unable to verify resident');
    }
  };

  const submit = async (event) => {
    event.preventDefault(); setBusyId('create');
    try {
      const createdVisitor = await societyService.createVisitor({
        ...form,
        residentId: Number(form.residentId),
        age: Number(form.age),
        expectedTime: form.expectedTime || null,
        photoUrl: null,
        vehicleNumber: form.vehicleNumber || null,
        remarks: form.remarks || null,
      });
      if (visitorPhoto) await societyService.uploadVisitorPhoto(createdVisitor.id, visitorPhoto);
      push('Visitor request created. Resident approval is pending.');
      setFormOpen(false); await load();
    } catch (error) { push(error.message || 'Unable to create visitor request.', 'error'); }
    finally { setBusyId(null); }
  };

  const changeStatus = async (item, nextStatus) => {
    setBusyId(item.id);
    const updates = { status: nextStatus };
    if (nextStatus === 'CHECKED_IN') updates.checkIn = new Date().toISOString();
    if (nextStatus === 'CHECKED_OUT') updates.checkOut = new Date().toISOString();
    try { await societyService.updateVisitor(item.id, updates); push(`Visitor status changed to ${nextStatus.replace('_', ' ')}.`); await load(); }
    catch (error) { push(error.message || 'Unable to update visitor.', 'error'); }
    finally { setBusyId(null); }
  };

  const openDetails = async (item) => {
    setBusyId('details');
    try {
      const visitor = await societyService.getVisitor(item.id);
      if (visitor.photoUrl) {
        const photo = await societyService.getVisitorPhoto(item.id);
        visitor.photoPreviewUrl = URL.createObjectURL(new Blob([photo.data], { type: photo.headers['content-type'] || 'image/jpeg' }));
      }
      setDetails(visitor);
    }
    catch (error) { push(error.message || 'Unable to load visitor details.', 'error'); }
    finally { setBusyId(null); }
  };

  const canApprove = ['ADMIN', 'RESIDENT', 'SECRETARY'].includes(user?.role);
  const canGate = ['ADMIN', 'SECURITY'].includes(user?.role);

  return (
    <div>
      <PageHeader eyebrow="Visitor service" title="Visitor management" subtitle="Register visitors, manage resident approvals, issue QR passes and record gate entry or exit." actions={<><button className="btn-secondary" onClick={() => downloadCsv('visitor-log.csv', filtered)}><Download className="h-4 w-4" /> Export log</button><button className="btn-primary" onClick={openCreate}><Plus className="h-4 w-4" /> New visitor</button></>} />

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Pending approval', items.filter((item) => item.status === 'PENDING').length, 'bg-amber-50 text-amber-700'],
          ['Approved', items.filter((item) => item.status === 'APPROVED').length, 'bg-emerald-50 text-emerald-700'],
          ['Inside society', items.filter((item) => item.status === 'CHECKED_IN').length, 'bg-blue-50 text-blue-700'],
          ['Completed visits', items.filter((item) => item.status === 'CHECKED_OUT').length, 'bg-slate-100 text-slate-700'],
        ].map(([label, value, tone]) => <div key={label} className="panel flex items-center gap-4 p-4"><div className={`grid h-11 w-11 place-items-center rounded-xl ${tone}`}><ShieldCheck className="h-5 w-5" /></div><div><p className="text-2xl font-extrabold text-slate-900">{value}</p><p className="text-xs font-semibold text-slate-500">{label}</p></div></div>)}
      </section>

      <section className="panel mt-6 overflow-hidden">
        <div className="panel-header">
          <div><h2 className="font-extrabold text-slate-900">Visitor log</h2><p className="text-xs text-slate-500">Approval and gate activity across all entries</p></div>
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row">
            <label className="relative block sm:w-64"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><input className="field-input pl-9" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search visitor or flat" /></label>
            <input className="field-input sm:w-40" type="date" value={visitDate} onChange={(event) => setVisitDate(event.target.value)} aria-label="Filter by visit date" />
            <select className="field-input sm:w-44" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All status</option><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option><option value="CHECKED_IN">Checked in</option><option value="CHECKED_OUT">Checked out</option></select>
          </div>
        </div>

        {loading ? <div className="p-5"><LoadingState /></div> : filtered.length === 0 ? <EmptyState /> : (
          <div className="table-responsive">
            <table className="data-table">
              <thead><tr><th>Visitor</th><th>Visiting</th><th>Purpose</th><th>Entry / Exit</th><th>Status</th><th className="text-right">Actions</th></tr></thead>
              <tbody>{filtered.map((item) => (
                <tr key={item.id}>
                  <td><p className="font-bold text-slate-800">{item.name}</p><p className="mt-0.5 text-xs text-slate-400">{item.mobile}{item.vehicleNo ? ` · ${item.vehicleNo}` : ''}</p></td>
                  <td><p className="font-semibold text-slate-700">{item.flat}</p><p className="mt-0.5 text-xs text-slate-400">{item.residentName}</p></td>
                  <td>{item.purpose}</td>
                  <td><p className="text-xs"><span className="font-semibold text-slate-600">In:</span> {formatDateTime(item.checkIn)}</p><p className="mt-1 text-xs"><span className="font-semibold text-slate-600">Out:</span> {formatDateTime(item.checkOut)}</p></td>
                  <td><Badge value={item.status} /></td>
                  <td><div className="flex justify-end gap-1"><button onClick={() => openDetails(item)} className="rounded-lg px-2 py-1 text-xs font-bold text-slate-600 hover:bg-slate-100">Details</button>
                    {canApprove && item.status === 'PENDING' && <><button disabled={busyId === item.id} onClick={() => changeStatus(item, 'APPROVED')} className="rounded-lg p-2 text-emerald-700 hover:bg-emerald-50" title="Approve"><Check className="h-4 w-4" /></button><button disabled={busyId === item.id} onClick={() => changeStatus(item, 'REJECTED')} className="rounded-lg p-2 text-rose-700 hover:bg-rose-50" title="Reject"><X className="h-4 w-4" /></button></>}
                    {['APPROVED', 'CHECKED_IN', 'CHECKED_OUT'].includes(item.status) && <button onClick={() => setQrVisitor(item)} className="rounded-lg p-2 text-violet-700 hover:bg-violet-50" title="Show QR pass"><QrCode className="h-4 w-4" /></button>}
                    {canGate && item.status === 'APPROVED' && <button disabled={busyId === item.id} onClick={() => changeStatus(item, 'CHECKED_IN')} className="rounded-lg p-2 text-blue-700 hover:bg-blue-50" title="Check in"><LogIn className="h-4 w-4" /></button>}
                    {canGate && item.status === 'CHECKED_IN' && <button disabled={busyId === item.id} onClick={() => changeStatus(item, 'CHECKED_OUT')} className="rounded-lg p-2 text-slate-700 hover:bg-slate-100" title="Check out"><LogOut className="h-4 w-4" /></button>}
                  </div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title="Register visitor" description="Create a visitor request for resident approval.">
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          <div><label className="field-label">Visitor name</label><input className="field-input" value={form.visitorName} onChange={(event) => setForm({ ...form, visitorName: event.target.value })} required /></div>
          <div><label className="field-label">Mobile</label><input className="field-input" value={form.mobile} onChange={(event) => setForm({ ...form, mobile: event.target.value })} pattern="[0-9]{10}" required /></div>
          <div><label className="field-label">Resident ID</label><input className="field-input" type="number" min="1" value={form.residentId} onChange={(event) => { setForm({ ...form, residentId: event.target.value, residentName: '', flat: '' }); setResidentLookup(''); }} onBlur={resolveResident} readOnly={user?.role === 'RESIDENT'} required />{residentLookup && <p className={`mt-1 text-xs font-semibold ${form.residentName ? 'text-emerald-600' : 'text-rose-600'}`}>{residentLookup}</p>}</div>
          <div><label className="field-label">Visit date</label><input className="field-input" type="date" value={form.visitDate} onChange={(event) => setForm({ ...form, visitDate: event.target.value })} required /></div>
          <div><label className="field-label">Gender</label><select className="field-input" value={form.gender} onChange={(event) => setForm({ ...form, gender: event.target.value })}><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></div>
          <div><label className="field-label">Age</label><input className="field-input" type="number" min="1" max="120" value={form.age} onChange={(event) => setForm({ ...form, age: event.target.value })} required /></div>
          <div className="sm:col-span-2"><label className="field-label">Address</label><input className="field-input" value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} required /></div>
          <div><label className="field-label">ID proof type</label><select className="field-input" value={form.idProofType} onChange={(event) => setForm({ ...form, idProofType: event.target.value })}><option value="AADHAAR">Aadhaar</option><option value="PAN">PAN</option><option value="PASSPORT">Passport</option><option value="DRIVING_LICENSE">Driving licence</option><option value="VOTER_ID">Voter ID</option><option value="OTHER">Other</option></select></div>
          <div><label className="field-label">ID proof number</label><input className="field-input" value={form.idProofNumber} onChange={(event) => setForm({ ...form, idProofNumber: event.target.value })} required /></div>
          <div><label className="field-label">Purpose</label><select className="field-input" value={form.purpose} onChange={(event) => setForm({ ...form, purpose: event.target.value })}><option>Family visit</option><option>Delivery</option><option>Maintenance</option><option>Domestic help</option><option>Cab / Driver</option><option>Other</option></select></div>
          <div><label className="field-label">Expected time (optional)</label><input className="field-input" type="time" value={form.expectedTime} onChange={(event) => setForm({ ...form, expectedTime: event.target.value })} /></div>
          <div><label className="field-label">Vehicle number (optional)</label><input className="field-input" value={form.vehicleNumber} onChange={(event) => setForm({ ...form, vehicleNumber: event.target.value.toUpperCase() })} /></div>
          <div><label className="field-label">Visitor photo (optional)</label><input ref={photoInput} className="field-input" type="file" accept="image/jpeg,image/png,image/webp" capture="environment" onChange={(event) => setVisitorPhoto(event.target.files?.[0] || null)} /><p className="mt-1 text-xs text-slate-500">{visitorPhoto ? `${visitorPhoto.name} selected` : 'Use the camera or select a photo. ID-document scans are not stored.'}</p></div>
          <div className="sm:col-span-2"><label className="field-label">Remarks (optional)</label><input className="field-input" value={form.remarks} onChange={(event) => setForm({ ...form, remarks: event.target.value })} /></div>
          <div className="sm:col-span-2 mt-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setFormOpen(false)}>Cancel</button><button className="btn-primary" disabled={busyId === 'create'}>{busyId === 'create' ? 'Creating…' : 'Create request'}</button></div>
        </form>
      </Modal>

      <Modal open={Boolean(qrVisitor)} onClose={() => setQrVisitor(null)} title="Visitor QR pass" description="Security can scan or verify this pass at the gate." maxWidth="max-w-md">
        {qrVisitor && <div className="text-center"><div className="mb-4 inline-flex rounded-full bg-emerald-100 px-3 py-1 text-xs font-extrabold uppercase tracking-wider text-emerald-700">Access {qrVisitor.status === 'APPROVED' ? 'approved' : qrVisitor.status.replace('_', ' ').toLowerCase()}</div><div className="mx-auto w-fit rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><QRCode value={`SMART SOCIETY CONNECT\nVISITOR ACCESS PASS\nVisitor: ${qrVisitor.name}\nFlat: ${qrVisitor.flat}\nStatus: ${qrVisitor.status}\nPass code: ${qrVisitor.passCode}\nPresent this pass at the security gate.`} size={210} /></div><p className="mt-5 text-lg font-extrabold text-slate-900">{qrVisitor.name}</p><p className="mt-1 text-sm text-slate-500">Visiting {qrVisitor.flat} · {qrVisitor.residentName}</p><div className="mt-4 inline-flex rounded-xl bg-slate-950 px-4 py-2 font-mono text-sm font-bold tracking-wider text-white">{qrVisitor.passCode}</div><p className="mt-3 text-xs text-slate-500">Present this pass to security for confirmation.</p></div>}
      </Modal>
      <Modal open={Boolean(details)} onClose={() => { if (details?.photoPreviewUrl) URL.revokeObjectURL(details.photoPreviewUrl); setDetails(null); }} title={details?.name || 'Visitor details'} description="Current visitor record retrieved from the backend.">{details && <div className="space-y-3 rounded-xl bg-slate-50 p-4 text-sm">{details.photoPreviewUrl && <img src={details.photoPreviewUrl} alt={`${details.name} visitor`} className="mx-auto h-36 w-36 rounded-2xl object-cover" />}{[['Mobile', details.mobile], ['Purpose', details.purpose], ['Resident', details.residentName], ['Flat', details.flat], ['Status', details.status], ['Visit date', details.visitDate], ['Expected time', details.expectedTime || '—'], ['Vehicle', details.vehicleNo || '—'], ['Address', details.address || '—']].map(([label, value]) => <div key={label}><span className="font-bold text-slate-500">{label}: </span><span className="text-slate-800">{value}</span></div>)}</div>}</Modal>
    </div>
  );
}
