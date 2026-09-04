import { ArrowLeft, MailCheck } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authService } from '../../services/authService.js';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true); setError(''); setMessage('');
    try {
      await authService.resendOtp({ email });
      setMessage('A fresh verification OTP is being sent to your email.');
      window.setTimeout(() => navigate('/verify-otp', { state: { email } }), 800);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Unable to resend the verification OTP.');
    } finally { setBusy(false); }
  };

  return (
    <div className="auth-card">
      <Link to="/login" className="mb-5 inline-flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-slate-800"><ArrowLeft className="h-4 w-4" /> Back to login</Link>
      <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Resend verification OTP</h1>
      <p className="mt-2 text-sm leading-6 text-slate-500">Enter your registered email and we will send a fresh OTP to activate your account.</p>
      <form onSubmit={submit} className="mt-7 space-y-5">
        <div><label className="field-label">Registered email</label><input className="field-input" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></div>
        {message && <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">{message}</div>}
        {error && <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</div>}
        <button className="btn-primary w-full py-3" disabled={busy}><MailCheck className="h-4 w-4" /> {busy ? 'Sending…' : 'Resend verification OTP'}</button>
      </form>
    </div>
  );
}
