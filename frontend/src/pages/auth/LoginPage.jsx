import { Eye, EyeOff, KeyRound, LockKeyhole, Mail, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import { USE_MOCK_API } from '../../config/api.js';

const demos = [
  ['Admin', 'admin@society.com', 'Admin@123'],
  ['Resident', 'resident@society.com', 'Resident@123'],
  ['Security', 'security@society.com', 'Security@123'],
  ['Accountant', 'accountant@society.com', 'Accountant@123'],
  ['Committee', 'committee@society.com', 'Committee@123'],
];

export default function LoginPage() {
  const { login, loading } = useAuth();
  const { push } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [form, setForm] = useState({ username: '', password: '', remember: true });
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    try {
      await login({ username: form.username, password: form.password });
      push('Welcome back. Login successful.');
      navigate(location.state?.from?.pathname || '/app/dashboard', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Unable to sign in.');
    }
  };

  const selectDemo = (email, password) => setForm((current) => ({ ...current, username: email, password }));

  return (
    <div className="auth-card">
      <div>
        <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-rose-400">Secure community portal</p>
        <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">Welcome back</h1>
        <p className="mt-2 text-sm leading-6 text-slate-400">Use your registered email or mobile number to continue.</p>
      </div>

      <form className="mt-7 space-y-5" onSubmit={submit}>
        <div>
          <label className="field-label" htmlFor="username">Email or mobile number</label>
          <div className="relative">
            <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input id="username" className="field-input pl-10" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} placeholder="you@example.com" required />
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between">
            <label className="field-label" htmlFor="password">Password</label>
            <Link to="/forgot-password" className="mb-1.5 text-xs font-bold text-rose-700 hover:text-rose-800">Resend OTP</Link>
          </div>
          <div className="relative">
            <LockKeyhole className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input id="password" type={showPassword ? 'text' : 'password'} className="field-input pl-10 pr-11" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="Minimum 8 characters" required minLength={8} />
            <button type="button" onClick={() => setShowPassword((current) => !current)} className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700" aria-label="Toggle password visibility">
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
        </div>

        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={form.remember} onChange={(event) => setForm({ ...form, remember: event.target.checked })} className="h-4 w-4 rounded border-slate-300 text-rose-600 focus:ring-rose-500" />
          Remember this device
        </label>

        {error && <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</div>}

        <button className="btn-primary w-full py-3" disabled={loading}>
          {loading ? <><span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" /> Signing in…</> : <><KeyRound className="h-4 w-4" /> Sign in securely</>}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-400">New to the community? <Link to="/register" className="font-bold text-rose-400 hover:text-rose-300">Create an account</Link></p>
      <p className="mt-3 text-center text-xs leading-5 text-slate-500">For your security, never share your password or verification code with anyone.</p>

      {USE_MOCK_API && (
        <div className="mt-7 rounded-2xl border border-blue-100 bg-blue-50/70 p-4">
          <div className="flex items-center gap-2 text-blue-800"><ShieldCheck className="h-4 w-4" /><p className="text-xs font-extrabold uppercase tracking-wider">Demo accounts</p></div>
          <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
            {demos.map(([label, email, password]) => (
              <button key={email} type="button" onClick={() => selectDemo(email, password)} className="rounded-xl border border-blue-100 bg-white px-3 py-2 text-left transition hover:border-blue-300 hover:shadow-sm">
                <span className="block text-xs font-bold text-slate-800">{label}</span>
                <span className="block truncate text-[10px] text-slate-400">Use account</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
