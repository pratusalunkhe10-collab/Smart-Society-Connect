import { Camera, DatabaseBackup, Mail, MapPinHouse, Phone, Save, ShieldCheck, Upload, UserRound } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import PageHeader from '../components/ui/PageHeader.jsx';
import Badge from '../components/ui/Badge.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { roleLabels } from '../data/navigation.js';
import { USE_MOCK_API } from '../config/api.js';
import { resetMockDb } from '../services/mockDb.js';
import { initials } from '../utils/format.js';

const notificationDefaults = { email: true, sms: true, visitor: true, billing: true, meetings: true };

export default function ProfilePage() {
  const { user, updateProfile, uploadProfileImage } = useAuth();
  const { push } = useToast();
  const [form, setForm] = useState({ name: user?.name || '', email: user?.email || '', mobile: user?.mobile || '', flat: user?.flat || '' });
  const notificationKey = `ssc_notification_preferences_${user?.id || 'guest'}`;
  const [notifications, setNotifications] = useState(notificationDefaults);
  const [busy, setBusy] = useState(false);
  const [imageBusy, setImageBusy] = useState(false);
  const fileInput = useRef(null);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(notificationKey);
      setNotifications(saved ? { ...notificationDefaults, ...JSON.parse(saved) } : notificationDefaults);
    } catch {
      setNotifications(notificationDefaults);
    }
  }, [notificationKey]);

  const setNotificationPreference = (key, checked) => {
    setNotifications((current) => {
      const next = { ...current, [key]: checked };
      localStorage.setItem(notificationKey, JSON.stringify(next));
      return next;
    });
  };

  const save = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      await updateProfile({ name: form.name, mobile: form.mobile });
      push('Profile information saved.');
    } catch (error) {
      push(error.message || 'Unable to save profile information.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const resetDemo = () => {
    resetMockDb();
    push('Demo records restored to their original values.', 'info');
  };

  const uploadImage = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      push('Please select a JPG, PNG, or WebP image.', 'error');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      push('Profile image must be 5 MB or smaller.', 'error');
      return;
    }
    setImageBusy(true);
    try {
      await uploadProfileImage(file);
      push('Profile image updated.');
    } catch (error) {
      push(error.message || 'Unable to upload profile image.', 'error');
    } finally {
      setImageBusy(false);
      event.target.value = '';
    }
  };

  return (
    <div>
      <PageHeader eyebrow="Account settings" title="My profile" subtitle="Manage your contact information and society notification preferences." />

      <section className="grid gap-6 xl:grid-cols-[0.8fr_1.6fr]">
        <article className="panel h-fit overflow-hidden">
          <div className="profile-summary p-6">
            <div className="relative h-20 w-20">
              {user?.profileImageUrl ? <img src={user.profileImageUrl} alt="Profile" className="h-20 w-20 rounded-3xl object-cover shadow-lg shadow-emerald-950/40" /> : <div className="grid h-20 w-20 place-items-center rounded-3xl bg-emerald-500 text-2xl font-extrabold shadow-lg shadow-emerald-950/40">{initials(user?.name)}</div>}
              <button type="button" onClick={() => fileInput.current?.click()} className="absolute -bottom-2 -right-2 rounded-xl bg-white p-2 text-emerald-700 shadow-lg hover:bg-emerald-50" aria-label="Upload profile image"><Camera className="h-4 w-4" /></button>
              <input ref={fileInput} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={uploadImage} />
            </div>
            <h2 className="mt-5 text-xl font-extrabold">{user?.name}</h2>
            <p className="profile-summary-role mt-1 text-sm">{roleLabels[user?.role] || user?.role}</p>
            <div className="mt-4"><Badge value="ACTIVE" /></div>
            <button type="button" disabled={imageBusy} onClick={() => fileInput.current?.click()} className="profile-summary-upload mt-5 inline-flex items-center gap-2 text-sm font-bold disabled:opacity-60"><Upload className="h-4 w-4" />{imageBusy ? 'Uploading…' : 'Upload photo'}</button>
          </div>
          <div className="space-y-4 p-5 text-sm">
            <div className="flex items-center gap-3"><Mail className="h-4 w-4 text-slate-400" /><span className="break-all text-slate-600">{user?.email || 'Not provided'}</span></div>
            <div className="flex items-center gap-3"><Phone className="h-4 w-4 text-slate-400" /><span className="text-slate-600">{user?.mobile || 'Not provided'}</span></div>
            <div className="flex items-center gap-3"><MapPinHouse className="h-4 w-4 text-slate-400" /><span className="text-slate-600">Flat / location: {user?.flat || 'Not allocated'}</span></div>
            <div className="flex items-center gap-3"><ShieldCheck className="h-4 w-4 text-slate-400" /><span className="text-slate-600">Role-based access enabled</span></div>
          </div>
        </article>

        <div className="space-y-6">
          <article className="panel overflow-hidden">
            <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Personal information</h2><p className="mt-1 text-xs text-slate-500">Keep your contact details current for visitor and billing alerts.</p></div><UserRound className="h-5 w-5 text-slate-400" /></div>
            <form onSubmit={save} className="grid gap-4 p-5 sm:grid-cols-2 sm:p-6">
              <div><label className="field-label">Full name</label><input className="field-input" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required /></div>
              <div><label className="field-label">Email</label><input className="field-input" type="email" value={form.email} readOnly aria-describedby="email-note" /><p id="email-note" className="mt-1 text-xs text-slate-500">Email changes require a separate verified-email process.</p></div>
              <div><label className="field-label">Mobile</label><input className="field-input" value={form.mobile} onChange={(event) => setForm({ ...form, mobile: event.target.value })} /></div>
              <div><label className="field-label">Flat / location</label><input className="field-input" value={form.flat} readOnly /><p className="mt-1 text-xs text-slate-500">Flat allocation is managed from resident records.</p></div>
              <div className="sm:col-span-2 flex justify-end"><button className="btn-primary" disabled={busy}><Save className="h-4 w-4" /> {busy ? 'Saving…' : 'Save profile'}</button></div>
            </form>
          </article>

          <article className="panel overflow-hidden">
            <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Notification preferences</h2><p className="mt-1 text-xs text-slate-500">Choose the operational updates you want to receive.</p></div></div>
            <div className="divide-y divide-slate-100 px-5 sm:px-6">
              {[
                ['email', 'Email notifications', 'Receive receipts, notices and complaint updates by email.'],
                ['sms', 'SMS notifications', 'Receive urgent visitor and payment alerts by SMS.'],
                ['visitor', 'Visitor approvals', 'Notify me when a new visitor requests access.'],
                ['billing', 'Billing reminders', 'Notify me before maintenance bills become overdue.'],
                ['meetings', 'Meeting notices', 'Notify me when committee or general meetings are published.'],
              ].map(([key, label, description]) => (
                <label key={key} className="flex cursor-pointer items-center justify-between gap-4 py-4">
                  <span><span className="block text-sm font-bold text-slate-800">{label}</span><span className="mt-1 block text-xs leading-5 text-slate-500">{description}</span></span>
                  <input type="checkbox" className="h-5 w-5 shrink-0 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500" checked={notifications[key]} onChange={(event) => setNotificationPreference(key, event.target.checked)} />
                </label>
              ))}
            </div>
          </article>

          {USE_MOCK_API && (
            <article className="panel flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
              <div className="flex items-start gap-3"><div className="rounded-xl bg-blue-50 p-2.5 text-blue-700"><DatabaseBackup className="h-5 w-5" /></div><div><h2 className="font-extrabold text-slate-900">Demo data controls</h2><p className="mt-1 text-xs leading-5 text-slate-500">Restore all sample residents, visitors, complaints, bills and meetings.</p></div></div>
              <button className="btn-secondary shrink-0" onClick={resetDemo}>Reset demo data</button>
            </article>
          )}
        </div>
      </section>
    </div>
  );
}
