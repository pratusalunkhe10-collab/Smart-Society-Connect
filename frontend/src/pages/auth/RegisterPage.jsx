import { ArrowLeft, UserPlus } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authService } from '../../services/authService.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { push } = useToast();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', email: '', mobile: '', password: '', confirmPassword: '' });

  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value });
  const submit = async (event) => {
    event.preventDefault();
    setError('');
    if (form.password !== form.confirmPassword) return setError('Passwords do not match.');
    setBusy(true);
    try {
      await authService.register(form);
      push('Registration successful. Check your email for the verification OTP.');
      navigate('/verify-otp', { state: { email: form.email } });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Registration failed.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-card">
      <Link to="/login" className="mb-5 inline-flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-slate-800"><ArrowLeft className="h-4 w-4" /> Back to login</Link>
      <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Create your account</h1>
      <p className="mt-2 text-sm leading-6 text-slate-500">Verify your email after registration. An administrator will review your application and assign the appropriate access role.</p>

      <form onSubmit={submit} className="mt-7 grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2"><label className="field-label">Full name</label><input className="field-input" name="name" value={form.name} onChange={update} required /></div>
        <div><label className="field-label">Email</label><input className="field-input" type="email" name="email" value={form.email} onChange={update} required /></div>
        <div><label className="field-label">Mobile</label><input className="field-input" name="mobile" value={form.mobile} onChange={update} pattern="[0-9]{10}" required /></div>
        <div><label className="field-label">Password</label><input className="field-input" type="password" name="password" value={form.password} onChange={update} minLength={8} required /></div>
        <div><label className="field-label">Confirm password</label><input className="field-input" type="password" name="confirmPassword" value={form.confirmPassword} onChange={update} minLength={8} required /></div>
        {error && <div className="sm:col-span-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</div>}
        <button className="btn-primary sm:col-span-2 py-3" disabled={busy}><UserPlus className="h-4 w-4" /> {busy ? 'Creating account…' : 'Create account'}</button>
      </form>
    </div>
  );
}
