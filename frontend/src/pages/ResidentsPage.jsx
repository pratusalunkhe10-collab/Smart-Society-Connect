import { Download, Edit3, Plus, Search, Trash2, UserPlus, UsersRound } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import Badge from '../components/ui/Badge.jsx';
import ConfirmDialog from '../components/ui/ConfirmDialog.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import Modal from '../components/ui/Modal.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { societyService } from '../services/societyService.js';
import { downloadCsv, formatDate } from '../utils/format.js';
import { API_ROUTES } from '../config/api.js';
import http, { unwrap } from '../services/http.js';
import useLiveRefresh from '../hooks/useLiveRefresh.js';

const emptyForm = {
  userId: '', flatId: '', residentType: 'OWNER', occupation: '', emergencyContact: '',
  moveInDate: new Date().toISOString().slice(0, 10), isPrimaryMember: true,
};

const emptyFamilyForm = { memberName: '', relation: 'SPOUSE', age: '', mobile: '' };
const emptyFlatForm = { wing: 'A', flatNumber: '', floorNumber: '', flatType: '2BHK', areaSqft: '' };

export default function ResidentsPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('ALL');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [movingOut, setMovingOut] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [familyOpen, setFamilyOpen] = useState(false);
  const [familyMode, setFamilyMode] = useState('view');
  const [familyResident, setFamilyResident] = useState(null);
  const [familyMembers, setFamilyMembers] = useState([]);
  const [familyLoading, setFamilyLoading] = useState(false);
  const [familyForm, setFamilyForm] = useState(emptyFamilyForm);
  const [editingFamily, setEditingFamily] = useState(null);
  const [assignmentOptions, setAssignmentOptions] = useState({ users: [], flats: [] });
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [flatModalOpen, setFlatModalOpen] = useState(false);
  const [flatForm, setFlatForm] = useState(emptyFlatForm);
  const [flatsOpen, setFlatsOpen] = useState(false);
  const [editingFlat, setEditingFlat] = useState(null);
  const [deletingFlat, setDeletingFlat] = useState(null);
  const [flatFilters, setFlatFilters] = useState({ status: 'ALL', wing: 'ALL', type: 'ALL' });

  const selfService = user?.role === 'RESIDENT';
  const canManageFamily = ['ADMIN', 'SECRETARY', 'RESIDENT'].includes(user?.role);
  const canManageAssignments = user?.role === 'ADMIN';

  const load = async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try {
      const residents = selfService
        ? [await societyService.getResidentForUser(user.id)]
        : await societyService.listResidents();
      const residentsWithFamilyCounts = await Promise.all(residents.map(async (resident) => {
        try {
          return { ...resident, familyMembers: await societyService.countFamilyMembers(resident.id) };
        } catch {
          return resident;
        }
      }));
      setItems(residentsWithFamilyCounts);
    }
    catch (error) { push(error.message || 'Unable to load residents.', 'error'); }
    finally { if (!silent) setLoading(false); }
  };

  useEffect(() => { if (user?.id) load(); }, [user?.id, user?.role]);
  useLiveRefresh(() => load({ silent: true }), {
    enabled: Boolean(user?.id),
    refreshKey: `${user?.id}-${user?.role}-residents`,
  });

  const filtered = useMemo(() => items.filter((item) => {
    const text = `${item.name} ${item.email} ${item.mobile} ${item.wing}-${item.flatNo}`.toLowerCase();
    return text.includes(query.toLowerCase()) && (status === 'ALL' || item.status === status);
  }), [items, query, status]);

  const loadAssignmentOptions = async () => {
    setAssignmentLoading(true);
    try {
      const [users, flats] = await Promise.all([
        unwrap(await http.get(API_ROUTES.adminUsers, { params: { status: 'APPROVED' } })),
        societyService.listFlats(),
      ]);
      setAssignmentOptions({ users, flats });
    } catch (error) {
      push(error.message || 'Unable to load approved users and flats.', 'error');
    } finally {
      setAssignmentLoading(false);
    }
  };

  const openCreate = async () => {
    setEditing(null);
    setForm(emptyForm);
    setModalOpen(true);
    await loadAssignmentOptions();
  };
  const openEdit = (item) => { setEditing(item); setForm({ ...item, residentType: item.residentType || item.memberType, isPrimaryMember: Boolean(item.isPrimaryMember) }); setModalOpen(true); };
  const updateField = (event) => setForm({ ...form, [event.target.name]: event.target.value });

  const submit = async (event) => {
    event.preventDefault(); setBusy(true);
    try {
      if (editing) {
        await societyService.updateResident(editing.id, {
          residentType: form.residentType,
          occupation: form.occupation || null,
          emergencyContact: form.emergencyContact || null,
          moveOutDate: form.moveOutDate || null,
          isPrimaryMember: Boolean(form.isPrimaryMember),
        });
      } else {
        await societyService.createResident({
          userId: Number(form.userId),
          flatId: Number(form.flatId),
          residentType: form.residentType,
          occupation: form.occupation || null,
          emergencyContact: form.emergencyContact || null,
          moveInDate: form.moveInDate,
          isPrimaryMember: Boolean(form.isPrimaryMember),
        });
      }
      push(editing ? 'Resident updated successfully.' : 'Resident added successfully.');
      setModalOpen(false); await load();
    } catch (error) { push(error.message || 'Unable to save resident.', 'error'); }
    finally { setBusy(false); }
  };

  const confirmDelete = async () => {
    setBusy(true);
    try { await societyService.deleteResident(deleting.id); push('Resident removed.'); setDeleting(null); await load(); }
    catch (error) { push(error.message || 'Unable to remove resident.', 'error'); }
    finally { setBusy(false); }
  };

  const confirmMoveOut = async () => {
    setBusy(true);
    try {
      await societyService.moveOutResident(movingOut.id);
      push('Resident marked as moved out.');
      setMovingOut(null);
      await load();
    } catch (error) {
      push(error.message || 'Unable to move out resident.', 'error');
    } finally { setBusy(false); }
  };

  const createFlat = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      const payload = {
        wing: flatForm.wing,
        flatNumber: flatForm.flatNumber.trim(),
        floorNumber: Number(flatForm.floorNumber),
        flatType: flatForm.flatType,
        areaSqft: flatForm.areaSqft === '' ? null : Number(flatForm.areaSqft),
      };
      if (editingFlat) {
        await societyService.updateFlat(editingFlat.flatId ?? editingFlat.id, payload);
        push('Flat updated.');
      } else {
        await societyService.createFlat(payload);
        push('Flat created. You can now assign an approved user to it.');
      }
      setFlatModalOpen(false);
      setFlatForm(emptyFlatForm);
      setEditingFlat(null);
      await loadAssignmentOptions();
    } catch (error) {
      push(error.message || 'Unable to create flat.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const openFlatManager = async () => {
    setFlatsOpen(true);
    setFlatFilters({ status: 'ALL', wing: 'ALL', type: 'ALL' });
    await loadAssignmentOptions();
  };

  const applyFlatFilters = async (nextFilters) => {
    setFlatFilters(nextFilters);
    setAssignmentLoading(true);
    try {
      let flats;
      if (nextFilters.status !== 'ALL') flats = await societyService.listFlatsByStatus(nextFilters.status);
      else if (nextFilters.wing !== 'ALL') flats = await societyService.listFlatsByWing(nextFilters.wing);
      else if (nextFilters.type !== 'ALL') flats = await societyService.listFlatsByType(nextFilters.type);
      else flats = await societyService.listFlats();
      setAssignmentOptions((current) => ({ ...current, flats }));
    } catch (error) { push(error.message || 'Unable to filter flats.', 'error'); }
    finally { setAssignmentLoading(false); }
  };

  const openFlatEdit = (flat) => {
    setEditingFlat(flat);
    setFlatForm({ wing: flat.wing || 'A', flatNumber: flat.flatNumber || '', floorNumber: flat.floorNumber ?? '', flatType: flat.flatType || '2BHK', areaSqft: flat.areaSqft ?? '' });
    setFlatModalOpen(true);
  };

  const changeFlatStatus = async (flat, status) => {
    setBusy(true);
    try { await societyService.updateFlatStatus(flat.flatId ?? flat.id, status); push(`Flat marked ${status.toLowerCase()}.`); await loadAssignmentOptions(); }
    catch (error) { push(error.message || 'Unable to update flat status.', 'error'); }
    finally { setBusy(false); }
  };

  const confirmFlatDelete = async () => {
    setBusy(true);
    try { await societyService.deleteFlat(deletingFlat.flatId ?? deletingFlat.id); push('Flat deleted.'); setDeletingFlat(null); await loadAssignmentOptions(); }
    catch (error) { push(error.message || 'Unable to delete flat.', 'error'); }
    finally { setBusy(false); }
  };

  const openFamily = async (resident, mode = 'view') => {
    setFamilyResident(resident);
    setFamilyMode(mode);
    setFamilyForm(emptyFamilyForm);
    setEditingFamily(null);
    setFamilyOpen(true);
    setFamilyLoading(true);
    try {
      setFamilyMembers(await societyService.listFamilyMembers(resident.id));
    } catch (error) {
      push(error.message || 'Unable to load family members.', 'error');
    } finally {
      setFamilyLoading(false);
    }
  };

  const loadFamilyMembers = async () => {
    if (!familyResident) return;
    setFamilyLoading(true);
    try {
      setFamilyMembers(await societyService.listFamilyMembers(familyResident.id));
      await load();
    } catch (error) {
      push(error.message || 'Unable to load family members.', 'error');
    } finally {
      setFamilyLoading(false);
    }
  };

  const saveFamilyMember = async (event) => {
    event.preventDefault();
    if (!familyResident) return;
    setBusy(true);
    const payload = {
      memberName: familyForm.memberName.trim(),
      relation: familyForm.relation,
      age: familyForm.age === '' ? null : Number(familyForm.age),
      mobile: familyForm.mobile.trim() || null,
    };
    try {
      if (editingFamily) {
        await societyService.updateFamilyMember(editingFamily.memberId ?? editingFamily.id, {
          memberName: payload.memberName,
          age: payload.age,
          mobile: payload.mobile,
        });
        push('Family member updated successfully.');
      } else {
        await societyService.createFamilyMember({ ...payload, residentId: familyResident.id });
        push('Family member added successfully.');
      }
      setFamilyForm(emptyFamilyForm);
      setEditingFamily(null);
      await loadFamilyMembers();
    } catch (error) {
      push(error.message || 'Unable to save family member.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const deleteFamilyMember = async (member) => {
    setBusy(true);
    try {
      await societyService.deleteFamilyMember(member.memberId ?? member.id);
      push('Family member removed.');
      if (editingFamily && (editingFamily.memberId ?? editingFamily.id) === (member.memberId ?? member.id)) {
        setEditingFamily(null);
        setFamilyForm(emptyFamilyForm);
      }
      await loadFamilyMembers();
    } catch (error) {
      push(error.message || 'Unable to remove family member.', 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <PageHeader eyebrow="Resident service" title={selfService ? 'My household' : 'Residents & flats'} subtitle={selfService ? 'Manage your household and family-member details.' : 'Create flats, then assign approved accounts to their flats.'} actions={!selfService && <><button className="btn-secondary" onClick={() => downloadCsv('residents.csv', filtered)}><Download className="h-4 w-4" /> Export</button>{canManageAssignments && <><button className="btn-secondary" onClick={openFlatManager}>Manage flats</button><button className="btn-secondary" onClick={() => { setEditingFlat(null); setFlatForm(emptyFlatForm); setFlatModalOpen(true); }}><Plus className="h-4 w-4" /> Add flat</button><button className="btn-primary" onClick={openCreate}><Plus className="h-4 w-4" /> Assign resident</button></>}</>} />

      <section className="panel overflow-hidden">
        <div className="panel-header">
          <div className="flex items-center gap-3"><div className="rounded-xl bg-blue-50 p-2.5 text-blue-700"><UsersRound className="h-5 w-5" /></div><div><h2 className="font-extrabold text-slate-900">Resident directory</h2><p className="text-xs text-slate-500">{items.length} total resident records</p></div></div>
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row">
            <label className="relative block sm:w-64"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><input className="field-input pl-9" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search resident or flat" /></label>
            <select className="field-input sm:w-36" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All status</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>
          </div>
        </div>

        {loading ? <div className="p-5"><LoadingState /></div> : filtered.length === 0 ? <EmptyState /> : (
          <div className="table-responsive">
            <table className="data-table">
              <thead><tr><th>Resident</th><th>Flat</th><th>Member type</th><th>Family</th><th>Move-in date</th><th>Status</th><th className="text-right">Actions</th></tr></thead>
              <tbody>{filtered.map((item) => (
                <tr key={item.id}>
                  <td><div><p className="font-bold text-slate-800">{item.name}</p><p className="mt-0.5 text-xs text-slate-400">{item.email} · {item.mobile}</p></div></td>
                  <td><span className="rounded-lg bg-slate-100 px-2.5 py-1 font-bold text-slate-700">{item.wing}-{item.flatNo}</span></td>
                  <td>{item.memberType}</td><td>{item.familyMembers} members</td><td>{formatDate(item.moveInDate)}</td><td><Badge value={item.status} /></td>
                  <td><div className="flex justify-end gap-1"><button className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-bold text-slate-600 hover:bg-blue-50 hover:text-blue-700" onClick={() => openFamily(item, 'view')} aria-label="View household" title="View household"><UsersRound className="h-4 w-4" /> Household</button>{canManageFamily && <button className="rounded-lg p-2 text-slate-500 hover:bg-emerald-50 hover:text-emerald-700" onClick={() => openFamily(item, 'manage')} aria-label="Add or manage family members" title="Manage household"><UserPlus className="h-4 w-4" /></button>}{!selfService && <><button className="rounded-lg p-2 text-slate-500 hover:bg-blue-50 hover:text-blue-700" onClick={() => openEdit(item)} aria-label="Edit"><Edit3 className="h-4 w-4" /></button>{item.status === 'ACTIVE' && <button className="rounded-lg px-2 py-1 text-xs font-bold text-amber-700 hover:bg-amber-50" onClick={() => setMovingOut(item)}>Move out</button>}<button className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700" onClick={() => setDeleting(item)} aria-label="Delete"><Trash2 className="h-4 w-4" /></button></>}</div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit resident' : 'Assign approved resident'} description={editing ? 'Update resident information.' : 'Select an approved user and an existing vacant flat. Database IDs are not required.'}>
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          {!editing && <>{assignmentLoading ? <div className="sm:col-span-2 text-sm text-slate-500">Loading approved users and flats…</div> : <><div><label className="field-label">Approved user</label><select className="field-input" name="userId" value={form.userId} onChange={updateField} required><option value="">Select approved user</option>{assignmentOptions.users.map((candidate) => <option key={candidate.id} value={candidate.id}>{candidate.name} · {candidate.email}</option>)}</select></div><div><label className="field-label">Vacant flat</label><select className="field-input" name="flatId" value={form.flatId} onChange={updateField} required><option value="">Select vacant flat</option>{assignmentOptions.flats.filter((flat) => flat.status === 'VACANT').map((flat) => <option key={flat.flatId ?? flat.id} value={flat.flatId ?? flat.id}>{flat.wing}-{flat.flatNumber} · {flat.flatType}</option>)}</select>{assignmentOptions.flats.filter((flat) => flat.status === 'VACANT').length === 0 && <p className="mt-1 text-xs text-amber-700">No vacant flat exists. Add a flat first.</p>}</div></>}</>}
          <div><label className="field-label">Resident type</label><select className="field-input" name="residentType" value={form.residentType} onChange={updateField}><option value="OWNER">Owner</option><option value="TENANT">Tenant</option></select></div>
          <div><label className="field-label">Move-in date</label><input className="field-input" type="date" name="moveInDate" value={String(form.moveInDate || '').slice(0, 10)} onChange={updateField} required={!editing} /></div>
          <div><label className="field-label">Occupation (optional)</label><input className="field-input" name="occupation" value={form.occupation || ''} onChange={updateField} /></div>
          <div><label className="field-label">Emergency contact (optional)</label><input className="field-input" name="emergencyContact" value={form.emergencyContact || ''} onChange={updateField} /></div>
          {editing && <div><label className="field-label">Move-out date (optional)</label><input className="field-input" type="date" name="moveOutDate" value={String(form.moveOutDate || '').slice(0, 10)} onChange={updateField} /></div>}
          <label className="sm:col-span-2 flex items-center gap-3 rounded-xl bg-slate-50 p-4 text-sm font-semibold text-slate-700"><input type="checkbox" checked={Boolean(form.isPrimaryMember)} onChange={(event) => setForm({ ...form, isPrimaryMember: event.target.checked })} className="h-4 w-4" /> Primary member for this flat</label>
          <div className="sm:col-span-2 mt-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save changes' : 'Add resident'}</button></div>
        </form>
      </Modal>

      <Modal open={flatModalOpen} onClose={() => setFlatModalOpen(false)} title={editingFlat ? 'Edit flat' : 'Add flat'} description="Create or update flat information before assigning a resident account to it.">
        <form onSubmit={createFlat} className="grid gap-4 sm:grid-cols-2">
          <div><label className="field-label">Wing</label><select className="field-input" value={flatForm.wing} onChange={(event) => setFlatForm({ ...flatForm, wing: event.target.value })}>{['A', 'B', 'C', 'D'].map((wing) => <option key={wing}>{wing}</option>)}</select></div>
          <div><label className="field-label">Flat number</label><input className="field-input" value={flatForm.flatNumber} onChange={(event) => setFlatForm({ ...flatForm, flatNumber: event.target.value })} placeholder="102" required /></div>
          <div><label className="field-label">Floor number</label><input className="field-input" type="number" min="0" max="255" value={flatForm.floorNumber} onChange={(event) => setFlatForm({ ...flatForm, floorNumber: event.target.value })} required /></div>
          <div><label className="field-label">Flat type</label><select className="field-input" value={flatForm.flatType} onChange={(event) => setFlatForm({ ...flatForm, flatType: event.target.value })}>{['1RK', '1BHK', '2BHK', '3BHK', '4BHK'].map((type) => <option key={type}>{type}</option>)}</select></div>
          <div className="sm:col-span-2"><label className="field-label">Area in sq. ft. (optional)</label><input className="field-input" type="number" min="100" max="65535" value={flatForm.areaSqft} onChange={(event) => setFlatForm({ ...flatForm, areaSqft: event.target.value })} /></div>
          <div className="sm:col-span-2 mt-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => setFlatModalOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : editingFlat ? 'Save flat' : 'Add flat'}</button></div>
        </form>
      </Modal>

      <Modal open={flatsOpen} onClose={() => setFlatsOpen(false)} title="Manage flats" description="Update flat details and occupancy status before resident assignment." maxWidth="max-w-3xl">
        <div className="mb-4 flex flex-wrap gap-2"><select className="field-input w-36" value={flatFilters.status} onChange={(event) => applyFlatFilters({ status: event.target.value, wing: 'ALL', type: 'ALL' })}><option value="ALL">All status</option><option value="VACANT">Vacant</option><option value="OCCUPIED">Occupied</option></select><select className="field-input w-28" value={flatFilters.wing} onChange={(event) => applyFlatFilters({ status: 'ALL', wing: event.target.value, type: 'ALL' })}><option value="ALL">All wings</option>{['A', 'B', 'C', 'D'].map((wing) => <option key={wing}>{wing}</option>)}</select><select className="field-input w-32" value={flatFilters.type} onChange={(event) => applyFlatFilters({ status: 'ALL', wing: 'ALL', type: event.target.value })}><option value="ALL">All types</option>{['1RK', '1BHK', '2BHK', '3BHK', '4BHK'].map((type) => <option key={type}>{type}</option>)}</select></div>{assignmentLoading ? <LoadingState /> : assignmentOptions.flats.length === 0 ? <EmptyState title="No flats found" description="Create the first flat to begin resident assignment." /> : <div className="table-responsive"><table className="data-table"><thead><tr><th>Flat</th><th>Floor</th><th>Type</th><th>Status</th><th className="text-right">Actions</th></tr></thead><tbody>{assignmentOptions.flats.map((flat) => <tr key={flat.flatId ?? flat.id}><td className="font-bold">{flat.wing}-{flat.flatNumber}</td><td>{flat.floorNumber}</td><td>{flat.flatType}</td><td><Badge value={flat.status} /></td><td><div className="flex justify-end gap-1"><button type="button" onClick={() => openFlatEdit(flat)} className="rounded-lg p-2 text-blue-700 hover:bg-blue-50" title="Edit flat"><Edit3 className="h-4 w-4" /></button><button type="button" disabled={busy} onClick={() => changeFlatStatus(flat, flat.status === 'VACANT' ? 'OCCUPIED' : 'VACANT')} className="rounded-lg px-2 py-1 text-xs font-bold text-emerald-700 hover:bg-emerald-50">Mark {flat.status === 'VACANT' ? 'occupied' : 'vacant'}</button><button type="button" disabled={busy} onClick={() => setDeletingFlat(flat)} className="rounded-lg p-2 text-rose-600 hover:bg-rose-50" title="Delete flat"><Trash2 className="h-4 w-4" /></button></div></td></tr>)}</tbody></table></div>}
      </Modal>

      <Modal open={familyOpen} onClose={() => setFamilyOpen(false)} title={`Family members${familyResident?.name ? ` · ${familyResident.name}` : ''}`} description={familyMode === 'manage' ? 'Add, edit, or remove members of this resident’s household.' : 'View household member details.'}>
        {familyMode === 'manage' && <form onSubmit={saveFamilyMember} className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><label className="field-label">Member name</label><input className="field-input" value={familyForm.memberName} onChange={(event) => setFamilyForm({ ...familyForm, memberName: event.target.value })} required /></div>
          <div><label className="field-label">Relation</label><select className="field-input" value={familyForm.relation} onChange={(event) => setFamilyForm({ ...familyForm, relation: event.target.value })}>{['FATHER', 'MOTHER', 'SPOUSE', 'SON', 'DAUGHTER', 'BROTHER', 'SISTER', 'OTHER'].map((relation) => <option key={relation} value={relation}>{relation.charAt(0) + relation.slice(1).toLowerCase()}</option>)}</select></div>
          <div><label className="field-label">Age (optional)</label><input className="field-input" type="number" min="0" max="127" value={familyForm.age} onChange={(event) => setFamilyForm({ ...familyForm, age: event.target.value })} /></div>
          <div className="sm:col-span-2"><label className="field-label">Mobile (optional)</label><input className="field-input" inputMode="numeric" pattern="[0-9]{10}" maxLength="10" value={familyForm.mobile} onChange={(event) => setFamilyForm({ ...familyForm, mobile: event.target.value.replace(/\D/g, '') })} /></div>
          <div className="sm:col-span-2 flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={() => { setEditingFamily(null); setFamilyForm(emptyFamilyForm); }}>Clear</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : editingFamily ? 'Save member' : 'Add family member'}</button></div>
        </form>}
        <div className="mt-6 border-t border-slate-100 pt-5"><h3 className="text-sm font-extrabold text-slate-900">Current family members</h3>{familyLoading ? <p className="mt-3 text-sm text-slate-500">Loading family members…</p> : familyMembers.length === 0 ? <p className="mt-3 text-sm text-slate-500">No family members added yet.</p> : <div className="mt-3 space-y-2">{familyMembers.map((member) => <div key={member.memberId ?? member.id} className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 p-3"><div><p className="font-bold text-slate-800">{member.memberName}</p><p className="text-xs text-slate-500">{member.relation} {member.age != null ? `· ${member.age} years` : ''} {member.mobile ? `· ${member.mobile}` : ''}</p></div>{familyMode === 'manage' && <div className="flex shrink-0 gap-1"><button type="button" className="rounded-lg p-2 text-slate-500 hover:bg-blue-50 hover:text-blue-700" onClick={() => { setEditingFamily(member); setFamilyForm({ memberName: member.memberName || '', relation: member.relation || 'OTHER', age: member.age ?? '', mobile: member.mobile || '' }); }} aria-label="Edit family member"><Edit3 className="h-4 w-4" /></button><button type="button" className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-700" onClick={() => deleteFamilyMember(member)} disabled={busy} aria-label="Delete family member"><Trash2 className="h-4 w-4" /></button></div>}</div>)}</div>}</div>
      </Modal>

      <ConfirmDialog open={Boolean(deleting)} onClose={() => setDeleting(null)} onConfirm={confirmDelete} busy={busy} title="Remove resident?" description={`This will remove ${deleting?.name || 'this resident'} from the directory.`} />
      <ConfirmDialog open={Boolean(movingOut)} onClose={() => setMovingOut(null)} onConfirm={confirmMoveOut} busy={busy} title="Mark resident as moved out?" description={`${movingOut?.name || 'This resident'} will remain in the history but their occupancy will be ended.`} />
      <ConfirmDialog open={Boolean(deletingFlat)} onClose={() => setDeletingFlat(null)} onConfirm={confirmFlatDelete} busy={busy} title="Delete flat?" description={`Flat ${deletingFlat?.wing || ''}-${deletingFlat?.flatNumber || ''} will be permanently removed.`} />
    </div>
  );
}
