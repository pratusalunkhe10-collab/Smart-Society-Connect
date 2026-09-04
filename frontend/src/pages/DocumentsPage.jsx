import { Check, Eye, FileText, Plus, Trash2, Upload, X } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
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
import SocietyDocumentsPanel from '../components/documents/SocietyDocumentsPanel.jsx';
import StaffDocumentsPanel from '../components/documents/StaffDocumentsPanel.jsx';
import useLiveRefresh from '../hooks/useLiveRefresh.js';

const documentTypes = ['AADHAAR', 'PAN', 'PROOF_OF_ADDRESS', 'OWNERSHIP', 'RENT_AGREEMENT', 'PROFILE_PHOTO', 'OTHER'];
const emptyUpload = { residentId: '', documentType: 'AADHAAR', documentName: '' };

export default function DocumentsPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const fileInput = useRef(null);
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('ALL');
  const [search, setSearch] = useState('');
  const [uploadOpen, setUploadOpen] = useState(false);
  const [upload, setUpload] = useState(emptyUpload);
  const [uploadResidentName, setUploadResidentName] = useState('');
  const [file, setFile] = useState(null);
  const [selected, setSelected] = useState(null);
  const [preview, setPreview] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [documentArea, setDocumentArea] = useState('resident');

  const isReviewer = ['ADMIN', 'SECRETARY'].includes(user?.role);
  const canAccessResidentDocuments = isReviewer || user?.role === 'RESIDENT';
  const documentAreas = isReviewer
    ? [['resident', 'Resident documents'], ['society', 'Society records'], ['staff', 'Staff & security']]
    : user?.role === 'RESIDENT'
      ? [['resident', 'My documents'], ['society', 'Society records']]
      : user?.role === 'SECURITY'
        ? [['society', 'Society records'], ['staff', 'My staff documents']]
        : [['society', 'Society records']];

  const load = async ({ silent = false } = {}) => {
    if (!canAccessResidentDocuments) {
      setDocuments([]);
      setLoading(false);
      return;
    }
    if (!silent) setLoading(true);
    try {
      if (isReviewer) setDocuments(status === 'ALL' ? await societyService.listAllDocuments() : await societyService.listDocumentsByStatus(status));
      else {
        const resident = await societyService.getResidentForUser(user.id);
        setDocuments(await societyService.listResidentDocuments(resident.id));
      }
    } catch (error) {
      push(error.message || 'Unable to load documents.', 'error');
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => { if (user?.id) load(); }, [user?.id, user?.role, status]);
  useLiveRefresh(() => load({ silent: true }), {
    enabled: Boolean(user?.id && canAccessResidentDocuments && documentArea === 'resident'),
    refreshKey: `${user?.id}-${user?.role}-${status}-${documentArea}`,
  });

  useEffect(() => {
    if (!documentAreas.some(([id]) => id === documentArea)) setDocumentArea(documentAreas[0][0]);
  }, [user?.role]);

  useEffect(() => () => {
    if (preview?.url) URL.revokeObjectURL(preview.url);
  }, [preview]);

  const openUpload = async () => {
    if (!canAccessResidentDocuments) return;
    let residentId = '';
    if (!isReviewer) {
      try { residentId = String((await societyService.getResidentForUser(user.id)).id); }
      catch (error) { push(error.message || 'Your resident profile is required before uploading documents.', 'error'); return; }
    }
    setUpload({ ...emptyUpload, residentId });
    setUploadResidentName('');
    setFile(null);
    if (fileInput.current) fileInput.current.value = '';
    setUploadOpen(true);
  };

  const resolveUploadResident = async () => {
    if (!upload.residentId) { setUploadResidentName(''); return; }
    try {
      const resident = await societyService.getResident(upload.residentId);
      const flat = `${resident.wing || ''}-${resident.flatNo || ''}`.replace(/^-|-$/g, '');
      setUploadResidentName(`${resident.name}${flat ? ` · Flat ${flat}` : ''}`);
    } catch { setUploadResidentName('Resident not found'); }
  };

  const submitUpload = async (event) => {
    event.preventDefault();
    if (!file) { push('Choose a document file to upload.', 'error'); return; }
    setBusy(true);
    try {
      await societyService.uploadResidentDocument({ ...upload, residentId: Number(upload.residentId) }, file);
      push('Document uploaded and submitted for verification.');
      setUploadOpen(false);
      await load();
    } catch (error) {
      push(error.message || 'Unable to upload document.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const review = async (document, action) => {
    setBusy(true);
    try {
      const id = document.residentDocId;
      if (action === 'verify') await societyService.verifyResidentDocument(id, { verifierUserId: user.id });
      else await societyService.rejectResidentDocument(id, { verifierUserId: user.id });
      push(action === 'verify' ? 'Document verified.' : 'Document rejected.');
      await load();
    } catch (error) {
      push(error.message || 'Unable to update document verification.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const openDetails = async (document) => {
    setBusy(true);
    try { setSelected(await societyService.getResidentDocument(document.residentDocId)); }
    catch (error) { push(error.message || 'Unable to load document details.', 'error'); }
    finally { setBusy(false); }
  };

  const previewFile = async (document) => {
    setBusy(true);
    try {
      const response = await societyService.getResidentDocumentFile(document.residentDocId);
      const mimeType = response.headers['content-type'] || document.mimeType || 'application/octet-stream';
      const url = URL.createObjectURL(new Blob([response.data], { type: mimeType }));
      setPreview((current) => {
        if (current?.url) URL.revokeObjectURL(current.url);
        return { url, mimeType, name: document.documentName };
      });
    } catch (error) {
      push(error.message || 'Unable to open this document file.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const closePreview = () => {
    setPreview((current) => {
      if (current?.url) URL.revokeObjectURL(current.url);
      return null;
    });
  };

  const filteredDocuments = documents.filter((document) => {
    const query = search.trim().toLowerCase();
    if (!query || !isReviewer) return true;
    return [
      document.residentId,
      document.residentName,
      document.documentName,
      document.documentType,
    ].some((value) => String(value || '').toLowerCase().includes(query));
  });

  const confirmDelete = async () => {
    setBusy(true);
    try {
      await societyService.deleteResidentDocument(deleting.residentDocId);
      push('Document deleted.');
      setDeleting(null);
      await load();
    } catch (error) {
      push(error.message || 'Unable to delete document.', 'error');
    } finally {
      setBusy(false);
    }
  };

  return <div>
    <div className="mb-6 flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
      {documentAreas.map(([id, label]) => <button key={id} type="button" onClick={() => setDocumentArea(id)} className={`rounded-xl px-4 py-2.5 text-sm font-bold transition ${documentArea === id ? 'bg-rose-500 text-white shadow-md shadow-rose-500/25' : 'text-slate-600 hover:bg-slate-100'}`}>{label}</button>)}
    </div>
    {documentArea === 'society' && <SocietyDocumentsPanel />}
    {documentArea === 'staff' && <StaffDocumentsPanel />}
    {documentArea === 'resident' && canAccessResidentDocuments && <>
    <PageHeader eyebrow="Resident documents" title={isReviewer ? 'Document verification' : 'My documents'} subtitle={isReviewer ? 'Review resident proof documents and maintain a clear verification audit.' : 'Upload required residence documents and track their verification status.'} actions={<button className="btn-primary" onClick={openUpload}><Plus className="h-4 w-4" /> Upload document</button>} />
    <section className="panel overflow-hidden">
      <div className="panel-header">{isReviewer ? <div className="flex w-full flex-col gap-3 lg:flex-row lg:items-center lg:justify-between"><div><h2 className="font-extrabold text-slate-900">Document register</h2><p className="text-xs text-slate-500">{filteredDocuments.length} of {documents.length} document records shown</p></div><div className="flex flex-col gap-2 sm:flex-row"><input className="field-input w-full sm:w-72" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search resident ID, name, PAN..." /><select className="field-input w-full sm:w-36" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All statuses</option><option value="PENDING">Pending</option><option value="VERIFIED">Verified</option><option value="REJECTED">Rejected</option></select></div></div> : <div><h2 className="font-extrabold text-slate-900">Uploaded documents</h2><p className="text-xs text-slate-500">{documents.length} document records shown</p></div>}</div>
      {loading ? <div className="p-6"><LoadingState /></div> : filteredDocuments.length === 0 ? <EmptyState title="No documents found" description={isReviewer ? 'Try a different resident, document type, or status.' : 'Upload a document to begin verification.'} /> : <div className="table-responsive"><table className="data-table"><thead><tr><th>Document</th><th>Resident</th><th>File</th><th>Uploaded</th><th>Status</th><th className="text-right">Actions</th></tr></thead><tbody>{filteredDocuments.map((document) => <tr key={document.residentDocId}><td><p className="font-bold text-slate-800">{document.documentName}</p><p className="mt-0.5 text-xs text-slate-500">{document.documentType}</p></td><td><p className="font-medium text-slate-800">{document.residentName || `Resident #${document.residentId}`}</p><p className="text-xs text-slate-500">ID #{document.residentId}</p></td><td><p className="max-w-44 truncate text-sm">{document.filePath || 'Stored file'}</p><p className="text-xs text-slate-400">{document.mimeType || 'Unknown type'}</p></td><td>{formatDateTime(document.createdAt)}</td><td><Badge value={document.verificationStatus} /></td><td><div className="flex justify-end gap-1"><button type="button" disabled={busy} onClick={() => previewFile(document)} className="rounded-lg p-2 text-slate-500 hover:bg-emerald-50 hover:text-emerald-700" title="View document"><FileText className="h-4 w-4" /></button><button type="button" onClick={() => openDetails(document)} className="rounded-lg p-2 text-slate-500 hover:bg-blue-50 hover:text-blue-700" title="Document details"><Eye className="h-4 w-4" /></button>{isReviewer && document.verificationStatus === 'PENDING' && <><button type="button" disabled={busy} onClick={() => review(document, 'verify')} className="rounded-lg p-2 text-emerald-700 hover:bg-emerald-50" title="Verify"><Check className="h-4 w-4" /></button><button type="button" disabled={busy} onClick={() => review(document, 'reject')} className="rounded-lg p-2 text-rose-700 hover:bg-rose-50" title="Reject"><X className="h-4 w-4" /></button></>}<button type="button" disabled={busy} onClick={() => setDeleting(document)} className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700" title="Delete"><Trash2 className="h-4 w-4" /></button></div></td></tr>)}</tbody></table></div>}
    </section>
    <Modal open={uploadOpen} onClose={() => setUploadOpen(false)} title="Upload resident document" description="Files are submitted as multipart data and then reviewed by the society team."><form onSubmit={submitUpload} className="grid gap-4 sm:grid-cols-2">{isReviewer && <div className="sm:col-span-2"><label className="field-label">Resident ID</label><input className="field-input" type="number" min="1" value={upload.residentId} onChange={(event) => { setUpload({ ...upload, residentId: event.target.value }); setUploadResidentName(''); }} onBlur={resolveUploadResident} required />{uploadResidentName && <p className={`mt-1 text-xs font-semibold ${uploadResidentName === 'Resident not found' ? 'text-rose-600' : 'text-emerald-600'}`}>{uploadResidentName}</p>}</div>}<div><label className="field-label">Document type</label><select className="field-input" value={upload.documentType} onChange={(event) => setUpload({ ...upload, documentType: event.target.value })}>{documentTypes.map((type) => <option key={type}>{type}</option>)}</select></div><div><label className="field-label">Document name</label><input className="field-input" value={upload.documentName} onChange={(event) => setUpload({ ...upload, documentName: event.target.value })} placeholder="Aadhaar card" required /></div><div className="sm:col-span-2"><label className="field-label">File</label><input ref={fileInput} className="field-input" type="file" onChange={(event) => setFile(event.target.files?.[0] || null)} required /><p className="mt-1 text-xs text-slate-500">Selected: {file?.name || 'No file selected'}</p></div><div className="sm:col-span-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setUploadOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}><Upload className="h-4 w-4" /> {busy ? 'Uploading…' : 'Upload document'}</button></div></form></Modal>
    <Modal open={Boolean(selected)} onClose={() => setSelected(null)} title="Document details" description="Metadata returned by the document API."><div className="space-y-3 rounded-xl bg-slate-50 p-4 text-sm">{selected && [['Document', selected.documentName], ['Type', selected.documentType], ['Resident ID', selected.residentId], ['File type', selected.mimeType], ['File size', selected.fileSize ? `${selected.fileSize} bytes` : '—'], ['Status', selected.verificationStatus], ['Uploaded', formatDateTime(selected.createdAt)], ['Verified', selected.verifiedAt ? formatDateTime(selected.verifiedAt) : '—']].map(([label, value]) => <div key={label} className="flex justify-between gap-4"><span className="text-slate-500">{label}</span><span className="text-right font-bold text-slate-800">{value || '—'}</span></div>)}</div></Modal>
    <Modal open={Boolean(preview)} onClose={closePreview} title={preview?.name || 'Document preview'} description="Secure preview of the uploaded file."><div className="overflow-hidden rounded-xl border border-slate-200 bg-slate-50">{preview?.mimeType?.startsWith('image/') ? <img src={preview.url} alt={preview.name} className="max-h-[70vh] w-full object-contain" /> : preview?.mimeType === 'application/pdf' ? <iframe src={preview.url} title={preview.name} className="h-[70vh] w-full" /> : <div className="p-6 text-center"><FileText className="mx-auto mb-3 h-10 w-10 text-slate-400" /><p className="text-sm text-slate-600">This file type cannot be previewed in the browser.</p><a href={preview?.url} download={preview?.name} className="btn-primary mt-4 inline-flex">Download document</a></div>}</div></Modal>
    <ConfirmDialog open={Boolean(deleting)} onClose={() => setDeleting(null)} onConfirm={confirmDelete} busy={busy} title="Delete document?" description={`This will permanently delete ${deleting?.documentName || 'this document'} and its stored file.`} />
    </>}
  </div>;
}
