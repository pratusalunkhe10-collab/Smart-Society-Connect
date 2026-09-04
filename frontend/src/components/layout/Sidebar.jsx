import { LogOut, X } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { navigation, roleLabels } from '../../data/navigation.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useTheme } from '../../context/ThemeContext.jsx';
import { initials } from '../../utils/format.js';
import Brand from './Brand.jsx';

export default function Sidebar({ open, onClose }) {
  const { user, logout } = useAuth();
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const items = navigation.filter((item) => item.roles.includes(user?.role));

  const handleLogout = () => {
    logout();
    onClose();
  };

  return (
    <>
      <button
        type="button"
        aria-label="Close navigation"
        onClick={onClose}
        className={`fixed inset-0 z-40 bg-slate-950/50 backdrop-blur-sm transition lg:hidden ${open ? 'pointer-events-auto opacity-100' : 'pointer-events-none opacity-0'}`}
      />
      <aside className={`fixed inset-y-0 left-0 z-50 flex w-[286px] flex-col border-r px-4 py-5 shadow-2xl transition-transform duration-300 lg:translate-x-0 ${isDark ? 'border-white/[0.06] bg-[#111722] text-white shadow-black/30' : 'border-slate-200 bg-white text-slate-900 shadow-slate-900/10'} ${open ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="flex items-center justify-between px-2">
          <Brand light={isDark} />
          <button type="button" onClick={onClose} className={`rounded-xl p-2 lg:hidden ${isDark ? 'text-slate-400 hover:bg-white/10 hover:text-white' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900'}`} aria-label="Close menu">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className={`mt-7 rounded-2xl border p-3 ${isDark ? 'border-white/10 bg-white/[0.06]' : 'border-slate-200 bg-slate-50'}`}>
          <div className="flex items-center gap-3">
            {user?.profileImageUrl ? <img src={user.profileImageUrl} alt="Profile" className="h-10 w-10 shrink-0 rounded-xl object-cover" /> : <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-rose-600 text-sm font-extrabold text-white">{initials(user?.name)}</div>}
            <div className="min-w-0">
              <p className={`truncate text-sm font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>{user?.name}</p>
              <p className="truncate text-xs text-slate-400">{roleLabels[user?.role] || user?.role}</p>
            </div>
          </div>
        </div>

        <nav className="mt-6 flex-1 space-y-1 overflow-y-auto pr-1">
          <p className="mb-3 px-3 text-[10px] font-extrabold uppercase tracking-[0.22em] text-slate-500">Workspace</p>
          {items.map(({ label, roleLabel, to, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) => `group flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold transition ${isActive ? 'bg-gradient-to-r from-rose-600 to-red-500 text-white shadow-lg shadow-rose-950/30' : isDark ? 'text-slate-400 hover:bg-white/[0.07] hover:text-white' : 'text-slate-600 hover:bg-rose-50 hover:text-rose-700'}`}
            >
              <Icon className="h-5 w-5 shrink-0" />
              <span>{roleLabel?.[user?.role] || label}</span>
            </NavLink>
          ))}
        </nav>

        <div className={`border-t pt-4 ${isDark ? 'border-white/10' : 'border-slate-200'}`}>
          <button type="button" onClick={handleLogout} className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-slate-400 transition hover:bg-rose-500/10 hover:text-rose-300">
            <LogOut className="h-5 w-5" />
            Sign out
          </button>
          <p className="mt-3 px-3 text-[10px] text-slate-500">Smart Society Connect v1.0</p>
        </div>
      </aside>
    </>
  );
}
