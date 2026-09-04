import { ArrowLeft, BadgeCheck, Clock3, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authService } from '../../services/authService.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { push } = useToast();
  const [email, setEmail] = useState(location.state?.email || '');
  const [otpCode, setOtpCode] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      await authService.verifyOtp({ email, otpCode });
      push('Account verified successfully. You can now sign in.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'OTP verification failed.');
    } finally { setBusy(false); }
  };

  return (
    <div className="auth-card">
      <Link to="/login" className="mb-5 inline-flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-slate-800"><ArrowLeft className="h-4 w-4" /> Back to login</Link>
      <div className="grid h-12 w-12 place-items-center rounded-2xl bg-rose-100 text-rose-700"><ShieldCheck className="h-6 w-6" /></div>
      <h1 className="mt-4 text-3xl font-extrabold tracking-tight text-slate-900">Verify your email</h1>
      <p className="mt-2 text-sm leading-6 text-slate-400">We sent a six-digit security code to your registered email address. Enter it below to activate your account.</p>
      <form onSubmit={submit} className="mt-7 space-y-5">
        <div><label className="field-label">Email</label><input className="field-input" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></div>
        <div><label className="field-label">6-digit security code</label><input className="field-input text-center text-xl font-extrabold tracking-[0.45em]" inputMode="numeric" autoComplete="one-time-code" value={otpCode} onChange={(event) => setOtpCode(event.target.value.replace(/\D/g, '').slice(0, 6))} pattern="[0-9]{6}" required /></div>
        <div className="flex gap-3 rounded-2xl bg-rose-50 px-4 py-3 text-xs leading-5 text-rose-900"><Clock3 className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" /><span>Your code expires after 5 minutes and can be used once. Smart Society Connect will never ask for it by phone or message.</span></div>
        {error && <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</div>}
        <button className="btn-primary w-full py-3" disabled={busy}><BadgeCheck className="h-4 w-4" /> {busy ? 'Verifying…' : 'Verify OTP'}</button>
      </form>
    </div>
  );
}
